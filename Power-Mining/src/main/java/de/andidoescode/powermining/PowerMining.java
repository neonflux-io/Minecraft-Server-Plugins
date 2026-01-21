package de.andidoescode.powermining;

import de.andidoescode.powermining.commands.PowerMiningCommand;
import de.andidoescode.powermining.listeners.MountedMiningListener;
import de.andidoescode.powermining.managers.*;
import org.bukkit.plugin.java.JavaPlugin;

public class PowerMining extends JavaPlugin {

    private static PowerMining instance;
    
    private MagnetHopperManager magnetHopperManager;
    private OreScannerBellManager oreScannerBellManager;
    private MinersHelmetManager minersHelmetManager;
    private EscapeRopeManager escapeRopeManager;
    private CaveCompassManager caveCompassManager;
    private AutoSmelterPickaxeManager autoSmelterPickaxeManager;
    private MinersGogglesManager minersGogglesManager;

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        magnetHopperManager = new MagnetHopperManager(this);
        oreScannerBellManager = new OreScannerBellManager(this);
        minersHelmetManager = new MinersHelmetManager(this);
        escapeRopeManager = new EscapeRopeManager(this);
        caveCompassManager = new CaveCompassManager(this);
        autoSmelterPickaxeManager = new AutoSmelterPickaxeManager(this);
        minersGogglesManager = new MinersGogglesManager(this);
        
        registerListeners();
        registerCommands();
        
        getLogger().info("Power Mining has been enabled!");
    }

    @Override
    public void onDisable() {
        if (magnetHopperManager != null) {
            magnetHopperManager.shutdown();
        }
        if (oreScannerBellManager != null) {
            oreScannerBellManager.shutdown();
        }
        if (minersHelmetManager != null) {
            minersHelmetManager.shutdown();
        }
        if (escapeRopeManager != null) {
            escapeRopeManager.shutdown();
        }
        if (caveCompassManager != null) {
            caveCompassManager.shutdown();
        }
        if (autoSmelterPickaxeManager != null) {
            autoSmelterPickaxeManager.shutdown();
        }
        if (minersGogglesManager != null) {
            minersGogglesManager.shutdown();
        }
        
        getLogger().info("Power Mining has been disabled!");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new MountedMiningListener(this), this);
        getServer().getPluginManager().registerEvents(magnetHopperManager, this);
        getServer().getPluginManager().registerEvents(oreScannerBellManager, this);
        getServer().getPluginManager().registerEvents(minersHelmetManager, this);
        getServer().getPluginManager().registerEvents(escapeRopeManager, this);
        getServer().getPluginManager().registerEvents(caveCompassManager, this);
        getServer().getPluginManager().registerEvents(autoSmelterPickaxeManager, this);
        getServer().getPluginManager().registerEvents(minersGogglesManager, this);
    }

    private void registerCommands() {
        var powerMiningCommand = getCommand("powermining");
        if (powerMiningCommand != null) {
            var commandHandler = new PowerMiningCommand(this);
            powerMiningCommand.setExecutor(commandHandler);
            powerMiningCommand.setTabCompleter(commandHandler);
        }
    }

    public static PowerMining getInstance() {
        return instance;
    }

    public MagnetHopperManager getMagnetHopperManager() {
        return magnetHopperManager;
    }

    public OreScannerBellManager getOreScannerBellManager() {
        return oreScannerBellManager;
    }

    public MinersHelmetManager getMinersHelmetManager() {
        return minersHelmetManager;
    }

    public EscapeRopeManager getEscapeRopeManager() {
        return escapeRopeManager;
    }

    public CaveCompassManager getCaveCompassManager() {
        return caveCompassManager;
    }

    public AutoSmelterPickaxeManager getAutoSmelterPickaxeManager() {
        return autoSmelterPickaxeManager;
    }

    public MinersGogglesManager getMinersGogglesManager() {
        return minersGogglesManager;
    }
}
