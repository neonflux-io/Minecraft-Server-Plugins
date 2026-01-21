package com.example.advancedachievements.listeners;

import com.example.advancedachievements.AdvancedAchievements;
import com.example.advancedachievements.commands.AchievementCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    
    private final AdvancedAchievements plugin;
    private final AchievementCommand achievementCommand;
    
    public ChatListener(AdvancedAchievements plugin, AchievementCommand achievementCommand) {
        this.plugin = plugin;
        this.achievementCommand = achievementCommand;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        if (achievementCommand.hasActiveCreationSession(player.getUniqueId())) {
            event.setCancelled(true);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                achievementCommand.handleChatInput(player, message);
            });
        }
    }
}
