package com.grapplinghook.managers;

import com.grapplinghook.GrapplingHookPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final GrapplingHookPlugin plugin;
    private final Map<UUID, Boolean> grapplingHookEnabled;
    private final Map<UUID, Double> playerRanges;
    private final Map<UUID, Double> playerPullSpeeds;
    private final Map<UUID, Double> playerVerticalBoosts;
    private final Map<UUID, Double> playerHookVelocities;
    private final Map<UUID, Boolean> playerSoundEnabled;
    private final Map<UUID, Boolean> playerParticlesEnabled;
    private File dataFile;
    private FileConfiguration dataConfig;

    public PlayerDataManager(GrapplingHookPlugin plugin) {
        this.plugin = plugin;
        this.grapplingHookEnabled = new HashMap<>();
        this.playerRanges = new HashMap<>();
        this.playerPullSpeeds = new HashMap<>();
        this.playerVerticalBoosts = new HashMap<>();
        this.playerHookVelocities = new HashMap<>();
        this.playerSoundEnabled = new HashMap<>();
        this.playerParticlesEnabled = new HashMap<>();
        
        loadData();
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create playerdata.yml!");
                e.printStackTrace();
            }
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        if (dataConfig.contains("players")) {
            for (String uuidStr : dataConfig.getConfigurationSection("players").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                String path = "players." + uuidStr;
                
                boolean enabled = dataConfig.getBoolean(path + ".enabled", false);
                double range = dataConfig.getDouble(path + ".range", getDefaultRange());
                double pullSpeed = dataConfig.getDouble(path + ".pull-speed", getDefaultPullSpeed());
                double verticalBoost = dataConfig.getDouble(path + ".vertical-boost", getDefaultVerticalBoost());
                double hookVelocity = dataConfig.getDouble(path + ".hook-velocity", getDefaultHookVelocity());
                boolean soundEnabled = dataConfig.getBoolean(path + ".sound-enabled", getDefaultSoundEnabled());
                boolean particlesEnabled = dataConfig.getBoolean(path + ".particles-enabled", getDefaultParticlesEnabled());
                
                grapplingHookEnabled.put(uuid, enabled);
                playerRanges.put(uuid, range);
                playerPullSpeeds.put(uuid, pullSpeed);
                playerVerticalBoosts.put(uuid, verticalBoost);
                playerHookVelocities.put(uuid, hookVelocity);
                playerSoundEnabled.put(uuid, soundEnabled);
                playerParticlesEnabled.put(uuid, particlesEnabled);
            }
        }
    }

    public void saveAllData() {
        for (UUID uuid : grapplingHookEnabled.keySet()) {
            String path = "players." + uuid.toString();
            dataConfig.set(path + ".enabled", grapplingHookEnabled.get(uuid));
            dataConfig.set(path + ".range", playerRanges.getOrDefault(uuid, getDefaultRange()));
            dataConfig.set(path + ".pull-speed", playerPullSpeeds.getOrDefault(uuid, getDefaultPullSpeed()));
            dataConfig.set(path + ".vertical-boost", playerVerticalBoosts.getOrDefault(uuid, getDefaultVerticalBoost()));
            dataConfig.set(path + ".hook-velocity", playerHookVelocities.getOrDefault(uuid, getDefaultHookVelocity()));
            dataConfig.set(path + ".sound-enabled", playerSoundEnabled.getOrDefault(uuid, getDefaultSoundEnabled()));
            dataConfig.set(path + ".particles-enabled", playerParticlesEnabled.getOrDefault(uuid, getDefaultParticlesEnabled()));
        }
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save playerdata.yml!");
            e.printStackTrace();
        }
    }

    public boolean isGrapplingHookEnabled(Player player) {
        return grapplingHookEnabled.getOrDefault(player.getUniqueId(), false);
    }

    public boolean toggleGrapplingHook(Player player) {
        UUID uuid = player.getUniqueId();
        boolean currentState = grapplingHookEnabled.getOrDefault(uuid, false);
        boolean newState = !currentState;
        
        grapplingHookEnabled.put(uuid, newState);
        
        if (!playerRanges.containsKey(uuid)) {
            playerRanges.put(uuid, plugin.getConfig().getDouble("default-range", 30.0));
        }
        
        saveAllData();
        return newState;
    }

    public double getRange(Player player) {
        return playerRanges.getOrDefault(player.getUniqueId(), 
            plugin.getConfig().getDouble("default-range", 30.0));
    }

    public void setRange(Player player, double range) {
        playerRanges.put(player.getUniqueId(), range);
        saveAllData();
    }

    public double getPullSpeed(Player player) {
        return playerPullSpeeds.getOrDefault(player.getUniqueId(), getDefaultPullSpeed());
    }

    public void setPullSpeed(Player player, double pullSpeed) {
        playerPullSpeeds.put(player.getUniqueId(), pullSpeed);
        saveAllData();
    }

    public double getVerticalBoost(Player player) {
        return playerVerticalBoosts.getOrDefault(player.getUniqueId(), getDefaultVerticalBoost());
    }

    public void setVerticalBoost(Player player, double verticalBoost) {
        playerVerticalBoosts.put(player.getUniqueId(), verticalBoost);
        saveAllData();
    }

    public double getHookVelocity(Player player) {
        return playerHookVelocities.getOrDefault(player.getUniqueId(), getDefaultHookVelocity());
    }

    public void setHookVelocity(Player player, double hookVelocity) {
        playerHookVelocities.put(player.getUniqueId(), hookVelocity);
        saveAllData();
    }

    public boolean isSoundEnabled(Player player) {
        return playerSoundEnabled.getOrDefault(player.getUniqueId(), getDefaultSoundEnabled());
    }

    public void setSoundEnabled(Player player, boolean enabled) {
        playerSoundEnabled.put(player.getUniqueId(), enabled);
        saveAllData();
    }

    public boolean isParticlesEnabled(Player player) {
        return playerParticlesEnabled.getOrDefault(player.getUniqueId(), getDefaultParticlesEnabled());
    }

    public void setParticlesEnabled(Player player, boolean enabled) {
        playerParticlesEnabled.put(player.getUniqueId(), enabled);
        saveAllData();
    }

    public void resetToDefaults(Player player) {
        UUID uuid = player.getUniqueId();
        playerRanges.put(uuid, getDefaultRange());
        playerPullSpeeds.put(uuid, getDefaultPullSpeed());
        playerVerticalBoosts.put(uuid, getDefaultVerticalBoost());
        playerHookVelocities.put(uuid, getDefaultHookVelocity());
        playerSoundEnabled.put(uuid, getDefaultSoundEnabled());
        playerParticlesEnabled.put(uuid, getDefaultParticlesEnabled());
        saveAllData();
    }

    public double getDefaultRange() {
        return plugin.getConfig().getDouble("default-range", 30.0);
    }

    public double getDefaultPullSpeed() {
        return plugin.getConfig().getDouble("pull-speed", 2.0);
    }

    public double getDefaultVerticalBoost() {
        return plugin.getConfig().getDouble("vertical-boost", 0.3);
    }

    public double getDefaultHookVelocity() {
        return plugin.getConfig().getDouble("hook-velocity-multiplier", 2.0);
    }

    public boolean getDefaultSoundEnabled() {
        return plugin.getConfig().getBoolean("play-sound", true);
    }

    public boolean getDefaultParticlesEnabled() {
        return plugin.getConfig().getBoolean("show-particles", true);
    }

    public double getMinRange() {
        return plugin.getConfig().getDouble("min-range", 10.0);
    }

    public double getMaxRange() {
        return plugin.getConfig().getDouble("max-range", 100.0);
    }

    public double getMinPullSpeed() {
        return plugin.getConfig().getDouble("min-pull-speed", 0.5);
    }

    public double getMaxPullSpeed() {
        return plugin.getConfig().getDouble("max-pull-speed", 5.0);
    }

    public double getMinVerticalBoost() {
        return plugin.getConfig().getDouble("min-vertical-boost", 0.0);
    }

    public double getMaxVerticalBoost() {
        return plugin.getConfig().getDouble("max-vertical-boost", 1.5);
    }

    public double getMinHookVelocity() {
        return plugin.getConfig().getDouble("min-hook-velocity", 1.0);
    }

    public double getMaxHookVelocity() {
        return plugin.getConfig().getDouble("max-hook-velocity", 5.0);
    }
}
