package com.example.advancedachievements.managers;

import com.example.advancedachievements.AdvancedAchievements;
import com.example.advancedachievements.models.Achievement;
import com.example.advancedachievements.models.PlayerProgress;
import com.example.advancedachievements.enums.TaskType;
import com.example.advancedachievements.events.AchievementUnlockEvent;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProgressManager {
    
    private final AdvancedAchievements plugin;
    private final Map<UUID, Map<String, PlayerProgress>> playerProgress;
    private final Map<UUID, BossBar> progressBars;
    private final Map<UUID, Boolean> progressBarSettings;

    public ProgressManager(AdvancedAchievements plugin) {
        this.plugin = plugin;
        this.playerProgress = new ConcurrentHashMap<>();
        this.progressBars = new ConcurrentHashMap<>();
        this.progressBarSettings = new ConcurrentHashMap<>();
    }
    
    public void loadPlayerProgress(Player player) {
        UUID playerId = player.getUniqueId();
        
        plugin.getDatabaseManager().loadPlayerProgress(playerId).thenAccept(progress -> {
            playerProgress.put(playerId, progress);
            
            plugin.getDatabaseManager().getProgressBarSetting(playerId).thenAccept(setting -> {
                progressBarSettings.put(playerId, setting);
            });
        });
    }
    
    public void savePlayerProgress(Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, PlayerProgress> progress = playerProgress.get(playerId);
        
        if (progress != null) {
            for (PlayerProgress p : progress.values()) {
                plugin.getDatabaseManager().savePlayerProgress(p);
            }
        }
    }
    
    public void saveAllProgress() {
        for (UUID playerId : playerProgress.keySet()) {
            Map<String, PlayerProgress> progress = playerProgress.get(playerId);
            if (progress != null) {
                for (PlayerProgress p : progress.values()) {
                    plugin.getDatabaseManager().savePlayerProgress(p);
                }
            }
        }
    }
    
    public void unloadPlayerProgress(Player player) {
        UUID playerId = player.getUniqueId();
        savePlayerProgress(player);
        playerProgress.remove(playerId);
        progressBarSettings.remove(playerId);
        
        BossBar bossBar = progressBars.remove(playerId);
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }
    
    public void addProgress(Player player, TaskType taskType, String target, int amount) {
        UUID playerId = player.getUniqueId();
        Map<String, PlayerProgress> progress = playerProgress.get(playerId);
        
        if (progress == null) {
            progress = new ConcurrentHashMap<>();
            playerProgress.put(playerId, progress);
        }
        
        for (Achievement achievement : plugin.getAchievementManager().getAllAchievements()) {
            if (achievement.getTaskType() != taskType) continue;
            
            if (!achievement.getTaskTarget().equalsIgnoreCase("ANY") &&
                !achievement.getTaskTarget().equalsIgnoreCase(target)) {
                continue;
            }
            
            if (!checkPrerequisites(player, achievement)) {
                continue;
            }
            
            PlayerProgress playerProg = progress.computeIfAbsent(achievement.getId(), 
                k -> new PlayerProgress(playerId, achievement.getId()));
            
            if (playerProg.isUnlocked()) continue;
            
            int oldProgress = playerProg.getProgress();
            playerProg.addProgress(amount);
            
            if (playerProg.getProgress() >= achievement.getRequiredAmount()) {
                playerProg.setProgress(achievement.getRequiredAmount());
                playerProg.setUnlocked(true);
                
                AchievementUnlockEvent event = new AchievementUnlockEvent(player, achievement);
                Bukkit.getPluginManager().callEvent(event);
                  if (!event.isCancelled()) {
                    notifyAchievementUnlocked(player, achievement);
                }
            } else {

            }
            
            plugin.getDatabaseManager().savePlayerProgress(playerProg);
        }
    }
    
    private boolean checkPrerequisites(Player player, Achievement achievement) {
        UUID playerId = player.getUniqueId();
        Map<String, PlayerProgress> progress = playerProgress.get(playerId);
        if (progress == null || achievement.getPrerequisites().isEmpty()) {
            return true;
        }
        for (String prereqId : achievement.getPrerequisites()) {
            if (prereqId.isEmpty()) continue;
            PlayerProgress prereqProgress = progress.get(prereqId);
            if (prereqProgress == null || !prereqProgress.isUnlocked()) {
                return false;
            }
        }
        return true;
    }

    public void notifyAchievementUnlocked(Player player, Achievement achievement) {
        player.sendMessage("§a§lAchievement Unlocked!");
        String rewardLine = "§6Reward: ";
        if (achievement.getRewards().isEmpty()) {
            rewardLine += "None";
        } else {
            List<String> formattedRewards = new ArrayList<>();
            for (String reward : achievement.getRewards()) {
                formattedRewards.add(formatReward(reward));
            }
            rewardLine += String.join(" + ", formattedRewards);
        }
        player.sendMessage(rewardLine);
        plugin.getRewardManager().giveRewards(player, achievement);
        if (plugin.getConfigManager().isSoundEnabled()) {
            try {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid sound for achievement unlock");
            }
        }
    }

    private String formatReward(String reward) {
        String[] parts = reward.split(":");
        if (parts.length < 2) return reward;
        String type = parts[0].toUpperCase();
        switch (type) {
            case "ITEM":
                if (parts.length >= 3) {
                    String itemMaterialName = parts[1].replace("_", " ");
                    String[] itemWords = itemMaterialName.split(" ");
                    StringBuilder itemFormattedName = new StringBuilder();
                    for (String word : itemWords) {
                        if (word.length() > 0) {
                            if (itemFormattedName.length() > 0) itemFormattedName.append(" ");
                            itemFormattedName.append(Character.toUpperCase(word.charAt(0)))
                                .append(word.substring(1).toLowerCase());
                        }
                    }
                    return parts[2] + "x " + itemFormattedName.toString();
                }
                String singleItemName = parts[1].replace("_", " ");
                String[] singleWords = singleItemName.split(" ");
                StringBuilder singleFormattedName = new StringBuilder();
                for (String word : singleWords) {
                    if (word.length() > 0) {
                        if (singleFormattedName.length() > 0) singleFormattedName.append(" ");
                        singleFormattedName.append(Character.toUpperCase(word.charAt(0)))
                            .append(word.substring(1).toLowerCase());
                    }
                }
                return singleFormattedName.toString();
            case "XP":
            case "EXPERIENCE":
                return parts[1] + " XP";
            case "MONEY":
                return "$" + parts[1];
            case "COMMAND":
                return "Special Reward";
            default:
                if (parts.length == 2 && type.matches("[A-Z_]+") && parts[1].matches("\\d+")) {
                    String directMaterialName = type.replace("_", " ");
                    String[] directWords = directMaterialName.split(" ");
                    StringBuilder directFormattedName = new StringBuilder();
                    for (String word : directWords) {
                        if (word.length() > 0) {
                            if (directFormattedName.length() > 0) directFormattedName.append(" ");
                            directFormattedName.append(Character.toUpperCase(word.charAt(0)))
                                .append(word.substring(1).toLowerCase());
                        }
                    }
                    return parts[1] + "x " + directFormattedName.toString();
                }
                return reward;
        }
    }

    private void spawnCelebrationFirework(Player player) {
        Location location = player.getLocation().add(0, 1, 0);
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (int i = 0; i < 3; i++) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Firework firework = (Firework) location.getWorld().spawnEntity(
                        location.clone().add(
                            (Math.random() - 0.5) * 4, 
                            Math.random() * 2, 
                            (Math.random() - 0.5) * 4
                        ), 
                        EntityType.FIREWORK
                    );
                    FireworkMeta meta = firework.getFireworkMeta();
                    FireworkEffect effect = FireworkEffect.builder()
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .withColor(Color.YELLOW, Color.ORANGE, Color.WHITE)
                        .withFade(Color.RED, Color.PURPLE)
                        .withFlicker()
                        .withTrail()
                        .build();
                    meta.addEffect(effect);
                    meta.setPower(1);
                    firework.setFireworkMeta(meta);
                    Bukkit.getScheduler().runTaskLater(plugin, firework::detonate, 1L);
                }, i * 5L);
            }
        });
    }

    private void showProgressUpdate(Player player, Achievement achievement, PlayerProgress progress) {
        if (!isProgressBarEnabled(player)) return;
        
        String progressBarType = plugin.getConfigManager().getProgressBarType();
        String message = plugin.getMessageManager().getMessage("progress-updated",
            "current", String.valueOf(progress.getProgress()),
            "required", String.valueOf(achievement.getRequiredAmount()));
          if (progressBarType.equalsIgnoreCase("ACTIONBAR")) {
            player.sendTitle("", message, 0, 20, 0);
        } else if (progressBarType.equalsIgnoreCase("BOSS_BAR")) {
            updateBossBar(player, achievement, progress);
        }
    }
    
    private void updateBossBar(Player player, Achievement achievement, PlayerProgress progress) {
        UUID playerId = player.getUniqueId();
        BossBar bossBar = progressBars.get(playerId);
        
        if (bossBar == null) {
            bossBar = Bukkit.createBossBar(achievement.getTitle(), BarColor.GREEN, BarStyle.SOLID);
            progressBars.put(playerId, bossBar);
            bossBar.addPlayer(player);
        }
          bossBar.setTitle(achievement.getTitle() + " (" + progress.getProgress() + "/" + achievement.getRequiredAmount() + ")");
        double progressPercentage = (double) progress.getProgress() / achievement.getRequiredAmount();
        bossBar.setProgress(Math.min(progressPercentage, 1.0));
        
        final BossBar finalBossBar = bossBar;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (finalBossBar != null) {
                finalBossBar.setVisible(false);
            }
        }, 100L);
    }
    
    private void startProgressBarTask() {
        int interval = plugin.getConfigManager().getProgressBarUpdateInterval();
        
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!isProgressBarEnabled(player)) continue;
                
                Achievement nearestAchievement = findNearestAchievement(player);
                if (nearestAchievement != null) {
                    PlayerProgress progress = getPlayerProgress(player, nearestAchievement.getId());
                    if (progress != null && !progress.isUnlocked()) {
                        showProgressUpdate(player, nearestAchievement, progress);
                    }
                }
            }
        }, interval, interval);
    }
    
    private Achievement findNearestAchievement(Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, PlayerProgress> progress = playerProgress.get(playerId);
        
        if (progress == null) return null;
        
        Achievement nearest = null;
        double bestPercentage = 0;
        
        for (PlayerProgress p : progress.values()) {
            if (p.isUnlocked()) continue;
            
            Achievement achievement = plugin.getAchievementManager().getAchievement(p.getAchievementId());
            if (achievement == null) continue;
            
            double percentage = (double) p.getProgress() / achievement.getRequiredAmount();
            if (percentage > bestPercentage) {
                bestPercentage = percentage;
                nearest = achievement;
            }
        }
        
        return nearest;
    }
    
    public PlayerProgress getPlayerProgress(Player player, String achievementId) {
        UUID playerId = player.getUniqueId();
        Map<String, PlayerProgress> progress = playerProgress.get(playerId);
        
        if (progress == null) {
            progress = new ConcurrentHashMap<>();
            playerProgress.put(playerId, progress);
        }
        
        return progress.computeIfAbsent(achievementId, 
            k -> new PlayerProgress(playerId, achievementId));
    }
    
    public void resetPlayerProgress(Player player, String achievementId) {
        UUID playerId = player.getUniqueId();
        Map<String, PlayerProgress> progress = playerProgress.get(playerId);
        
        if (progress != null) {
            PlayerProgress p = progress.get(achievementId);
            if (p != null) {
                p.resetProgress();
                plugin.getDatabaseManager().savePlayerProgress(p);
            }
        }
        
        plugin.getDatabaseManager().resetPlayerProgress(playerId, achievementId);
    }
    
    public void giveAchievement(Player player, String achievementId) {
        Achievement achievement = plugin.getAchievementManager().getAchievement(achievementId);
        if (achievement == null) return;
        
        PlayerProgress progress = getPlayerProgress(player, achievementId);
        progress.setProgress(achievement.getRequiredAmount());
        progress.setUnlocked(true);
        
        notifyAchievementUnlocked(player, achievement);
        plugin.getDatabaseManager().savePlayerProgress(progress);
    }
    
    public boolean isProgressBarEnabled(Player player) {
        return progressBarSettings.getOrDefault(player.getUniqueId(), true);
    }
    
    public void setProgressBarEnabled(Player player, boolean enabled) {
        UUID playerId = player.getUniqueId();
        progressBarSettings.put(playerId, enabled);
        plugin.getDatabaseManager().setProgressBarSetting(playerId, enabled);
        
        if (!enabled) {
            BossBar bossBar = progressBars.remove(playerId);
            if (bossBar != null) {
                bossBar.removeAll();
            }
        }
    }
      public Map<String, PlayerProgress> getAllPlayerProgress(Player player) {
        UUID playerId = player.getUniqueId();
        return playerProgress.getOrDefault(playerId, new HashMap<>());
    }

    public void showProgressForAchievement(Player player, String achievementId) {
        Achievement achievement = plugin.getAchievementManager().getAchievement(achievementId);
        if (achievement == null) return;
        PlayerProgress progress = getPlayerProgress(player, achievementId);
        if (progress == null) return;
        if (progress.isUnlocked()) {
            plugin.getMessageManager().sendMessage(player, "achievement-already-unlocked",
                "title", achievement.getTitle());
            return;
        }
        showProgressUpdate(player, achievement, progress);
    }

    public void showNearestProgress(Player player) {
        Achievement nearest = findNearestAchievement(player);
        if (nearest != null) {
            showProgressForAchievement(player, nearest.getId());
        } else {
            plugin.getMessageManager().sendMessage(player, "no-progress-found");
        }
    }
}