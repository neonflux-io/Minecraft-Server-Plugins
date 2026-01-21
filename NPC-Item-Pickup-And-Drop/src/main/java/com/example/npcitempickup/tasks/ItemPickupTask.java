package com.example.npcitempickup.tasks;

import com.example.npcitempickup.NPCItemPickupPlugin;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Collection;

/**
 * Task that runs periodically to check for items that NPCs can pick up
 */
public class ItemPickupTask extends BukkitRunnable {

    private final NPCItemPickupPlugin plugin;
    private final double pickupRadius;
    private final boolean moveToItems;
    private final double movementSpeed;
    private final boolean debugEnabled;

    public ItemPickupTask(NPCItemPickupPlugin plugin) {
        this.plugin = plugin;
        this.pickupRadius = plugin.getConfig().getDouble("pickup.radius", 5.0);
        this.moveToItems = plugin.getConfig().getBoolean("pickup.move_to_items", true);
        this.movementSpeed = plugin.getConfig().getDouble("pickup.movement_speed", 1.2);
        this.debugEnabled = plugin.getConfig().getBoolean("debug.enabled", false);
    }

    @Override
    public void run() {
        // Only run if plugin is enabled
        if (!plugin.isPluginEnabled()) {
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

                Entity npcEntity = npc.getEntity();
                if (npcEntity == null) {
                    continue;
                }

                Location npcLocation = npcEntity.getLocation();

                // Find nearby items
                Collection<Entity> nearbyEntities = npcLocation.getWorld()
                        .getNearbyEntities(npcLocation, pickupRadius, pickupRadius, pickupRadius);

                for (Entity entity : nearbyEntities) {
                    if (!(entity instanceof Item)) {
                        continue;
                    }

                    Item item = (Item) entity;
                    ItemStack itemStack = item.getItemStack();

                    // Check if NPC can pick up this item
                    if (!plugin.getInventoryManager().canPickupItem(npc, itemStack)) {
                        continue;
                    }

                    // Check if item is on ground and pickupable
                    if (item.getPickupDelay() > 0) {
                        continue;
                    }

                    double distance = npcLocation.distance(item.getLocation());

                    // If NPC is close enough, pick up the item
                    if (distance <= 1.5) {
                        pickupItem(npc, item);
                    } else if (moveToItems && distance <= pickupRadius) {
                        // Move NPC towards the item
                        moveTowardsItem(npc, item);
                    }
                }
            }
        } catch (Exception e) {
            if (debugEnabled) {
                plugin.getLogger().warning("Error in pickup task: " + e.getMessage());
            }
        }
    }

    private void pickupItem(NPC npc, Item item) {
        try {
            ItemStack itemStack = item.getItemStack();

            // Try to add item to NPC inventory
            if (plugin.getInventoryManager().addItem(npc, itemStack)) {
                // Remove the item from the world
                item.remove();

                if (debugEnabled && plugin.getConfig().getBoolean("debug.log_pickups", true)) {
                    plugin.getLogger().info("NPC " + npc.getName() + " picked up " +
                            itemStack.getAmount() + "x " + itemStack.getType().name());
                }

                // Play pickup sound effect (optional)
                if (npc.getEntity() != null) {
                    npc.getEntity().getWorld().playSound(
                            npc.getEntity().getLocation(),
                            org.bukkit.Sound.ENTITY_ITEM_PICKUP,
                            0.2f,
                            1.0f);
                }
            }
        } catch (Exception e) {
            if (debugEnabled) {
                plugin.getLogger().warning("Error picking up item: " + e.getMessage());
            }
        }
    }

    private void moveTowardsItem(NPC npc, Item item) {
        try {
            Entity npcEntity = npc.getEntity();
            if (npcEntity == null) {
                return;
            }

            Location npcLoc = npcEntity.getLocation();
            Location itemLoc = item.getLocation();

            // Calculate direction vector
            Vector direction = itemLoc.toVector().subtract(npcLoc.toVector()).normalize();

            // Apply movement speed
            direction.multiply(movementSpeed * 0.1); // Scale down for smooth movement

            // Set NPC velocity (if the NPC supports it)
            if (npcEntity.getVelocity().length() < 0.1) { // Only move if not already moving
                npcEntity.setVelocity(direction);
            }

            // Make NPC look at the item
            Vector lookDirection = itemLoc.toVector().subtract(npcLoc.toVector()).normalize();
            Location lookLocation = npcLoc.clone();
            lookLocation.setDirection(lookDirection);
            npcEntity.teleport(lookLocation);

        } catch (Exception e) {
            if (debugEnabled) {
                plugin.getLogger().warning("Error moving NPC towards item: " + e.getMessage());
            }
        }
    }
}
