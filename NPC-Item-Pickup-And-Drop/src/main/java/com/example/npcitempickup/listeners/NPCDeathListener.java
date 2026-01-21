package com.example.npcitempickup.listeners;

import com.example.npcitempickup.NPCItemPickupPlugin;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCDeathEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;

/**
 * Listener for NPC death events to handle item dropping
 */
public class NPCDeathListener implements Listener {

    private final NPCItemPickupPlugin plugin;
    private final Random random;

    public NPCDeathListener(NPCItemPickupPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    /**
     * Handle NPC death from Citizens2 API
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onNPCDeath(NPCDeathEvent event) {
        if (!plugin.isPluginEnabled()) {
            return;
        }

        NPC npc = event.getNPC();
        handleNPCDeath(npc);
    }

    /**
     * Handle entity death (backup method in case NPCDeathEvent doesn't fire)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!plugin.isPluginEnabled()) {
            return;
        }

        Entity entity = event.getEntity();
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);

        if (npc != null) {
            handleNPCDeath(npc);
        }
    }

    private void handleNPCDeath(NPC npc) {
        if (!plugin.getConfig().getBoolean("drop.enabled", true)) {
            return;
        }

        // Check if NPC has any items
        if (!plugin.getInventoryManager().hasItems(npc)) {
            return;
        }

        // Check drop chance
        double dropChance = plugin.getConfig().getDouble("drop.drop_chance", 1.0);
        if (random.nextDouble() > dropChance) {
            // Clear inventory without dropping (unlucky!)
            plugin.getInventoryManager().clearInventory(npc);
            return;
        }

        // Get NPC location
        Location dropLocation;
        if (npc.isSpawned() && npc.getEntity() != null) {
            dropLocation = npc.getEntity().getLocation();
        } else {
            // Fallback to stored location if available
            dropLocation = npc.getStoredLocation();
        }

        if (dropLocation == null) {
            return;
        }

        // Get all items from NPC inventory
        List<ItemStack> items = plugin.getInventoryManager().clearInventory(npc);

        if (items.isEmpty()) {
            return;
        }

        // Drop items
        dropItems(items, dropLocation);

        if (plugin.getConfig().getBoolean("debug.enabled", false) &&
                plugin.getConfig().getBoolean("debug.log_drops", true)) {
            plugin.getLogger().info("NPC " + npc.getName() + " died and dropped " +
                    items.size() + " item stacks");
        }
    }

    private void dropItems(List<ItemStack> items, Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        boolean scatterItems = plugin.getConfig().getBoolean("drop.scatter_items", true);
        double scatterRadius = plugin.getConfig().getDouble("drop.scatter_radius", 2.0);

        for (ItemStack item : items) {
            if (item == null || item.getAmount() <= 0) {
                continue;
            }

            Location dropLoc = location.clone();

            if (scatterItems && scatterRadius > 0) {
                // Randomly scatter items around the death location
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = random.nextDouble() * scatterRadius;

                double offsetX = Math.cos(angle) * distance;
                double offsetZ = Math.sin(angle) * distance;

                dropLoc.add(offsetX, 0, offsetZ);

                // Ensure the location is safe (not inside blocks)
                dropLoc = findSafeDropLocation(dropLoc);
            }

            // Drop the item
            org.bukkit.entity.Item droppedItem = world.dropItemNaturally(dropLoc, item);

            // Add some random velocity for more natural looking drops
            if (scatterItems) {
                Vector velocity = new Vector(
                        (random.nextDouble() - 0.5) * 0.2,
                        random.nextDouble() * 0.1 + 0.1,
                        (random.nextDouble() - 0.5) * 0.2);
                droppedItem.setVelocity(velocity);
            }

            // Set pickup delay so items don't immediately get picked up again
            droppedItem.setPickupDelay(40); // 2 seconds
        }
    }

    private Location findSafeDropLocation(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return location;
        }

        // Check if current location is safe
        if (world.getBlockAt(location).isEmpty() &&
                world.getBlockAt(location.clone().add(0, 1, 0)).isEmpty()) {
            return location;
        }

        // Try to find a safe location nearby
        for (int y = 0; y <= 3; y++) {
            Location testLoc = location.clone().add(0, y, 0);
            if (world.getBlockAt(testLoc).isEmpty() &&
                    world.getBlockAt(testLoc.clone().add(0, 1, 0)).isEmpty()) {
                return testLoc;
            }
        }

        // If no safe location found, use original location
        return location;
    }
}
