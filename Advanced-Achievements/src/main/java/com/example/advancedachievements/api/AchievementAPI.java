package com.example.advancedachievements.api;

import com.example.advancedachievements.AdvancedAchievements;
import com.example.advancedachievements.enums.TaskType;
import com.example.advancedachievements.models.Achievement;
import com.example.advancedachievements.models.PlayerProgress;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AchievementAPI {
    
    private final AdvancedAchievements plugin;
    
    public AchievementAPI(AdvancedAchievements plugin) {
        this.plugin = plugin;
    }
    
    public Achievement createAchievement(String id, String title, String description, String category,
                                       Material icon, TaskType taskType, String taskTarget, 
                                       int requiredAmount, List<String> rewards, int points, 
                                       boolean hidden, List<String> prerequisites) {
        if (plugin.getAchievementManager().achievementExists(id)) {
            return null;
        }
        return plugin.getAchievementManager().createAchievement(id, title, description, category,
            icon, taskType, taskTarget, requiredAmount, rewards, points, hidden, prerequisites);
    }
    
    public Achievement getAchievement(String id) {
        return plugin.getAchievementManager().getAchievement(id);
    }
    
    public Collection<Achievement> getAllAchievements() {
        return plugin.getAchievementManager().getAllAchievements();
    }
    
    public List<Achievement> getAchievementsByCategory(String category) {
        return plugin.getAchievementManager().getAchievementsByCategory(category);
    }
    
    public boolean deleteAchievement(String id) {
        return plugin.getAchievementManager().deleteAchievement(id);
    }
    
    public boolean achievementExists(String id) {
        return plugin.getAchievementManager().achievementExists(id);
    }
    
    public void addProgress(Player player, TaskType taskType, String target, int amount) {
        plugin.getProgressManager().addProgress(player, taskType, target, amount);
    }
    @Deprecated
    public void addCustomProgress(Player player, String customTarget, int amount) {
        plugin.getLogger().warning("addCustomProgress is deprecated. Use addProgress with a specific TaskType instead.");
    }
    
    public PlayerProgress getPlayerProgress(Player player, String achievementId) {
        return plugin.getProgressManager().getPlayerProgress(player, achievementId);
    }
    
    public Map<String, PlayerProgress> getAllPlayerProgress(Player player) {
        return plugin.getProgressManager().getAllPlayerProgress(player);
    }
    
    public void resetPlayerProgress(Player player, String achievementId) {
        plugin.getProgressManager().resetPlayerProgress(player, achievementId);
    }
    
    public void giveAchievement(Player player, String achievementId) {
        plugin.getProgressManager().giveAchievement(player, achievementId);
    }
    
    public boolean hasUnlockedAchievement(Player player, String achievementId) {
        PlayerProgress progress = plugin.getProgressManager().getPlayerProgress(player, achievementId);
        return progress.isUnlocked();
    }
    
    public boolean hasClaimedReward(Player player, String achievementId) {
        return plugin.getRewardManager().hasClaimedReward(player, achievementId);
    }
    
    public boolean claimReward(Player player, String achievementId) {
        return plugin.getRewardManager().claimReward(player, achievementId);
    }
    
    public boolean canClaimReward(Player player, String achievementId) {
        return plugin.getRewardManager().canClaimReward(player, achievementId);
    }
    
    public double getProgressPercentage(Player player, String achievementId) {
        Achievement achievement = getAchievement(achievementId);
        if (achievement == null) return 0.0;
        
        PlayerProgress progress = getPlayerProgress(player, achievementId);
        return Math.min(1.0, (double) progress.getProgress() / achievement.getRequiredAmount());
    }
    
    public java.util.Set<String> getCategories() {
        return plugin.getAchievementManager().getCategories();
    }
    @Deprecated
    public boolean registerCustomTaskType(String taskId, String displayName) {
        plugin.getLogger().warning("Custom task types are no longer supported. Use existing task types instead.");
        return false;
    }
    
    public int getUnlockedCount(Player player) {
        Map<String, PlayerProgress> progress = getAllPlayerProgress(player);
        return (int) progress.values().stream().filter(PlayerProgress::isUnlocked).count();
    }
    
    public int getClaimedCount(Player player) {
        Map<String, PlayerProgress> progress = getAllPlayerProgress(player);
        return (int) progress.values().stream().filter(PlayerProgress::isClaimed).count();
    }
    
    public int getTotalPoints(Player player) {
        Map<String, PlayerProgress> progress = getAllPlayerProgress(player);
        int totalPoints = 0;
        
        for (PlayerProgress p : progress.values()) {
            if (p.isUnlocked()) {
                Achievement achievement = getAchievement(p.getAchievementId());
                if (achievement != null) {
                    totalPoints += achievement.getPoints();
                }
            }
        }
        
        return totalPoints;
    }
}