package com.example.npcitempickup;

import com.example.npcitempickup.commands.NPCPickupCommand;
import com.example.npcitempickup.listeners.ItemPickupListener;
import com.example.npcitempickup.listeners.NPCDeathListener;
import com.example.npcitempickup.managers.NPCInventoryManager;
import com.example.npcitempickup.tasks.ItemPickupTask;
import com.example.npcitempickup.tasks.ChestTheftTask;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for NPCItemPickup
 * Allows Citizens2 NPCs to pick up items and drop them when killed
 */
public class NPCItemPickupPlugin extends JavaPlugin {

    private NPCInventoryManager inventoryManager;
    private ItemPickupTask pickupTask;
    private ChestTheftTask theftTask;
    private boolean pluginEnabled;

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();

        // Initialize managers
        inventoryManager = new NPCInventoryManager(this);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new ItemPickupListener(this), this);
        getServer().getPluginManager().registerEvents(new NPCDeathListener(this), this);

        // Register commands
        getCommand("npcpickup").setExecutor(new NPCPickupCommand(this));

        // Start pickup task
        startPickupTask();

        // Start theft task
        startTheftTask();

        pluginEnabled = getConfig().getBoolean("enabled", true);

        getLogger().info("NPCItemPickup has been enabled!");
    }

    @Override
    public void onDisable() {
        // Stop pickup task
        if (pickupTask != null) {
            pickupTask.cancel();
        }

        // Stop theft task
        if (theftTask != null) {
            theftTask.cancel();
        }

        // Save any pending data
        if (inventoryManager != null) {
            inventoryManager.saveAllData();
        }

        getLogger().info("NPCItemPickup has been disabled!");
    }

    private void startPickupTask() {
        if (pickupTask != null) {
            pickupTask.cancel();
        }

        int delay = getConfig().getInt("pickup.delay", 20);
        pickupTask = new ItemPickupTask(this);
        pickupTask.runTaskTimer(this, 0L, delay);
    }

    private void startTheftTask() {
        if (theftTask != null) {
            theftTask.cancel();
        }

        if (getConfig().getBoolean("thief_mode.enabled", true)) {
            int delay = getConfig().getInt("thief_mode.theft_delay", 10);
            theftTask = new ChestTheftTask(this);
            theftTask.runTaskTimer(this, 0L, delay);
        }
    }

    public void reload() {
        reloadConfig();
        pluginEnabled = getConfig().getBoolean("enabled", true);

        // Restart pickup task with new delay
        startPickupTask();

        // Restart theft task with new settings
        startTheftTask();

        getLogger().info("NPCItemPickup configuration reloaded!");
    }

    public NPCInventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public boolean isPluginEnabled() {
        return pluginEnabled;
    }

    public void setPluginEnabled(boolean enabled) {
        this.pluginEnabled = enabled;
        getConfig().set("enabled", enabled);
        saveConfig();
    }
}
