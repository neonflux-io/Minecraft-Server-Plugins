package com.example.advancedachievements.managers;

import com.example.advancedachievements.AdvancedAchievements;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class MessageManager {
    
    private final AdvancedAchievements plugin;
    private final ConfigManager configManager;
    
    public MessageManager(AdvancedAchievements plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }
    
    public void sendMessage(Player player, String key, String... placeholders) {
        String message = configManager.getMessage(key, placeholders);
        String prefix = configManager.getMessage("prefix");
        player.sendMessage(colorize(prefix + message));
    }
    
    public void sendMessageWithoutPrefix(Player player, String key, String... placeholders) {
        String message = configManager.getMessage(key, placeholders);
        player.sendMessage(colorize(message));
    }
    
    public void sendRawMessage(Player player, String message) {
        player.sendMessage(colorize(message));
    }
    
    public void broadcastMessage(String key, String... placeholders) {
        String message = configManager.getMessage(key, placeholders);
        String prefix = configManager.getMessage("prefix");
        plugin.getServer().broadcastMessage(colorize(prefix + message));
    }
    
    public String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public String getMessage(String key, String... placeholders) {
        return colorize(configManager.getMessage(key, placeholders));
    }
    
    public String getMessageWithoutColor(String key, String... placeholders) {
        return configManager.getMessage(key, placeholders);
    }
}
