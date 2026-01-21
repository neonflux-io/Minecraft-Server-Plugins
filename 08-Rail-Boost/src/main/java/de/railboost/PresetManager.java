package de.railboost;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class PresetManager {

    private final RailBoostPlugin plugin;
    private final Map<String, MinecartSettings> presets = new ConcurrentHashMap<>();
    private final File presetsFile;

    public PresetManager(RailBoostPlugin plugin) {
        this.plugin = plugin;
        this.presetsFile = new File(plugin.getDataFolder(), "presets.yml");

        createDefaultPresets();
    }

    private void createDefaultPresets() {
        MinecartSettings speed = new MinecartSettings("Speed");
        speed.setSpeed(4);
        speed.setSpeedometer(true);
        presets.put("speed", speed);

        MinecartSettings collector = new MinecartSettings("Collector");
        collector.setAutoPickup(true);
        collector.setPickupRadius(4);
        collector.setSpeed(2);
        presets.put("collector", collector);

        MinecartSettings magnet = new MinecartSettings("Magnet");
        magnet.setMagnet(true);
        magnet.setSpeed(3);
        magnet.setEffects(true);
        presets.put("magnet", magnet);
    }

    public void savePreset(String name, MinecartSettings settings) {
        settings.setName(name);
        presets.put(name.toLowerCase(), settings);
        savePresets();
    }

    public void deletePreset(String name) {
        presets.remove(name.toLowerCase());
        savePresets();
    }

    public MinecartSettings getPreset(String name) {
        return presets.get(name.toLowerCase());
    }

    public Set<String> getPresetNames() {
        return new HashSet<>(presets.keySet());
    }

    public ItemStack createPresetStick(String presetName) {
        MinecartSettings settings = getPreset(presetName);
        if (settings == null) return null;

        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();

        meta.setDisplayName("§6RailBoost Stick §7- §e" + settings.getName());

        List<String> lore = new ArrayList<>();
        lore.add("§7Right-click a minecart to apply preset");
        lore.add("");
        lore.add("§7Settings:");
        lore.add("§8• Speed: §f" + settings.getSpeed() + "/6");
        lore.add("§8• Auto Pickup: " + (settings.isAutoPickup() ? "§aEnabled" : "§cDisabled"));
        lore.add("§8• Pickup Radius: §f" + settings.getPickupRadius());
        lore.add("§8• Speedometer: " + (settings.isSpeedometer() ? "§aEnabled" : "§cDisabled"));
        lore.add("§8• Chunk Loading: " + (settings.isChunkLoad() ? "§aEnabled" : "§cDisabled"));
        lore.add("§8• Magnet: " + (settings.isMagnet() ? "§aEnabled" : "§cDisabled"));
        lore.add("§8• Effects: " + (settings.isEffects() ? "§aEnabled" : "§cDisabled"));
        if (settings.isEffects()) {
            lore.add("§8• Effect Type: §f" + settings.getEffectType());
        }
        if (!settings.getBlacklist().isEmpty()) {
            lore.add("§8• Blacklisted: §f" + settings.getBlacklist().size() + " items");
        }

        meta.setLore(lore);

        NamespacedKey key = new NamespacedKey(plugin, "preset");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, settings.serialize());

        stick.setItemMeta(meta);
        return stick;
    }

    public void givePresetStick(Player player, String presetName) {
        ItemStack stick = createPresetStick(presetName);
        if (stick != null) {
            player.getInventory().addItem(stick);
            player.sendMessage("§aReceived preset stick: §e" + presetName);
        } else {
            player.sendMessage("§cPreset not found: " + presetName);
        }
    }

    public void savePresets() {
        try {
            YamlConfiguration config = new YamlConfiguration();

            for (Map.Entry<String, MinecartSettings> entry : presets.entrySet()) {
                String key = "presets." + entry.getKey();
                MinecartSettings settings = entry.getValue();

                config.set(key + ".name", settings.getName());
                config.set(key + ".speed", settings.getSpeed());
                config.set(key + ".autopickup", settings.isAutoPickup());
                config.set(key + ".pickupRadius", settings.getPickupRadius());
                config.set(key + ".speedometer", settings.isSpeedometer());
                config.set(key + ".chunkload", settings.isChunkLoad());
                config.set(key + ".magnet", settings.isMagnet());
                config.set(key + ".effects", settings.isEffects());
                config.set(key + ".effectType", settings.getEffectType());

                List<String> blacklistNames = new ArrayList<>();
                for (Material material : settings.getBlacklist()) {
                    blacklistNames.add(material.name());
                }
                config.set(key + ".blacklist", blacklistNames);
            }

            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            config.save(presetsFile);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save presets: " + e.getMessage());
        }
    }

    public void loadPresets() {
        if (!presetsFile.exists()) {
            plugin.getLogger().info("No presets file found, using defaults");
            return;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(presetsFile);

            if (!config.contains("presets")) {
                plugin.getLogger().info("No presets found in file");
                return;
            }

            for (String presetKey : config.getConfigurationSection("presets").getKeys(false)) {
                String path = "presets." + presetKey;

                String name = config.getString(path + ".name", presetKey);
                MinecartSettings settings = new MinecartSettings(name);

                settings.setSpeed(config.getInt(path + ".speed", 1));
                settings.setAutoPickup(config.getBoolean(path + ".autopickup", false));
                settings.setPickupRadius(config.getInt(path + ".pickupRadius", 3));
                settings.setSpeedometer(config.getBoolean(path + ".speedometer", false));
                settings.setChunkLoad(config.getBoolean(path + ".chunkload", false));
                settings.setMagnet(config.getBoolean(path + ".magnet", false));
                settings.setEffects(config.getBoolean(path + ".effects", false));
                settings.setEffectType(config.getString(path + ".effectType", "FLAME"));

                Set<Material> blacklist = new HashSet<>();
                List<String> blacklistNames = config.getStringList(path + ".blacklist");
                for (String materialName : blacklistNames) {
                    try {
                        blacklist.add(Material.valueOf(materialName));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid material in blacklist: " + materialName);
                    }
                }
                settings.setBlacklist(blacklist);

                presets.put(presetKey, settings);
            }

            plugin.getLogger().info("Loaded " + presets.size() + " presets");

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load presets: " + e.getMessage());
        }
    }
}