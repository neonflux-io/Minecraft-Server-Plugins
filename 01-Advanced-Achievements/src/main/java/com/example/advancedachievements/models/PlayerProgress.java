package com.example.advancedachievements.models;

import java.util.UUID;

public class PlayerProgress {
    private UUID playerId;
    private String achievementId;
    private int progress;
    private boolean unlocked;
    private boolean claimed;
    private long unlockedDate;
    private long claimedDate;

    public PlayerProgress(UUID playerId, String achievementId, int progress,
                         boolean unlocked, boolean claimed, long unlockedDate, long claimedDate) {
        this.playerId = playerId;
        this.achievementId = achievementId;
        this.progress = progress;
        this.unlocked = unlocked;
        this.claimed = claimed;
        this.unlockedDate = unlockedDate;
        this.claimedDate = claimedDate;
    }

    public PlayerProgress(UUID playerId, String achievementId) {
        this(playerId, achievementId, 0, false, false, 0, 0);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public String getAchievementId() {
        return achievementId;
    }

    public void setAchievementId(String achievementId) {
        this.achievementId = achievementId;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
        if (unlocked && unlockedDate == 0) {
            this.unlockedDate = System.currentTimeMillis();
        }
    }

    public boolean isClaimed() {
        return claimed;
    }

    public void setClaimed(boolean claimed) {
        this.claimed = claimed;
        if (claimed && claimedDate == 0) {
            this.claimedDate = System.currentTimeMillis();
        }
    }

    public long getUnlockedDate() {
        return unlockedDate;
    }

    public void setUnlockedDate(long unlockedDate) {
        this.unlockedDate = unlockedDate;
    }

    public long getClaimedDate() {
        return claimedDate;
    }

    public void setClaimedDate(long claimedDate) {
        this.claimedDate = claimedDate;
    }

    public void addProgress(int amount) {
        this.progress += amount;
    }

    public void resetProgress() {
        this.progress = 0;
        this.unlocked = false;
        this.claimed = false;
        this.unlockedDate = 0;
        this.claimedDate = 0;
    }
}
