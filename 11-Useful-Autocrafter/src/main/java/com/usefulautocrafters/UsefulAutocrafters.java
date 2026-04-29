package com.usefulautocrafters;

import org.bukkit.plugin.java.JavaPlugin;

public class UsefulAutocrafters extends JavaPlugin {

    private boolean featureEnabled = true;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new AutocrafterListener(this), this);
        
        // Register command
        getCommand("usefulcrafter").setExecutor(new UsefulCrafterCommand(this));
        getCommand("usefulcrafter").setTabCompleter(new UsefulCrafterCommand(this));
        
        getLogger().info("Useful-Autocrafters plugin enabled!");
        getLogger().info("Autocrafters will now preserve at least 1 item in each enabled slot");
    }

    @Override
    public void onDisable() {
        getLogger().info("Useful-Autocrafters plugin disabled!");
    }
    
    public boolean isFeatureEnabled() {
        return featureEnabled;
    }
    
    public void setFeatureEnabled(boolean enabled) {
        this.featureEnabled = enabled;
        getLogger().info("Useful-Autocrafters functionality " + (enabled ? "enabled" : "disabled"));
    }
}
