package com.example.advancedachievements;

import com.example.advancedachievements.commands.AchievementCommand;
import com.example.advancedachievements.database.DatabaseManager;
import com.example.advancedachievements.listeners.AchievementListener;
import com.example.advancedachievements.managers.AchievementManager;
import com.example.advancedachievements.managers.ConfigManager;
import com.example.advancedachievements.managers.MessageManager;
import com.example.advancedachievements.managers.ProgressManager;
import com.example.advancedachievements.managers.RewardManager;
import com.example.advancedachievements.gui.AchievementGUI;
import com.example.advancedachievements.api.AchievementAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class AdvancedAchievements extends JavaPlugin {
    
    private static AdvancedAchievements instance;
    private Logger logger;
    
    private ConfigManager configManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private AchievementManager achievementManager;
    private ProgressManager progressManager;
    private RewardManager rewardManager;
    private AchievementGUI achievementGUI;
    private AchievementAPI achievementAPI;
    
    private Object economy;

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        
        logger.info("Enabling AdvancedAchievements...");
        
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);
        databaseManager = new DatabaseManager(this);
        achievementManager = new AchievementManager(this);
        progressManager = new ProgressManager(this);
        rewardManager = new RewardManager(this);
        achievementGUI = new AchievementGUI(this);
        achievementAPI = new AchievementAPI(this);
        
        setupEconomy();
        
        databaseManager.initialize();
        
        achievementManager.loadAchievements();
        AchievementCommand achievementCommand = new AchievementCommand(this);
        getCommand("achievementadmin").setExecutor(achievementCommand);
        Bukkit.getPluginManager().registerEvents(new AchievementListener(this), this);
        Bukkit.getPluginManager().registerEvents(new com.example.advancedachievements.listeners.CreationChatListener(this, achievementCommand), this);
        
        logger.info("AdvancedAchievements has been enabled!");
    }
    
    @Override
    public void onDisable() {
        if (progressManager != null) {
            progressManager.saveAllProgress();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        logger.info("AdvancedAchievements has been disabled!");
    }
    
    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            logger.info("Vault not found - economy features will be disabled");
            return;
        }

        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> rsp = getServer().getServicesManager().getRegistration(economyClass);
            if (rsp == null) {
                logger.info("No economy provider found - economy features will be disabled");
                return;
            }

            economy = rsp.getProvider();
            logger.info("Economy integration enabled with " + economy.getClass().getSimpleName());
        } catch (ClassNotFoundException e) {
            logger.info("Vault Economy class not found - economy features will be disabled");
        }
    }
    
    public static AdvancedAchievements getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public AchievementManager getAchievementManager() {
        return achievementManager;
    }
    
    public ProgressManager getProgressManager() {
        return progressManager;
    }
    
    public RewardManager getRewardManager() {
        return rewardManager;
    }
    
    public AchievementGUI getAchievementGUI() {
        return achievementGUI;
    }
    public AchievementAPI getAchievementAPI() {
        return achievementAPI;
    }
    
    public Object getEconomy() {
        return economy;
    }
    public boolean hasEconomy() {
        return economy != null;
    }

    public boolean depositPlayer(String playerName, double amount) {
        if (!hasEconomy()) return false;

        try {
            Object response = economy.getClass().getMethod("depositPlayer", String.class, double.class)
                    .invoke(economy, playerName, amount);
            return (Boolean) response.getClass().getMethod("transactionSuccess").invoke(response);
        } catch (Exception e) {
            logger.warning("Failed to deposit money for " + playerName + ": " + e.getMessage());
            return false;
        }
    }
}