package org.example;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.example.commands.EnchantListCommand;
import org.example.commands.SuperEnchantCommand;
import org.example.listeners.CustomEnchantmentListener;
import org.example.util.CustomEnchantmentManager;
import org.example.util.EnchantmentManager;

public class SuperEnchantments extends JavaPlugin {

    private EnchantmentManager enchantmentManager;
    private CustomEnchantmentManager customEnchantmentManager;

    private boolean cooldownDisplayEnabled;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.cooldownDisplayEnabled = getConfig().getBoolean("cooldown-display-enabled", true);

        this.enchantmentManager = new EnchantmentManager();
        this.customEnchantmentManager = new CustomEnchantmentManager(this);

        getCommand("superenchant").setExecutor(new SuperEnchantCommand(this));
        getCommand("enchantlist").setExecutor(new EnchantListCommand(this));

        getServer().getPluginManager().registerEvents(new CustomEnchantmentListener(this), this);

        getLogger().info("SuperEnchantments has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("SuperEnchantments has been disabled!");
    }

    public EnchantmentManager getEnchantmentManager() {
        return enchantmentManager;
    }

    public CustomEnchantmentManager getCustomEnchantmentManager() {
        return customEnchantmentManager;
    }

    public boolean isCooldownDisplayEnabled() {
        return cooldownDisplayEnabled;
    }

    public void setCooldownDisplayEnabled(boolean enabled) {
        this.cooldownDisplayEnabled = enabled;
        getConfig().set("cooldown-display-enabled", enabled);
        saveConfig();
    }

    public NamespacedKey getCustomEnchantmentKey(String enchantmentName) {
        return new NamespacedKey(this, "custom_enchantment_" + enchantmentName);
    }
}