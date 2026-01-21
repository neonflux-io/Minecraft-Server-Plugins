package com.example.advancedachievements.events;

import com.example.advancedachievements.models.Achievement;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AchievementProgressEvent extends Event implements Cancellable {
    
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    
    private final Player player;
    private final Achievement achievement;
    private final int oldProgress;
    private final int newProgress;
    private final int progressAdded;
    
    public AchievementProgressEvent(Player player, Achievement achievement, 
                                  int oldProgress, int newProgress, int progressAdded) {
        this.player = player;
        this.achievement = achievement;
        this.oldProgress = oldProgress;
        this.newProgress = newProgress;
        this.progressAdded = progressAdded;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public Achievement getAchievement() {
        return achievement;
    }
    
    public int getOldProgress() {
        return oldProgress;
    }
    
    public int getNewProgress() {
        return newProgress;
    }
    
    public int getProgressAdded() {
        return progressAdded;
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
