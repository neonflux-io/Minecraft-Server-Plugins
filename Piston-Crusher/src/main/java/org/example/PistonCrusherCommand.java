package org.example;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PistonCrusherCommand implements CommandExecutor, TabCompleter {
    private final PistonCrusherPlugin plugin;

    public PistonCrusherCommand(PistonCrusherPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§eUsage: /pistoncrusher <whitelist|multiplier|crusherblock> ...");
            return true;
        }
        if (args[0].equalsIgnoreCase("whitelist")) {
            if (args.length < 2) {
                sender.sendMessage("§eUsage: /pistoncrusher whitelist <add|remove|list> [Material]");
                return true;
            }
            if (args[1].equalsIgnoreCase("add") && args.length == 3) {
                try {
                    Material mat = Material.valueOf(args[2].toUpperCase());
                    if (plugin.getWhitelist().add(mat)) {
                        sender.sendMessage("§a" + mat.name().toLowerCase() + " was added to the whitelist.");
                    } else {
                        sender.sendMessage("§c" + mat.name().toLowerCase() + " is already on the whitelist.");
                    }
                } catch (IllegalArgumentException e) {
                    sender.sendMessage("§cUnknown material: " + args[2]);
                }
                return true;
            }
            if (args[1].equalsIgnoreCase("remove") && args.length == 3) {
                try {
                    Material mat = Material.valueOf(args[2].toUpperCase());
                    if (plugin.getWhitelist().remove(mat)) {
                        sender.sendMessage("§a" + mat.name().toLowerCase() + " was removed from the whitelist.");
                    } else {
                        sender.sendMessage("§c" + mat.name().toLowerCase() + " is not on the whitelist.");
                    }
                } catch (IllegalArgumentException e) {
                    sender.sendMessage("§cUnknown material: " + args[2]);
                }
                return true;
            }
            if (args[1].equalsIgnoreCase("list")) {
                String whitelistString = plugin.getWhitelist().stream()
                    .map(mat -> mat.name().toLowerCase())
                    .collect(java.util.stream.Collectors.joining(", "));
                sender.sendMessage("§eWhitelist: " + whitelistString);
                return true;
            }
        }
        if (args[0].equalsIgnoreCase("multiplier")) {
            if (args.length == 1) {
                sender.sendMessage("§eCurrent multiplier: " + plugin.getMultiplier());
                return true;
            }
            if (args.length == 2) {
                try {
                    double multi = Double.parseDouble(args[1]);
                    if (multi < 1) multi = 1;
                    plugin.setMultiplier(multi);
                    sender.sendMessage("§aMultiplier set to " + multi + ".");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid value: " + args[1]);
                }
                return true;
            }
        }
        if (args[0].equalsIgnoreCase("crusherblock")) {
            if (args.length == 1) {
                sender.sendMessage("§eCurrent crusher block: " + plugin.getCrusherBlock());
                sender.sendMessage("§eUsage: /pistoncrusher crusherblock <Material>");
                return true;
            }
            if (args.length == 2) {
                try {
                    Material mat = Material.valueOf(args[1].toUpperCase());
                    plugin.setCrusherBlock(mat);
                    sender.sendMessage("§aCrusher block set to " + mat + ".");
                } catch (IllegalArgumentException e) {
                    sender.sendMessage("§cUnknown block: " + args[1]);
                }
                return true;
            }
        }
        sender.sendMessage("§cUnknown command. /pistoncrusher <whitelist|multiplier|crusherblock>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(Arrays.asList("whitelist", "multiplier", "crusherblock"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("whitelist")) {
            return filterStartsWith(Arrays.asList("add", "remove", "list"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("whitelist") && (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove"))) {
            return getMaterialSuggestions(args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("crusherblock")) {
            return getMaterialSuggestions(args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> getMaterialSuggestions(String input) {
        List<String> suggestions = new ArrayList<>();
        String lowercaseInput = input.toLowerCase();

        List<String> startsWith = new ArrayList<>();
        List<String> contains = new ArrayList<>();

        for (Material mat : Material.values()) {
            String name = mat.name().toLowerCase();

            if (name.startsWith(lowercaseInput)) {
                startsWith.add(name);
            } else if (name.contains(lowercaseInput)) {
                contains.add(name);
            }
        }

        suggestions.addAll(startsWith);
        suggestions.addAll(contains);

        return suggestions.isEmpty() ? Collections.emptyList() : suggestions;
    }

    private List<String> filterStartsWith(List<String> list, String prefix) {
        String lowercasePrefix = prefix.toLowerCase();
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(lowercasePrefix))
                .collect(java.util.stream.Collectors.toList());
    }
}
