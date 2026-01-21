package com.example.advancedachievements.events;

import com.example.advancedachievements.models.Achievement;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AchievementUnlockEvent extends Event implements Cancellable {
    
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    
    private final Player player;
    private final Achievement achievement;
    
    public AchievementUnlockEvent(Player player, Achievement achievement) {
        this.player = player;
        this.achievement = achievement;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public Achievement getAchievement() {
        return achievement;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
