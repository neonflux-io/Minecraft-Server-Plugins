package com.usefulautocrafters;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class UsefulCrafterCommand implements CommandExecutor, TabCompleter {
    
    private final UsefulAutocrafters plugin;
    
    public UsefulCrafterCommand(UsefulAutocrafters plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                           @NotNull String label, @NotNull String[] args) {
        
        if (!sender.hasPermission("usefulautocrafters.toggle")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        if (args.length == 0) {
            // Show current status
            boolean enabled = plugin.isFeatureEnabled();
            sender.sendMessage(ChatColor.YELLOW + "Useful-Autocrafters is currently: " + 
                             (enabled ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
            sender.sendMessage(ChatColor.GRAY + "Use /usefulcrafter on|off to toggle");
            return true;
        }
        
        String action = args[0].toLowerCase();
        
        switch (action) {
            case "on":
            case "enable":
                if (plugin.isFeatureEnabled()) {
                    sender.sendMessage(ChatColor.YELLOW + "Useful-Autocrafters is already enabled!");
                } else {
                    plugin.setFeatureEnabled(true);
                    sender.sendMessage(ChatColor.GREEN + "Useful-Autocrafters enabled! " +
                                     "Autocrafters will now preserve at least 1 item in each slot.");
                }
                return true;
                
            case "off":
            case "disable":
                if (!plugin.isFeatureEnabled()) {
                    sender.sendMessage(ChatColor.YELLOW + "Useful-Autocrafters is already disabled!");
                } else {
                    plugin.setFeatureEnabled(false);
                    sender.sendMessage(ChatColor.RED + "Useful-Autocrafters disabled! " +
                                     "Autocrafters will now work normally.");
                }
                return true;
                
            default:
                sender.sendMessage(ChatColor.RED + "Usage: /usefulcrafter <on|off>");
                return true;
        }
    }
    
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, 
                                     @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            if (sender.hasPermission("usefulautocrafters.toggle")) {
                completions.add("on");
                completions.add("off");
            }
        }
        
        return completions;
    }
}
