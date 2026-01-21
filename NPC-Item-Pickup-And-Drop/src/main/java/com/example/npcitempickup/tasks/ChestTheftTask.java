package com.example.npcitempickup.tasks;

import com.example.npcitempickup.NPCItemPickupPlugin;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Task that runs periodically to check for chests that thief NPCs can steal
 * from
 */
public class ChestTheftTask extends BukkitRunnable {

    private final NPCItemPickupPlugin plugin;
    private final double chestRadius;
    private final double theftDistance;
    private final int itemsPerTick;
    private final boolean moveToChests;
    private final boolean debugEnabled;

    public ChestTheftTask(NPCItemPickupPlugin plugin) {
        this.plugin = plugin;
        this.chestRadius = plugin.getConfig().getDouble("thief_mode.chest_radius", 5.0);
        this.theftDistance = plugin.getConfig().getDouble("thief_mode.theft_distance", 4.0);
        this.itemsPerTick = plugin.getConfig().getInt("thief_mode.items_per_tick", 1);
        this.moveToChests = plugin.getConfig().getBoolean("thief_mode.move_to_chests", true);
        this.debugEnabled = plugin.getConfig().getBoolean("debug.enabled", false);
    }

    @Override
    public void run() {
        // Only run if plugin and thief mode are enabled
        if (!plugin.isPluginEnabled() || !plugin.getConfig().getBoolean("thief_mode.enabled", true)) {
            return;
        }

        try {
            NPCRegistry registry = CitizensAPI.getNPCRegistry();
            if (registry == null) {
                return;
            }

            // Check each spawned NPC
            for (NPC npc : registry) {
                if (!npc.isSpawned()) {
                    continue;
                }

                // Only check NPCs that are in thief mode
                if (!plugin.getInventoryManager().isThiefMode(npc)) {
                    continue;
                }

                Entity npcEntity = npc.getEntity();
                if (npcEntity == null) {
                    continue;
                }

                Location npcLocation = npcEntity.getLocation();

                // Find nearby chests within radius (scanning 5x5x5 area around NPC)
                int radius = (int) Math.ceil(chestRadius);
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            Location checkLocation = npcLocation.clone().add(x, y, z);

                            // Check distance to avoid checking too far
                            if (npcLocation.distance(checkLocation) > chestRadius) {
                                continue;
                            }

                            Block block = checkLocation.getBlock();

                            // Check if block is a container (chest, barrel, etc.)
                            if (isContainer(block.getType())) {
                                BlockState blockState = block.getState();
                                if (blockState instanceof InventoryHolder) {
                                    InventoryHolder container = (InventoryHolder) blockState;

                                    double distance = npcLocation.distance(checkLocation);

                                    // If NPC is within theft distance, steal items
                                    if (distance <= theftDistance) {
                                        stealFromContainer(npc, container);
                                    } else if (moveToChests && distance <= chestRadius) {
                                        // Move NPC towards the container
                                        moveTowardsContainer(npc, checkLocation);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (debugEnabled) {
                plugin.getLogger().warning("Error in chest theft task: " + e.getMessage());
            }
        }
    }

    private boolean isContainer(Material material) {
        switch (material) {
            case CHEST:
            case TRAPPED_CHEST:
            case BARREL:
            case SHULKER_BOX:
            case WHITE_SHULKER_BOX:
            case ORANGE_SHULKER_BOX:
            case MAGENTA_SHULKER_BOX:
            case LIGHT_BLUE_SHULKER_BOX:
            case YELLOW_SHULKER_BOX:
            case LIME_SHULKER_BOX:
            case PINK_SHULKER_BOX:
            case GRAY_SHULKER_BOX:
            case LIGHT_GRAY_SHULKER_BOX:
            case CYAN_SHULKER_BOX:
            case PURPLE_SHULKER_BOX:
            case BLUE_SHULKER_BOX:
            case BROWN_SHULKER_BOX:
            case GREEN_SHULKER_BOX:
            case RED_SHULKER_BOX:
            case BLACK_SHULKER_BOX:
            case HOPPER:
            case DISPENSER:
            case DROPPER:
                return true;
            default:
                return false;
        }
    }

    private void stealFromContainer(NPC npc, InventoryHolder container) {
        try {
            Inventory inventory = container.getInventory();
            int stolenCount = 0;

            // Try to steal up to itemsPerTick items
            for (int slot = 0; slot < inventory.getSize() && stolenCount < itemsPerTick; slot++) {
                ItemStack item = inventory.getItem(slot);

                if (item == null || item.getType() == Material.AIR) {
                    continue;
                }

                // Check if NPC can pick up this item
                if (!plugin.getInventoryManager().canPickupItem(npc, item)) {
                    continue;
                }

                // Take one item from the stack
                ItemStack stolenItem = item.clone();
                stolenItem.setAmount(1);

                // Try to add the stolen item to NPC inventory
                if (plugin.getInventoryManager().addItem(npc, stolenItem)) {
                    // Remove one item from the container
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                    } else {
                        inventory.setItem(slot, null);
                    }

                    stolenCount++;

                    if (debugEnabled && plugin.getConfig().getBoolean("debug.log_theft", true)) {
                        plugin.getLogger().info("NPC " + npc.getName() + " stole " +
                                stolenItem.getType().name() + " from container");
                    }

                    // Play sound effect (optional)
                    if (npc.getEntity() != null) {
                        npc.getEntity().getWorld().playSound(
                                npc.getEntity().getLocation(),
                                org.bukkit.Sound.BLOCK_CHEST_OPEN,
                                0.1f,
                                1.2f);
                    }
                }
            }
        } catch (Exception e) {
            if (debugEnabled) {
                plugin.getLogger().warning("Error stealing from container: " + e.getMessage());
            }
        }
    }

    private void moveTowardsContainer(NPC npc, Location containerLocation) {
        try {
            Entity npcEntity = npc.getEntity();
            if (npcEntity == null) {
                return;
            }

            Location npcLoc = npcEntity.getLocation();

            // Calculate direction vector
            Vector direction = containerLocation.toVector().subtract(npcLoc.toVector()).normalize();

            // Apply movement speed (slower than normal pickup movement)
            direction.multiply(0.05); // Scale down for slower, sneaky movement

            // Set NPC velocity (if the NPC supports it)
            if (npcEntity.getVelocity().length() < 0.1) { // Only move if not already moving
                npcEntity.setVelocity(direction);
            }

            // Make NPC look at the container
            Vector lookDirection = containerLocation.toVector().subtract(npcLoc.toVector()).normalize();
            Location lookLocation = npcLoc.clone();
            lookLocation.setDirection(lookDirection);
            npcEntity.teleport(lookLocation);

        } catch (Exception e) {
            if (debugEnabled) {
                plugin.getLogger().warning("Error moving NPC towards container: " + e.getMessage());
            }
        }
    }
}
