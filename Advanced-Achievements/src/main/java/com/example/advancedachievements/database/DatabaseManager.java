package com.example.advancedachievements.database;

import com.example.advancedachievements.AdvancedAchievements;
import com.example.advancedachievements.models.Achievement;
import com.example.advancedachievements.models.PlayerProgress;
import com.example.advancedachievements.enums.TaskType;
import org.bukkit.Material;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {
    
    private final AdvancedAchievements plugin;
    private Connection connection;
    private final String databaseType;
    
    public DatabaseManager(AdvancedAchievements plugin) {
        this.plugin = plugin;
        this.databaseType = plugin.getConfigManager().getDatabaseType();
    }
    
    public void initialize() {
        try {
            if (databaseType.equalsIgnoreCase("MySQL")) {
                setupMySQL();
            } else {
                setupSQLite();
            }
            createTables();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupSQLite() throws SQLException {
        File databaseFile = new File(plugin.getDataFolder(), plugin.getConfigManager().getSQLiteFile());
        if (!databaseFile.exists()) {
            databaseFile.getParentFile().mkdirs();
        }
        
        String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
        connection = DriverManager.getConnection(url);
    }
    
    private void setupMySQL() throws SQLException {
        String host = plugin.getConfigManager().getMySQLHost();
        int port = plugin.getConfigManager().getMySQLPort();
        String database = plugin.getConfigManager().getMySQLDatabase();
        String username = plugin.getConfigManager().getMySQLUsername();
        String password = plugin.getConfigManager().getMySQLPassword();
        
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";
        connection = DriverManager.getConnection(url, username, password);
    }
    
    private void createTables() throws SQLException {
        String achievementsTable = "CREATE TABLE IF NOT EXISTS achievements (" +
                "id VARCHAR(255) PRIMARY KEY," +
                "title VARCHAR(255) NOT NULL," +
                "description TEXT," +
                "icon VARCHAR(100)," +
                "task_type VARCHAR(50)," +
                "task_target VARCHAR(255)," +
                "required_amount INTEGER," +
                "rewards TEXT," +
                "reward_data TEXT," +
                "hidden BOOLEAN DEFAULT FALSE," +
                "prerequisites TEXT" +
                ")";
        String progressTable = "CREATE TABLE IF NOT EXISTS player_progress (" +
                "player_id VARCHAR(36)," +
                "achievement_id VARCHAR(255)," +
                "progress INTEGER DEFAULT 0," +
                "unlocked BOOLEAN DEFAULT FALSE," +
                "claimed BOOLEAN DEFAULT FALSE," +
                "unlocked_date BIGINT DEFAULT 0," +
                "claimed_date BIGINT DEFAULT 0," +
                "PRIMARY KEY (player_id, achievement_id)" +
                ")";
        String settingsTable = "CREATE TABLE IF NOT EXISTS player_settings (" +
                "player_id VARCHAR(36) PRIMARY KEY," +
                "progress_bar_enabled BOOLEAN DEFAULT TRUE," +
                "notifications_enabled BOOLEAN DEFAULT TRUE" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(achievementsTable);
            stmt.execute(progressTable);
            stmt.execute(settingsTable);
        }
    }
    
    public CompletableFuture<Void> saveAchievement(Achievement achievement) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT OR REPLACE INTO achievements (id, title, description, icon, task_type, task_target, required_amount, rewards, reward_data, hidden, prerequisites) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, achievement.getId());
                stmt.setString(2, achievement.getTitle());
                stmt.setString(3, achievement.getDescription());
                stmt.setString(4, achievement.getIcon().name());
                stmt.setString(5, achievement.getTaskType().getId());
                stmt.setString(6, achievement.getTaskTarget());
                stmt.setInt(7, achievement.getRequiredAmount());
                stmt.setString(8, String.join(",", achievement.getRewards()));
                stmt.setString(9, serializeMap(achievement.getRewardData()));
                stmt.setBoolean(10, achievement.isHidden());
                stmt.setString(11, String.join(",", achievement.getPrerequisites()));
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save achievement: " + e.getMessage());
            }
        });
    }
    
    public CompletableFuture<List<Achievement>> loadAchievements() {
        return CompletableFuture.supplyAsync(() -> {
            List<Achievement> achievements = new ArrayList<>();
            String sql = "SELECT * FROM achievements";
            
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    Achievement achievement = new Achievement(
                            rs.getString("id"),
                            rs.getString("title"),
                            rs.getString("description"),
                            Material.valueOf(rs.getString("icon")),
                            TaskType.fromId(rs.getString("task_type")),
                            rs.getString("task_target"),
                            rs.getInt("required_amount"),
                            Arrays.asList(rs.getString("rewards").split(",")),
                            deserializeMap(rs.getString("reward_data")),
                            rs.getBoolean("hidden"),
                            Arrays.asList(rs.getString("prerequisites").split(","))
                    );
                    achievements.add(achievement);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load achievements: " + e.getMessage());
            }
            
            return achievements;
        });
    }
    
    public CompletableFuture<Void> deleteAchievement(String id) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM achievements WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, id);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete achievement: " + e.getMessage());
            }
        });
    }
    
    public CompletableFuture<Void> savePlayerProgress(PlayerProgress progress) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT OR REPLACE INTO player_progress (player_id, achievement_id, progress, " +
                    "unlocked, claimed, unlocked_date, claimed_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, progress.getPlayerId().toString());
                stmt.setString(2, progress.getAchievementId());
                stmt.setInt(3, progress.getProgress());
                stmt.setBoolean(4, progress.isUnlocked());
                stmt.setBoolean(5, progress.isClaimed());
                stmt.setLong(6, progress.getUnlockedDate());
                stmt.setLong(7, progress.getClaimedDate());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save player progress: " + e.getMessage());
            }
        });
    }
    
    public CompletableFuture<Map<String, PlayerProgress>> loadPlayerProgress(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, PlayerProgress> progressMap = new HashMap<>();
            String sql = "SELECT * FROM player_progress WHERE player_id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    PlayerProgress progress = new PlayerProgress(
                            UUID.fromString(rs.getString("player_id")),
                            rs.getString("achievement_id"),
                            rs.getInt("progress"),
                            rs.getBoolean("unlocked"),
                            rs.getBoolean("claimed"),
                            rs.getLong("unlocked_date"),
                            rs.getLong("claimed_date")
                    );
                    progressMap.put(progress.getAchievementId(), progress);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load player progress: " + e.getMessage());
            }
            
            return progressMap;
        });
    }
    
    public CompletableFuture<Void> resetPlayerProgress(UUID playerId, String achievementId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM player_progress WHERE player_id = ? AND achievement_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, achievementId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to reset player progress: " + e.getMessage());
            }
        });
    }
    
    public CompletableFuture<Boolean> getProgressBarSetting(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT progress_bar_enabled FROM player_settings WHERE player_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getBoolean("progress_bar_enabled");
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get progress bar setting: " + e.getMessage());
            }
            return true;
        });
    }
    
    public CompletableFuture<Void> setProgressBarSetting(UUID playerId, boolean enabled) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT OR REPLACE INTO player_settings (player_id, progress_bar_enabled) VALUES (?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.setBoolean(2, enabled);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to set progress bar setting: " + e.getMessage());
            }
        });
    }
    
    private String serializeMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
        }
        return sb.toString();
    }
    
    private Map<String, Object> deserializeMap(String serialized) {
        Map<String, Object> map = new HashMap<>();
        if (serialized == null || serialized.isEmpty()) {
            return map;
        }
        
        String[] pairs = serialized.split(";");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                map.put(keyValue[0], keyValue[1]);
            }
        }
        return map;
    }
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close database connection: " + e.getMessage());
        }
    }
    
    public Connection getConnection() {
        return connection;
    }
}
