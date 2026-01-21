package de.railboost;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

public class RailBoostPlugin extends JavaPlugin implements Listener, TabExecutor {

    private MinecartController minecartController;
    private PresetManager presetManager;
    private CommandManager commandManager;

    @Override
    public void onEnable() {
        minecartController = new MinecartController(this);
        presetManager = new PresetManager(this);
        commandManager = new CommandManager(this, minecartController, presetManager);

        getServer().getPluginManager().registerEvents(minecartController, this);

        getCommand("railboost").setExecutor(this);
        getCommand("railboost").setTabCompleter(this);

        presetManager.loadPresets();

        getLogger().info("RailBoost enabled!");
    }

    @Override
    public void onDisable() {
        presetManager.savePresets();
        getLogger().info("RailBoost disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return commandManager.handleCommand(sender, args);
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return commandManager.handleTabComplete(sender, args);
    }

    public MinecartController getMinecartController() {
        return minecartController;
    }

    public PresetManager getPresetManager() {
        return presetManager;
    }
}