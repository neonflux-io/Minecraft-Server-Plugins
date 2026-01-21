package com.grapplinghook.commands;

import com.grapplinghook.GrapplingHookPlugin;
import com.grapplinghook.managers.PlayerDataManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GrapplingHookCommand implements CommandExecutor, TabCompleter {

    private final GrapplingHookPlugin plugin;
    private final PlayerDataManager playerDataManager;
    
    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "toggle", "range", "speed", "boost", "velocity", "sound", "particles", "reset", "info"
    );

    public GrapplingHookCommand(GrapplingHookPlugin plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("grapplinghook.use")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            return toggleGrapplingHook(player);
        }

        switch (args[0].toLowerCase()) {
            case "toggle":
                return toggleGrapplingHook(player);
                
            case "range":
                return handleSetting(player, args, "range");
                
            case "speed":
                return handleSetting(player, args, "speed");
                
            case "boost":
                return handleSetting(player, args, "boost");
                
            case "velocity":
                return handleSetting(player, args, "velocity");
                
            case "sound":
                return toggleSound(player);
                
            case "particles":
                return toggleParticles(player);
                
            case "reset":
                return resetSettings(player);
                
            case "info":
                return showInfo(player);
                
            default:
                sendUsage(player);
                return true;
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "range":
                    return Arrays.asList("10", "20", "30", "50", "100");
                case "speed":
                    return Arrays.asList("1.0", "1.5", "2.0", "2.5", "3.0");
                case "boost":
                    return Arrays.asList("0.0", "0.3", "0.5", "0.8", "1.0");
                case "velocity":
                    return Arrays.asList("1.0", "1.5", "2.0", "3.0", "4.0");
            }
        }
        
        return new ArrayList<>();
    }
    
    private void sendUsage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Grappling Hook Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/grapplinghook " + ChatColor.WHITE + "- Toggle grappling hook on/off");
        player.sendMessage(ChatColor.YELLOW + "/grapplinghook range <value> " + ChatColor.WHITE + "- Set hook range");
        player.sendMessage(ChatColor.YELLOW + "/grapplinghook speed <value> " + ChatColor.WHITE + "- Set pull speed");
        player.sendMessage(ChatColor.YELLOW + "/grapplinghook boost <value> " + ChatColor.WHITE + "- Set vertical boost");
        player.sendMessage(ChatColor.YELLOW + "/grapplinghook velocity <value> " + ChatColor.WHITE + "- Set hook throw speed");
        player.sendMessage(ChatColor.YELLOW + "/grapplinghook sound " + ChatColor.WHITE + "- Toggle sound effects");
        player.sendMessage(ChatColor.YELLOW + "/grapplinghook particles " + ChatColor.WHITE + "- Toggle particle effects");
        player.sendMessage(ChatColor.YELLOW + "/grapplinghook reset " + ChatColor.WHITE + "- Reset all settings to defaults");
        player.sendMessage(ChatColor.YELLOW + "/grapplinghook info " + ChatColor.WHITE + "- Show current settings");
    }

    private boolean toggleGrapplingHook(Player player) {
        boolean enabled = playerDataManager.toggleGrapplingHook(player);
        
        if (enabled) {
            player.sendMessage(ChatColor.GREEN + "✓ Grappling hook enabled!");
        } else {
            player.sendMessage(ChatColor.YELLOW + "✗ Grappling hook disabled!");
        }
        
        return true;
    }
    
    private boolean handleSetting(Player player, String[] args, String setting) {
        if (!player.hasPermission("grapplinghook.settings")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to change grappling hook settings!");
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /grapplinghook " + setting + " <value>");
            return true;
        }
        
        try {
            double value = Double.parseDouble(args[1]);
            
            switch (setting) {
                case "range":
                    return setRange(player, value);
                case "speed":
                    return setPullSpeed(player, value);
                case "boost":
                    return setVerticalBoost(player, value);
                case "velocity":
                    return setHookVelocity(player, value);
                default:
                    return false;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid value! Please enter a number.");
            return true;
        }
    }
    
    private boolean setRange(Player player, double range) {
        double min = playerDataManager.getMinRange();
        double max = playerDataManager.getMaxRange();
        
        if (range < min || range > max) {
            player.sendMessage(ChatColor.RED + "Range must be between " + min + " and " + max + "!");
            return true;
        }
        
        playerDataManager.setRange(player, range);
        player.sendMessage(ChatColor.GREEN + "Hook range set to " + range + " blocks!");
        return true;
    }
    
    private boolean setPullSpeed(Player player, double speed) {
        double min = playerDataManager.getMinPullSpeed();
        double max = playerDataManager.getMaxPullSpeed();
        
        if (speed < min || speed > max) {
            player.sendMessage(ChatColor.RED + "Pull speed must be between " + min + " and " + max + "!");
            return true;
        }
        
        playerDataManager.setPullSpeed(player, speed);
        player.sendMessage(ChatColor.GREEN + "Pull speed set to " + speed + "!");
        return true;
    }
    
    private boolean setVerticalBoost(Player player, double boost) {
        double min = playerDataManager.getMinVerticalBoost();
        double max = playerDataManager.getMaxVerticalBoost();
        
        if (boost < min || boost > max) {
            player.sendMessage(ChatColor.RED + "Vertical boost must be between " + min + " and " + max + "!");
            return true;
        }
        
        playerDataManager.setVerticalBoost(player, boost);
        player.sendMessage(ChatColor.GREEN + "Vertical boost set to " + boost + "!");
        return true;
    }
    
    private boolean setHookVelocity(Player player, double velocity) {
        double min = playerDataManager.getMinHookVelocity();
        double max = playerDataManager.getMaxHookVelocity();
        
        if (velocity < min || velocity > max) {
            player.sendMessage(ChatColor.RED + "Hook velocity must be between " + min + " and " + max + "!");
            return true;
        }
        
        playerDataManager.setHookVelocity(player, velocity);
        player.sendMessage(ChatColor.GREEN + "Hook throw velocity set to " + velocity + "!");
        return true;
    }
    
    private boolean toggleSound(Player player) {
        if (!player.hasPermission("grapplinghook.settings")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to change grappling hook settings!");
            return true;
        }
        
        boolean current = playerDataManager.isSoundEnabled(player);
        playerDataManager.setSoundEnabled(player, !current);
        
        if (!current) {
            player.sendMessage(ChatColor.GREEN + "✓ Sound effects enabled!");
        } else {
            player.sendMessage(ChatColor.YELLOW + "✗ Sound effects disabled!");
        }
        return true;
    }
    
    private boolean toggleParticles(Player player) {
        if (!player.hasPermission("grapplinghook.settings")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to change grappling hook settings!");
            return true;
        }
        
        boolean current = playerDataManager.isParticlesEnabled(player);
        playerDataManager.setParticlesEnabled(player, !current);
        
        if (!current) {
            player.sendMessage(ChatColor.GREEN + "✓ Particle effects enabled!");
        } else {
            player.sendMessage(ChatColor.YELLOW + "✗ Particle effects disabled!");
        }
        return true;
    }
    
    private boolean resetSettings(Player player) {
        playerDataManager.resetToDefaults(player);
        player.sendMessage(ChatColor.GREEN + "✓ All grappling hook settings have been reset to defaults!");
        return true;
    }

    private boolean showInfo(Player player) {
        boolean enabled = playerDataManager.isGrapplingHookEnabled(player);
        double range = playerDataManager.getRange(player);
        double pullSpeed = playerDataManager.getPullSpeed(player);
        double verticalBoost = playerDataManager.getVerticalBoost(player);
        double hookVelocity = playerDataManager.getHookVelocity(player);
        boolean soundEnabled = playerDataManager.isSoundEnabled(player);
        boolean particlesEnabled = playerDataManager.isParticlesEnabled(player);
        
        player.sendMessage(ChatColor.GOLD + "=== Grappling Hook Settings ===");
        player.sendMessage(ChatColor.YELLOW + "Status: " + (enabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
        player.sendMessage(ChatColor.YELLOW + "Range: " + ChatColor.WHITE + range + " blocks");
        player.sendMessage(ChatColor.YELLOW + "Pull Speed: " + ChatColor.WHITE + pullSpeed);
        player.sendMessage(ChatColor.YELLOW + "Vertical Boost: " + ChatColor.WHITE + verticalBoost);
        player.sendMessage(ChatColor.YELLOW + "Hook Velocity: " + ChatColor.WHITE + hookVelocity);
        player.sendMessage(ChatColor.YELLOW + "Sound: " + (soundEnabled ? ChatColor.GREEN + "On" : ChatColor.RED + "Off"));
        player.sendMessage(ChatColor.YELLOW + "Particles: " + (particlesEnabled ? ChatColor.GREEN + "On" : ChatColor.RED + "Off"));
        
        return true;
    }
}
