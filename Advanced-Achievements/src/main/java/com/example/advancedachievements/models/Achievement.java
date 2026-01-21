package com.example.advancedachievements.models;

import com.example.advancedachievements.enums.TaskType;
import org.bukkit.Material;

import java.util.List;
import java.util.Map;

public class Achievement {
    private String id;
    private String title;
    private String description;
    private String category;
    private Material icon;
    private TaskType taskType;
    private String taskTarget;
    private int requiredAmount;
    private List<String> rewards;
    private Map<String, Object> rewardData;
    private int points;
    private boolean hidden;
    private List<String> prerequisites;

    public Achievement(String id, String title, String description, String category,
                      Material icon, TaskType taskType, String taskTarget, 
                      int requiredAmount, List<String> rewards, Map<String, Object> rewardData,
                      int points, boolean hidden, List<String> prerequisites) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.icon = icon;
        this.taskType = taskType;
        this.taskTarget = taskTarget;
        this.requiredAmount = requiredAmount;
        this.rewards = rewards;
        this.rewardData = rewardData;
        this.points = points;
        this.hidden = hidden;
        this.prerequisites = prerequisites;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Material getIcon() {
        return icon;
    }

    public void setIcon(Material icon) {
        this.icon = icon;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public String getTaskTarget() {
        return taskTarget;
    }

    public void setTaskTarget(String taskTarget) {
        this.taskTarget = taskTarget;
    }

    public int getRequiredAmount() {
        return requiredAmount;
    }

    public void setRequiredAmount(int requiredAmount) {
        this.requiredAmount = requiredAmount;
    }

    public List<String> getRewards() {
        return rewards;
    }

    public void setRewards(List<String> rewards) {
        this.rewards = rewards;
    }

    public Map<String, Object> getRewardData() {
        return rewardData;
    }

    public void setRewardData(Map<String, Object> rewardData) {
        this.rewardData = rewardData;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public List<String> getPrerequisites() {
        return prerequisites;
    }

    public void setPrerequisites(List<String> prerequisites) {
        this.prerequisites = prerequisites;
    }
}
