package org.example;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

public class PistonCrusherPlugin extends JavaPlugin {
    private PistonCrusherListener listener;
    private Set<Material> whitelist = new HashSet<>(Arrays.asList(Material.COBBLESTONE));
    private double multiplier = 1.0;
    private Material crusherBlock = Material.WAXED_CHISELED_COPPER;

    public static final List<Material> UNMOVABLE_BLOCKS = Arrays.asList(
            Material.OBSIDIAN,
            Material.CRYING_OBSIDIAN,
            Material.RESPAWN_ANCHOR,
            Material.REINFORCED_DEEPSLATE,
            Material.ANCIENT_DEBRIS,
            Material.ENCHANTING_TABLE,
            Material.ENDER_CHEST,
            Material.NETHERITE_BLOCK,
            Material.COMMAND_BLOCK,
            Material.BARRIER,
            Material.BEDROCK,
            Material.JIGSAW,
            Material.STRUCTURE_BLOCK,
            Material.STRUCTURE_VOID
    );

    public static final List<Material> MOVABLE_BLOCKS = Arrays.stream(Material.values())
            .filter(mat -> mat.isBlock() && mat != Material.AIR && !UNMOVABLE_BLOCKS.contains(mat))
            .collect(Collectors.toList());

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        File whitelistFile = new File(getDataFolder(), "whitelist.yml");
        if (!whitelistFile.exists()) {
            try {
                whitelistFile.createNewFile();
                YamlConfiguration config = new YamlConfiguration();
                config.set("whitelist", Arrays.asList(Material.COBBLESTONE.name()));
                config.save(whitelistFile);
            } catch (IOException e) {
                getLogger().warning("Could not create whitelist.yml: " + e.getMessage());
            }
        }
        File crusherFile = new File(getDataFolder(), "crusherblock.yml");
        if (!crusherFile.exists()) {
            try {
                crusherFile.createNewFile();
                YamlConfiguration config = new YamlConfiguration();
                config.set("crusherblock", Material.WAXED_CHISELED_COPPER.name());
                config.save(crusherFile);
            } catch (IOException e) {
                getLogger().warning("Could not create crusherblock.yml: " + e.getMessage());
            }
        }
        loadWhitelist();
        loadCrusherBlock();
        if (whitelist.isEmpty()) {
            whitelist.add(Material.COBBLESTONE);
        }
        listener = new PistonCrusherListener(this, whitelist, multiplier);
        Bukkit.getPluginManager().registerEvents(listener, this);
        PistonCrusherCommand command = new PistonCrusherCommand(this);
        getCommand("pistoncrusher").setExecutor(command);
        getCommand("pistoncrusher").setTabCompleter(command);
        getLogger().info("PistonCrusher enabled!");
    }

    @Override
    public void onDisable() {
        saveWhitelist();
        saveCrusherBlock();
    }

    public void saveWhitelist() {
        File file = new File(getDataFolder(), "whitelist.yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("whitelist", whitelist.stream().map(Material::name).toArray(String[]::new));
        try {
            config.save(file);
        } catch (IOException e) {
            getLogger().warning("Could not save whitelist: " + e.getMessage());
        }
    }

    public void loadWhitelist() {
        File file = new File(getDataFolder(), "whitelist.yml");
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        whitelist.clear();
        for (String matName : config.getStringList("whitelist")) {
            try {
                whitelist.add(Material.valueOf(matName));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void saveCrusherBlock() {
        File file = new File(getDataFolder(), "crusherblock.yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("crusherblock", crusherBlock.name());
        try {
            config.save(file);
        } catch (IOException e) {
            getLogger().warning("Could not save crusher block: " + e.getMessage());
        }
    }

    public void loadCrusherBlock() {
        File file = new File(getDataFolder(), "crusherblock.yml");
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String matName = config.getString("crusherblock");
        if (matName != null) {
            try {
                Material mat = Material.valueOf(matName);
                if (MOVABLE_BLOCKS.contains(mat)) {
                    crusherBlock = mat;
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public PistonCrusherListener getListener() {
        return listener;
    }

    public Set<Material> getWhitelist() {
        return whitelist;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
        listener.setMultiplier(multiplier);
    }

    public Material getCrusherBlock() {
        return crusherBlock;
    }

    public void setCrusherBlock(Material mat) {
        if (!MOVABLE_BLOCKS.contains(mat)) {
            getLogger().warning("Crusher block must be a movable block!");
            return;
        }
        this.crusherBlock = mat;
    }

    public List<Material> getUnmovableBlocks() {
        return UNMOVABLE_BLOCKS;
    }

    public List<Material> getMovableBlocks() {
        return MOVABLE_BLOCKS;
    }

    public boolean addToWhitelist(Material mat) {
        if (!MOVABLE_BLOCKS.contains(mat)) {
            return false;
        }
        boolean added = whitelist.add(mat);
        if (added) saveWhitelist();
        return added;
    }

    public boolean removeFromWhitelist(Material mat) {
        boolean removed = whitelist.remove(mat);
        if (removed) saveWhitelist();
        return removed;
    }
}
