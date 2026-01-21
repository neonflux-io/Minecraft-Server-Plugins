package com.example.npcitempickup.managers;

import net.citizensnpcs.api.npc.NPC;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the inventory of items picked up by NPCs
 */
public class NPCInventoryManager {

    private final Plugin plugin;
    private final Map<UUID, List<ItemStack>> npcInventories;
    private final int maxItems;

    public NPCInventoryManager(Plugin plugin) {
        this.plugin = plugin;
        this.npcInventories = new HashMap<>();
        this.maxItems = plugin.getConfig().getInt("pickup.max_items", 64);
    }

    /**
     * Add an item to an NPC's inventory
     * 
     * @param npc  The NPC to add the item to
     * @param item The item to add
     * @return true if the item was added successfully, false if inventory is full
     */
    public boolean addItem(NPC npc, ItemStack item) {
        UUID npcId = npc.getUniqueId();
        List<ItemStack> inventory = npcInventories.computeIfAbsent(npcId, k -> new ArrayList<>());

        // Check if inventory is full
        int totalItems = inventory.stream().mapToInt(ItemStack::getAmount).sum();
        if (totalItems >= maxItems) {
            return false;
        }

        // Try to stack with existing items
        for (ItemStack existingItem : inventory) {
            if (existingItem.isSimilar(item)) {
                int maxStack = existingItem.getMaxStackSize();
                int currentAmount = existingItem.getAmount();
                int canAdd = Math.min(item.getAmount(), maxStack - currentAmount);

                if (canAdd > 0) {
                    existingItem.setAmount(currentAmount + canAdd);
                    item.setAmount(item.getAmount() - canAdd);

                    if (item.getAmount() <= 0) {
                        return true; // Item fully added
                    }
                }
            }
        }

        // Add remaining items as new stack(s)
        if (item.getAmount() > 0) {
            inventory.add(item.clone());
        }

        return true;
    }

    /**
     * Get all items from an NPC's inventory
     * 
     * @param npc The NPC to get items from
     * @return List of items in the NPC's inventory
     */
    public List<ItemStack> getItems(NPC npc) {
        UUID npcId = npc.getUniqueId();
        return npcInventories.getOrDefault(npcId, new ArrayList<>());
    }

    /**
     * Remove all items from an NPC's inventory
     * 
     * @param npc The NPC to clear the inventory of
     * @return List of items that were removed
     */
    public List<ItemStack> clearInventory(NPC npc) {
        UUID npcId = npc.getUniqueId();
        List<ItemStack> items = npcInventories.remove(npcId);
        return items != null ? items : new ArrayList<>();
    }

    /**
     * Check if an NPC has any items in their inventory
     * 
     * @param npc The NPC to check
     * @return true if the NPC has items, false otherwise
     */
    public boolean hasItems(NPC npc) {
        UUID npcId = npc.getUniqueId();
        List<ItemStack> inventory = npcInventories.get(npcId);
        return inventory != null && !inventory.isEmpty();
    }

    /**
     * Get the number of items an NPC is carrying
     * 
     * @param npc The NPC to count items for
     * @return The total number of items
     */
    public int getItemCount(NPC npc) {
        UUID npcId = npc.getUniqueId();
        List<ItemStack> inventory = npcInventories.get(npcId);
        if (inventory == null) {
            return 0;
        }

        return inventory.stream().mapToInt(ItemStack::getAmount).sum();
    }

    /**
     * Check if an NPC can pick up an item
     * 
     * @param npc  The NPC
     * @param item The item to check
     * @return true if the NPC can pick up the item, false otherwise
     */
    public boolean canPickupItem(NPC npc, ItemStack item) {
        // Check if pickup is disabled for this specific NPC
        if (isPickupDisabled(npc)) {
            return false;
        }

        if (!isItemAllowed(item)) {
            return false;
        }

        int currentItems = getItemCount(npc);
        return currentItems < maxItems;
    }

    /**
     * Check if an item is allowed to be picked up based on config filters
     * 
     * @param item The item to check
     * @return true if the item is allowed, false otherwise
     */
    private boolean isItemAllowed(ItemStack item) {
        boolean useWhitelist = plugin.getConfig().getBoolean("item_filter.use_whitelist", false);
        List<String> whitelist = plugin.getConfig().getStringList("item_filter.whitelist");
        List<String> blacklist = plugin.getConfig().getStringList("item_filter.blacklist");

        String itemType = item.getType().name();

        if (useWhitelist) {
            return whitelist.contains(itemType);
        } else {
            return !blacklist.contains(itemType);
        }
    }

    /**
     * Save all inventory data (called on plugin disable)
     */
    public void saveAllData() {
        // In a more advanced implementation, you might save this data to a file
        // For now, we'll just clear the data since it's stored in memory
        if (plugin.getConfig().getBoolean("debug.enabled", false)) {
            plugin.getLogger().info("Saving NPC inventory data for " + npcInventories.size() + " NPCs");
        }
    }

    /**
     * Get all NPCs that have items
     * 
     * @return Map of NPC UUIDs to their item lists
     */
    public Map<UUID, List<ItemStack>> getAllInventories() {
        return new HashMap<>(npcInventories);
    }

    /**
     * Check if pickup is disabled for a specific NPC
     * 
     * @param npc The NPC to check
     * @return true if pickup is disabled for this NPC, false otherwise
     */
    public boolean isPickupDisabled(NPC npc) {
        List<String> disabledNPCs = plugin.getConfig().getStringList("npc_settings.disabled_pickup");
        return disabledNPCs.contains(String.valueOf(npc.getId()));
    }

    /**
     * Disable pickup for a specific NPC
     * 
     * @param npc The NPC to disable pickup for
     */
    public void disablePickup(NPC npc) {
        List<String> disabledNPCs = plugin.getConfig().getStringList("npc_settings.disabled_pickup");
        String npcId = String.valueOf(npc.getId());
        if (!disabledNPCs.contains(npcId)) {
            disabledNPCs.add(npcId);
            plugin.getConfig().set("npc_settings.disabled_pickup", disabledNPCs);
            plugin.saveConfig();
        }
    }

    /**
     * Enable pickup for a specific NPC
     * 
     * @param npc The NPC to enable pickup for
     */
    public void enablePickup(NPC npc) {
        List<String> disabledNPCs = plugin.getConfig().getStringList("npc_settings.disabled_pickup");
        String npcId = String.valueOf(npc.getId());
        disabledNPCs.remove(npcId);
        plugin.getConfig().set("npc_settings.disabled_pickup", disabledNPCs);
        plugin.saveConfig();
    }

    /**
     * Check if an NPC is in thief mode
     * 
     * @param npc The NPC to check
     * @return true if the NPC is in thief mode, false otherwise
     */
    public boolean isThiefMode(NPC npc) {
        if (!plugin.getConfig().getBoolean("thief_mode.enabled", true)) {
            return false;
        }
        List<String> thiefNPCs = plugin.getConfig().getStringList("thief_mode.thief_npcs");
        return thiefNPCs.contains(String.valueOf(npc.getId()));
    }

    /**
     * Enable thief mode for a specific NPC
     * 
     * @param npc The NPC to enable thief mode for
     */
    public void enableThiefMode(NPC npc) {
        List<String> thiefNPCs = plugin.getConfig().getStringList("thief_mode.thief_npcs");
        String npcId = String.valueOf(npc.getId());
        if (!thiefNPCs.contains(npcId)) {
            thiefNPCs.add(npcId);
            plugin.getConfig().set("thief_mode.thief_npcs", thiefNPCs);
            plugin.saveConfig();
        }
    }

    /**
     * Disable thief mode for a specific NPC
     * 
     * @param npc The NPC to disable thief mode for
     */
    public void disableThiefMode(NPC npc) {
        List<String> thiefNPCs = plugin.getConfig().getStringList("thief_mode.thief_npcs");
        String npcId = String.valueOf(npc.getId());
        thiefNPCs.remove(npcId);
        plugin.getConfig().set("thief_mode.thief_npcs", thiefNPCs);
        plugin.saveConfig();
    }
}
