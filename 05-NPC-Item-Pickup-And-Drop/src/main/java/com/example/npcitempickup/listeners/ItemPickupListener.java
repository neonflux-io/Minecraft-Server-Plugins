package com.example.npcitempickup.listeners;

import com.example.npcitempickup.NPCItemPickupPlugin;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener for item-related events
 */
public class ItemPickupListener implements Listener {

    private final NPCItemPickupPlugin plugin;

    public ItemPickupListener(NPCItemPickupPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Prevent players from picking up items that NPCs are targeting
     * This ensures NPCs have a fair chance to pick up items
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!plugin.isPluginEnabled()) {
            return;
        }

        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }

        Item item = event.getItem();

        // Check if any nearby NPC is interested in this item
        boolean npcInterested = false;
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (!npc.isSpawned() || npc.getEntity() == null) {
                continue;
            }

            double distance = npc.getEntity().getLocation().distance(item.getLocation());
            double pickupRadius = plugin.getConfig().getDouble("pickup.radius", 5.0);

            if (distance <= pickupRadius &&
                    plugin.getInventoryManager().canPickupItem(npc, item.getItemStack())) {
                npcInterested = true;
                break;
            }
        }

        // If an NPC is interested and close, give them priority
        if (npcInterested) {
            // Small delay to give NPC a chance
            item.setPickupDelay(10); // 0.5 seconds
        }
    }

    /**
     * Handle when players drop items
     * This can be used for additional functionality if needed
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!plugin.isPluginEnabled()) {
            return;
        }

        // For now, we don't need special handling for dropped items
        // But this could be extended to make NPCs more likely to pick up
        // items dropped by players, or to add special behaviors

        if (plugin.getConfig().getBoolean("debug.enabled", false)) {
            Player player = event.getPlayer();
            ItemStack item = event.getItemDrop().getItemStack();
            plugin.getLogger().info("Player " + player.getName() + " dropped " +
                    item.getAmount() + "x " + item.getType().name());
        }
    }
}
