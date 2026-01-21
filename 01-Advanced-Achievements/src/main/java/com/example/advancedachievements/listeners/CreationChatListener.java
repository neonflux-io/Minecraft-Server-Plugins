package com.example.advancedachievements.listeners;

import com.example.advancedachievements.AdvancedAchievements;
import com.example.advancedachievements.commands.AchievementCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class CreationChatListener implements Listener {
    private final AdvancedAchievements plugin;
    private final AchievementCommand achievementCommand;

    public CreationChatListener(AdvancedAchievements plugin, AchievementCommand achievementCommand) {
        this.plugin = plugin;
        this.achievementCommand = achievementCommand;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (achievementCommand.hasActiveCreationSession(player.getUniqueId())) {
            event.setCancelled(true);
            String input = event.getMessage();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                achievementCommand.handleChatInput(player, input);
            });
        }
    }
}
