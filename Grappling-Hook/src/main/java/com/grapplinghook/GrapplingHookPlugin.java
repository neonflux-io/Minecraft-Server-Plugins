package com.grapplinghook;

import com.grapplinghook.commands.GrapplingHookCommand;
import com.grapplinghook.listeners.FishingRodListener;
import com.grapplinghook.managers.PlayerDataManager;
import org.bukkit.plugin.java.JavaPlugin;

public class GrapplingHookPlugin extends JavaPlugin {

    private PlayerDataManager playerDataManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        playerDataManager = new PlayerDataManager(this);
        
        GrapplingHookCommand grapplingHookCommand = new GrapplingHookCommand(this, playerDataManager);
        getCommand("grapplinghook").setExecutor(grapplingHookCommand);
        getCommand("grapplinghook").setTabCompleter(grapplingHookCommand);
        
        getServer().getPluginManager().registerEvents(new FishingRodListener(this, playerDataManager), this);
        
        getLogger().info("Grappling Hook plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAllData();
        }
        
        getLogger().info("Grappling Hook plugin has been disabled!");
    }
    
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
}
