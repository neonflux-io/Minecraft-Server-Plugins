package com.example.advancedachievements.managers;

import com.example.advancedachievements.AdvancedAchievements;
import com.example.advancedachievements.models.Achievement;
import com.example.advancedachievements.enums.TaskType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AchievementManager {
    
    private final AdvancedAchievements plugin;
    private final Map<String, Achievement> achievements;
    private final File achievementsFile;
    private FileConfiguration achievementsConfig;
    
    public AchievementManager(AdvancedAchievements plugin) {
        this.plugin = plugin;
        this.achievements = new ConcurrentHashMap<>();
        this.achievementsFile = new File(plugin.getDataFolder(), "achievements.yml");
        if (!achievementsFile.exists()) {
            plugin.saveResource("achievements.yml", false);
        }
        this.achievementsConfig = YamlConfiguration.loadConfiguration(achievementsFile);
    }
    
    public void loadAchievements() {
        achievements.clear();
        plugin.getDatabaseManager().loadAchievements().thenAccept(dbAchievements -> {
            for (Achievement achievement : dbAchievements) {
                achievements.put(achievement.getId(), achievement);
            }
            loadFromFile();
            plugin.getLogger().info("Loaded " + achievements.size() + " achievements");
        });
    }
    
    private void loadFromFile() {
        ConfigurationSection achievementsSection = achievementsConfig.getConfigurationSection("achievements");
        if (achievementsSection == null) {
            createDefaultAchievements();
            return;
        }
        for (String id : achievementsSection.getKeys(false)) {
            ConfigurationSection section = achievementsSection.getConfigurationSection(id);
            if (section == null) continue;
            try {
                Achievement achievement = createAchievementFromConfig(id, section);
                achievements.put(id, achievement);
                plugin.getDatabaseManager().saveAchievement(achievement);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load achievement " + id + ": " + e.getMessage());
            }
        }
    }
    
    private Achievement createAchievementFromConfig(String id, ConfigurationSection section) {
        String title = section.getString("title", id);
        String description = section.getString("description", "");
        Material icon = Material.valueOf(section.getString("icon", plugin.getConfigManager().getDefaultIcon()));
        TaskType taskType = TaskType.fromId(section.getString("task.type", "custom"));
        String taskTarget = section.getString("task.target", "");
        int requiredAmount = section.getInt("task.amount", 1);
        List<String> rewards = section.getStringList("rewards");
        Map<String, Object> rewardData = new HashMap<>();
        boolean hidden = section.getBoolean("hidden", false);
        List<String> prerequisites = section.getStringList("prerequisites");
        return new Achievement(id, title, description, icon, taskType, taskTarget, requiredAmount, rewards, rewardData, hidden, prerequisites);
    }

    private void createDefaultAchievements() {
        List<Achievement> defaultAchievements = Arrays.asList(
            new Achievement("first_blocks", "First Steps", "Break your first 10 blocks", 
                          Material.COBBLESTONE, TaskType.BLOCK_BREAK, "ANY", 10,
                          Arrays.asList("ITEM:BREAD:5", "XP:100"), new HashMap<>(), false, new ArrayList<>()),
            new Achievement("wood_collector", "Wood Collector", "Collect 64 wood logs",
                          Material.OAK_LOG, TaskType.ITEM_PICKUP, "LOG", 64,
                          Arrays.asList("ITEM:IRON_AXE:1", "XP:200"), new HashMap<>(), false, new ArrayList<>()),
            new Achievement("monster_hunter", "Monster Hunter", "Kill 25 hostile mobs",
                          Material.IRON_SWORD, TaskType.MOB_KILL, "HOSTILE", 25,
                          Arrays.asList("ITEM:GOLDEN_APPLE:3", "XP:500"), new HashMap<>(), false, new ArrayList<>()),
            new Achievement("master_crafter", "Master Crafter", "Craft 100 items",
                          Material.CRAFTING_TABLE, TaskType.ITEM_CRAFT, "ANY", 100,
                          Arrays.asList("ITEM:DIAMOND:5", "XP:1000"), new HashMap<>(), false, new ArrayList<>())
        );
        for (Achievement achievement : defaultAchievements) {
            achievements.put(achievement.getId(), achievement);
            saveAchievementToFile(achievement);
            plugin.getDatabaseManager().saveAchievement(achievement);
        }
        try {
            achievementsConfig.save(achievementsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save achievements.yml: " + e.getMessage());
        }
    }

    public Achievement createAchievement(String id, String title, String description,
                                       Material icon, TaskType taskType, String taskTarget,
                                       int requiredAmount, List<String> rewards,
                                       boolean hidden, List<String> prerequisites) {
        Map<String, Object> rewardData = new HashMap<>();
        Achievement achievement = new Achievement(id, title, description, icon, taskType, taskTarget, requiredAmount, rewards, rewardData, hidden, prerequisites);
        achievements.put(id, achievement);
        saveAchievementToFile(achievement);
        plugin.getDatabaseManager().saveAchievement(achievement);
        return achievement;
    }
    
    private void saveAchievementToFile(Achievement achievement) {
        String path = "achievements." + achievement.getId();
        achievementsConfig.set(path + ".title", achievement.getTitle());
        achievementsConfig.set(path + ".description", achievement.getDescription());
        achievementsConfig.set(path + ".icon", achievement.getIcon().name());
        achievementsConfig.set(path + ".task.type", achievement.getTaskType().getId());
        achievementsConfig.set(path + ".task.target", achievement.getTaskTarget());
        achievementsConfig.set(path + ".task.amount", achievement.getRequiredAmount());
        achievementsConfig.set(path + ".rewards", achievement.getRewards());
        achievementsConfig.set(path + ".hidden", achievement.isHidden());
        achievementsConfig.set(path + ".prerequisites", achievement.getPrerequisites());
        if (!achievement.getRewardData().isEmpty()) {
            for (Map.Entry<String, Object> entry : achievement.getRewardData().entrySet()) {
                achievementsConfig.set(path + ".reward-data." + entry.getKey(), entry.getValue());
            }
        }
        try {
            achievementsConfig.save(achievementsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save achievement to file: " + e.getMessage());
        }
    }
    
    public boolean deleteAchievement(String id) {
        if (!achievements.containsKey(id)) {
            return false;
        }
        achievements.remove(id);
        achievementsConfig.set("achievements." + id, null);
        try {
            achievementsConfig.save(achievementsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save achievements.yml: " + e.getMessage());
        }
        plugin.getDatabaseManager().deleteAchievement(id);
        return true;
    }
    
    public Achievement getAchievement(String id) {
        return achievements.get(id);
    }
    
    public Collection<Achievement> getAllAchievements() {
        return achievements.values();
    }
    
    public List<Achievement> getVisibleAchievements() {
        return achievements.values().stream()
                .filter(a -> !a.isHidden())
                .sorted(Comparator.comparing(Achievement::getId))
                .toList();
    }
    
    public boolean achievementExists(String id) {
        return achievements.containsKey(id);
    }
    
    public void reloadAchievements() {
        achievementsConfig = YamlConfiguration.loadConfiguration(achievementsFile);
        loadAchievements();
    }
}
