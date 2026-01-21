package org.example;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public class PistonCrusherPlugin extends JavaPlugin {
    private PistonCrusherListener listener;
    private Set<Material> whitelist = new HashSet<>();
    private double multiplier = 1.0;
    private Material crusherBlock = Material.POLISHED_ANDESITE;

    @Override
    public void onEnable() {
        whitelist.add(Material.COBBLESTONE);

        listener = new PistonCrusherListener(this, whitelist, multiplier);
        Bukkit.getPluginManager().registerEvents(listener, this);
        PistonCrusherCommand command = new PistonCrusherCommand(this);
        getCommand("pistoncrusher").setExecutor(command);
        getCommand("pistoncrusher").setTabCompleter(command);
        getLogger().info("PistonCrusher enabled!");
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
        this.crusherBlock = mat;
    }

    @Override
    public void onDisable() {
        getLogger().info("PistonCrusher disabled!");
    }
}
