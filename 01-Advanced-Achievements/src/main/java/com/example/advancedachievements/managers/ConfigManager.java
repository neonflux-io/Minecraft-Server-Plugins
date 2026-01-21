package com.example.advancedachievements.managers;

import com.example.advancedachievements.AdvancedAchievements;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ConfigManager {
    
    private final AdvancedAchievements plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private File configFile;
    private File messagesFile;
    
    public ConfigManager(AdvancedAchievements plugin) {
        this.plugin = plugin;
        loadConfigs();
    }
    
    private void loadConfigs() {
        plugin.saveDefaultConfig();
        configFile = new File(plugin.getDataFolder(), "config.yml");
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        config = plugin.getConfig();
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    public void reloadConfigs() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config.yml: " + e.getMessage());
        }
    }
    
    public void saveMessages() {
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save messages.yml: " + e.getMessage());
        }
    }
    
    public String getDatabaseType() {
        return config.getString("database.type", "SQLite");
    }
    
    public String getSQLiteFile() {
        return config.getString("database.sqlite.file", "achievements.db");
    }
    
    public String getMySQLHost() {
        return config.getString("database.mysql.host", "localhost");
    }
    
    public int getMySQLPort() {
        return config.getInt("database.mysql.port", 3306);
    }
    
    public String getMySQLDatabase() {
        return config.getString("database.mysql.database", "achievements");
    }
    
    public String getMySQLUsername() {
        return config.getString("database.mysql.username", "root");
    }
    
    public String getMySQLPassword() {
        return config.getString("database.mysql.password", "");
    }
    
    public boolean isProgressBarEnabled() {
        return config.getBoolean("progress-bar.enabled", true);
    }
    
    public String getProgressBarType() {
        return config.getString("progress-bar.type", "ACTIONBAR");
    }
    
    public int getProgressBarUpdateInterval() {
        return config.getInt("progress-bar.update-interval", 20);
    }
    
    public boolean isSoundEnabled() {
        return config.getBoolean("notifications.sound.enabled", true);
    }
    
    public String getNotificationSound() {
        return config.getString("notifications.sound.sound", "ENTITY_PLAYER_LEVELUP");
    }
    
    public float getSoundVolume() {
        return (float) config.getDouble("notifications.sound.volume", 1.0);
    }
    
    public float getSoundPitch() {
        return (float) config.getDouble("notifications.sound.pitch", 1.0);
    }
    
    public boolean isTitleEnabled() {
        return config.getBoolean("notifications.title.enabled", true);
    }
    
    public int getTitleFadeIn() {
        return config.getInt("notifications.title.fade-in", 10);
    }
    
    public int getTitleStay() {
        return config.getInt("notifications.title.stay", 70);
    }
    
    public int getTitleFadeOut() {
        return config.getInt("notifications.title.fade-out", 20);
    }
    
    public boolean isChatEnabled() {
        return config.getBoolean("notifications.chat.enabled", true);
    }
    
    public boolean isBroadcastEnabled() {
        return config.getBoolean("notifications.chat.broadcast", true);
    }
    
    public boolean isFireworkEnabled() {
        return config.getBoolean("notifications.firework.enabled", true);
    }
    
    public int getGUIRows() {
        return config.getInt("gui.rows", 6);
    }
    
    public int getItemsPerPage() {
        return config.getInt("gui.items-per-page", 45);
    }
    
    public boolean isGUIAutoRefresh() {
        return config.getBoolean("gui.auto-refresh", true);
    }
    
    public int getGUIRefreshInterval() {
        return config.getInt("gui.refresh-interval", 60);
    }
    
    public String getDefaultIcon() {
        return config.getString("defaults.icon", "PAPER");
    }
    
    public String getMessage(String key) {
        return messages.getString(key, "Message not found: " + key);
    }
    
    public String getMessage(String key, String... placeholders) {
        String message = getMessage(key);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        return message;
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    public FileConfiguration getMessages() {
        return messages;
    }
}
