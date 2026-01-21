package org.example;

import org.bukkit.plugin.java.JavaPlugin;
import org.example.commands.EnchantListCommand;
import org.example.commands.SuperEnchantCommand;
import org.example.util.EnchantmentManager;

public class SuperEnchantments extends JavaPlugin {

    private EnchantmentManager enchantmentManager;

    @Override
    public void onEnable() {
        this.enchantmentManager = new EnchantmentManager();

        getCommand("superenchant").setExecutor(new SuperEnchantCommand(this));
        getCommand("enchantlist").setExecutor(new EnchantListCommand(this));

        getLogger().info("SuperEnchantments has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("SuperEnchantments has been disabled!");
    }

    public EnchantmentManager getEnchantmentManager() {
        return enchantmentManager;
    }
}
