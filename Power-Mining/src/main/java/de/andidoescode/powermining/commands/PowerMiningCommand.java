package de.andidoescode.powermining.commands;

import de.andidoescode.powermining.PowerMining;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PowerMiningCommand implements CommandExecutor, TabCompleter {

    private final PowerMining plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public PowerMiningCommand(PowerMining plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        return switch (subCommand) {
            case "magnethopper", "mh" -> handleMagnetHopper(sender, subArgs);
            case "orebell", "ob" -> handleOreBell(sender, subArgs);
            case "helmet", "minershelmet" -> handleMinersHelmet(sender, subArgs);
            case "escaperope", "rope", "er" -> handleEscapeRope(sender, subArgs);
            case "cavecompass", "compass", "cc" -> handleCaveCompass(sender, subArgs);
            case "smelterpick", "smelter", "sp" -> handleSmelterPickaxe(sender, subArgs);
            case "goggles", "minersgoggles", "mg" -> handleMinersGoggles(sender, subArgs);
            case "drill" -> handleDrill(sender);
            case "help" -> {
                sendHelp(sender);
                yield true;
            }
            default -> {
                sender.sendMessage(miniMessage.deserialize("<red>Unknown subcommand. Use <yellow>/pm help</yellow> for help.</red>"));
                yield true;
            }
        };
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("═══ Power Mining Commands ═══")
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.BOLD, true));
        sender.sendMessage(Component.empty());
        
        sender.sendMessage(Component.text("/pm magnethopper ")
            .color(NamedTextColor.YELLOW)
            .append(Component.text("[player] [radius]")
                .color(NamedTextColor.GRAY))
            .append(Component.text(" - Give a Magnet Hopper")
                .color(NamedTextColor.WHITE)));
        
        sender.sendMessage(Component.text("/pm orebell ")
            .color(NamedTextColor.YELLOW)
            .append(Component.text("[player] [radius] [--filter ORE] [--duration TICKS]")
                .color(NamedTextColor.GRAY))
            .append(Component.text(" - Give an Ore Scanner Bell")
                .color(NamedTextColor.WHITE)));
        
        sender.sendMessage(Component.text("/pm helmet ")
            .color(NamedTextColor.YELLOW)
            .append(Component.text("[player]")
                .color(NamedTextColor.GRAY))
            .append(Component.text(" - Give a Miner's Helmet")
                .color(NamedTextColor.WHITE)));
        
        sender.sendMessage(Component.text("/pm escaperope ")
            .color(NamedTextColor.YELLOW)
            .append(Component.text("[player]")
                .color(NamedTextColor.GRAY))
            .append(Component.text(" - Give an Escape Rope")
                .color(NamedTextColor.WHITE)));
        
        sender.sendMessage(Component.text("/pm cavecompass ")
            .color(NamedTextColor.YELLOW)
            .append(Component.text("[player]")
                .color(NamedTextColor.GRAY))
            .append(Component.text(" - Give a Cave Compass")
                .color(NamedTextColor.WHITE)));
        
        sender.sendMessage(Component.text("/pm smelterpick ")
            .color(NamedTextColor.YELLOW)
            .append(Component.text("[player]")
                .color(NamedTextColor.GRAY))
            .append(Component.text(" - Give a Smelter's Pickaxe")
                .color(NamedTextColor.WHITE)));
        
        sender.sendMessage(Component.text("/pm goggles ")
            .color(NamedTextColor.YELLOW)
            .append(Component.text("[player] [radius] [--filter ORE]")
                .color(NamedTextColor.GRAY))
            .append(Component.text(" - Give Miner's Goggles")
                .color(NamedTextColor.WHITE)));
        
        sender.sendMessage(Component.text("/pm drill")
            .color(NamedTextColor.YELLOW)
            .append(Component.text(" - Info about mounted mining")
                .color(NamedTextColor.WHITE)));
        
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Aliases: ")
            .color(NamedTextColor.GRAY)
            .append(Component.text("/powermining, /pm")
                .color(NamedTextColor.AQUA)));
        sender.sendMessage(Component.empty());
    }

    private boolean handleMagnetHopper(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermining.give.magnethopper")) {
            String noPermMsg = plugin.getConfig().getString("messages.no-permission", "<red>You don't have permission to do that!</red>");
            sender.sendMessage(miniMessage.deserialize(noPermMsg));
            return true;
        }

        Player target;
        int radius = plugin.getConfig().getInt("magnet-hopper.default-radius", 8);

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(miniMessage.deserialize("<red>Console must specify a player!</red>"));
                return true;
            }
            target = player;
        } else {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(miniMessage.deserialize("<red>Player not found: " + args[0] + "</red>"));
                return true;
            }
            
            if (args.length >= 2) {
                try {
                    radius = Integer.parseInt(args[1]);
                    int maxRadius = plugin.getConfig().getInt("magnet-hopper.max-radius", 32);
                    if (radius < 1 || radius > maxRadius) {
                        sender.sendMessage(miniMessage.deserialize("<red>Radius must be between 1 and " + maxRadius + "!</red>"));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(miniMessage.deserialize("<red>Invalid radius: " + args[1] + "</red>"));
                    return true;
                }
            }
        }

        var magnetHopper = plugin.getMagnetHopperManager().createMagnetHopper(radius);
        target.getInventory().addItem(magnetHopper);
        
        String givenMsg = plugin.getConfig().getString("messages.magnet-hopper-given", 
            "<green>You received a <gold>Magnet Hopper</gold> with radius <yellow>{radius}</yellow>!</green>");
        target.sendMessage(miniMessage.deserialize(givenMsg.replace("{radius}", String.valueOf(radius))));
        
        if (sender != target) {
            sender.sendMessage(miniMessage.deserialize("<green>Gave Magnet Hopper to " + target.getName() + "!</green>"));
        }

        return true;
    }

    private boolean handleOreBell(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermining.give.orescannerbell")) {
            String noPermMsg = plugin.getConfig().getString("messages.no-permission", "<red>You don't have permission to do that!</red>");
            sender.sendMessage(miniMessage.deserialize(noPermMsg));
            return true;
        }

        Player target;
        int radius = plugin.getConfig().getInt("ore-scanner-bell.default-radius", 16);
        String filter = null;
        int duration = plugin.getConfig().getInt("ore-scanner-bell.default-duration", 60);

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(miniMessage.deserialize("<red>Console must specify a player!</red>"));
                return true;
            }
            target = player;
        } else {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(miniMessage.deserialize("<red>Player not found: " + args[0] + "</red>"));
                return true;
            }
            
            if (args.length >= 2) {
                try {
                    radius = Integer.parseInt(args[1]);
                    int maxRadius = plugin.getConfig().getInt("ore-scanner-bell.max-radius", 64);
                    if (radius < 1 || radius > maxRadius) {
                        sender.sendMessage(miniMessage.deserialize("<red>Radius must be between 1 and " + maxRadius + "!</red>"));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(miniMessage.deserialize("<red>Invalid radius: " + args[1] + "</red>"));
                    return true;
                }
            }
            
            for (int i = 2; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("--filter") && i + 1 < args.length) {
                    filter = args[i + 1].toUpperCase();
                    i++;
                } else if (args[i].equalsIgnoreCase("--duration") && i + 1 < args.length) {
                    try {
                        duration = Integer.parseInt(args[i + 1]);
                        if (duration < 20) duration = 20;
                        if (duration > 600) duration = 600;
                    } catch (NumberFormatException e) {
                        sender.sendMessage(miniMessage.deserialize("<red>Invalid duration: " + args[i + 1] + "</red>"));
                        return true;
                    }
                    i++;
                }
            }
        }

        var oreScannerBell = plugin.getOreScannerBellManager().createOreScannerBell(radius, filter, duration);
        target.getInventory().addItem(oreScannerBell);
        
        StringBuilder message = new StringBuilder("<green>You received an <gold>Ore Scanner Bell</gold>");
        message.append(" with radius <yellow>").append(radius).append("</yellow>");
        if (filter != null) {
            message.append(", filter <yellow>").append(filter).append("</yellow>");
        }
        message.append(", duration <yellow>").append(duration / 20.0).append("s</yellow>!</green>");
        
        target.sendMessage(miniMessage.deserialize(message.toString()));
        
        if (sender != target) {
            sender.sendMessage(miniMessage.deserialize("<green>Gave Ore Scanner Bell to " + target.getName() + "!</green>"));
        }

        return true;
    }

    private boolean handleMinersHelmet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermining.give.minershelmet")) {
            String noPermMsg = plugin.getConfig().getString("messages.no-permission", "<red>You don't have permission to do that!</red>");
            sender.sendMessage(miniMessage.deserialize(noPermMsg));
            return true;
        }

        Player target;

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(miniMessage.deserialize("<red>Console must specify a player!</red>"));
                return true;
            }
            target = player;
        } else {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(miniMessage.deserialize("<red>Player not found: " + args[0] + "</red>"));
                return true;
            }
        }

        var minersHelmet = plugin.getMinersHelmetManager().createMinersHelmet();
        target.getInventory().addItem(minersHelmet);
        
        String givenMsg = plugin.getConfig().getString("messages.miners-helmet-given", 
            "<green>You received a <gold>Miner's Helmet</gold>! Wear it for night vision.</green>");
        target.sendMessage(miniMessage.deserialize(givenMsg));
        
        if (sender != target) {
            sender.sendMessage(miniMessage.deserialize("<green>Gave Miner's Helmet to " + target.getName() + "!</green>"));
        }

        return true;
    }

    private boolean handleEscapeRope(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermining.give.escaperope")) {
            String noPermMsg = plugin.getConfig().getString("messages.no-permission", "<red>You don't have permission to do that!</red>");
            sender.sendMessage(miniMessage.deserialize(noPermMsg));
            return true;
        }

        Player target;

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(miniMessage.deserialize("<red>Console must specify a player!</red>"));
                return true;
            }
            target = player;
        } else {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(miniMessage.deserialize("<red>Player not found: " + args[0] + "</red>"));
                return true;
            }
        }

        var escapeRope = plugin.getEscapeRopeManager().createEscapeRope();
        target.getInventory().addItem(escapeRope);
        
        String givenMsg = plugin.getConfig().getString("messages.escape-rope-given", 
            "<green>You received an <light_purple>Escape Rope</light_purple>! Right-click a block to set return point.</green>");
        target.sendMessage(miniMessage.deserialize(givenMsg));
        
        if (sender != target) {
            sender.sendMessage(miniMessage.deserialize("<green>Gave Escape Rope to " + target.getName() + "!</green>"));
        }

        return true;
    }

    private boolean handleCaveCompass(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermining.give.cavecompass")) {
            String noPermMsg = plugin.getConfig().getString("messages.no-permission", "<red>You don't have permission to do that!</red>");
            sender.sendMessage(miniMessage.deserialize(noPermMsg));
            return true;
        }

        Player target;

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(miniMessage.deserialize("<red>Console must specify a player!</red>"));
                return true;
            }
            target = player;
        } else {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(miniMessage.deserialize("<red>Player not found: " + args[0] + "</red>"));
                return true;
            }
        }

        var caveCompass = plugin.getCaveCompassManager().createCaveCompass();
        target.getInventory().addItem(caveCompass);
        
        String givenMsg = plugin.getConfig().getString("messages.cave-compass-given", 
            "<green>You received a <light_purple>Cave Compass</light_purple>! Shift+Right-click to cycle targets.</green>");
        target.sendMessage(miniMessage.deserialize(givenMsg));
        
        if (sender != target) {
            sender.sendMessage(miniMessage.deserialize("<green>Gave Cave Compass to " + target.getName() + "!</green>"));
        }

        return true;
    }

    private boolean handleSmelterPickaxe(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermining.give.smelterpickaxe")) {
            String noPermMsg = plugin.getConfig().getString("messages.no-permission", "<red>You don't have permission to do that!</red>");
            sender.sendMessage(miniMessage.deserialize(noPermMsg));
            return true;
        }

        Player target;

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(miniMessage.deserialize("<red>Console must specify a player!</red>"));
                return true;
            }
            target = player;
        } else {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(miniMessage.deserialize("<red>Player not found: " + args[0] + "</red>"));
                return true;
            }
        }

        var smelterPickaxe = plugin.getAutoSmelterPickaxeManager().createAutoSmelterPickaxe();
        target.getInventory().addItem(smelterPickaxe);
        
        String givenMsg = plugin.getConfig().getString("messages.smelter-pickaxe-given", 
            "<green>You received a <red>Smelter's Pickaxe</red>! Ores are auto-smelted when mined.</green>");
        target.sendMessage(miniMessage.deserialize(givenMsg));
        
        if (sender != target) {
            sender.sendMessage(miniMessage.deserialize("<green>Gave Smelter's Pickaxe to " + target.getName() + "!</green>"));
        }

        return true;
    }

    private boolean handleMinersGoggles(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powermining.give.minersgoggles")) {
            String noPermMsg = plugin.getConfig().getString("messages.no-permission", "<red>You don't have permission to do that!</red>");
            sender.sendMessage(miniMessage.deserialize(noPermMsg));
            return true;
        }

        Player target;
        int radius = plugin.getConfig().getInt("miners-goggles.default-radius", 10);
        String filter = null;

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(miniMessage.deserialize("<red>Console must specify a player!</red>"));
                return true;
            }
            target = player;
        } else {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(miniMessage.deserialize("<red>Player not found: " + args[0] + "</red>"));
                return true;
            }

            if (args.length >= 2) {
                try {
                    radius = Integer.parseInt(args[1]);
                    int minRadius = plugin.getConfig().getInt("miners-goggles.min-radius", 5);
                    int maxRadius = plugin.getConfig().getInt("miners-goggles.max-radius", 20);
                    if (radius < minRadius || radius > maxRadius) {
                        sender.sendMessage(miniMessage.deserialize("<red>Radius must be between " + minRadius + " and " + maxRadius + "!</red>"));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(miniMessage.deserialize("<red>Invalid radius: " + args[1] + "</red>"));
                    return true;
                }
            }

            for (int i = 2; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("--filter") && i + 1 < args.length) {
                    filter = args[i + 1].toUpperCase();
                    i++;
                }
            }
        }

        var goggles = plugin.getMinersGogglesManager().createMinersGoggles(radius, filter);
        target.getInventory().addItem(goggles);

        String filterDisplay = filter == null ? "All Ores" : filter;
        String givenMsg = plugin.getConfig().getString("messages.miners-goggles-given",
                "<green>You received <gold>Miner's Goggles</gold> with radius <yellow>{radius}</yellow> and filter <yellow>{filter}</yellow>!</green>");
        target.sendMessage(miniMessage.deserialize(givenMsg
                .replace("{radius}", String.valueOf(radius))
                .replace("{filter}", filterDisplay)));

        if (sender != target) {
            sender.sendMessage(miniMessage.deserialize("<green>Gave Miner's Goggles to " + target.getName() + "!</green>"));
        }

        return true;
    }

    private boolean handleDrill(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("═══ Mounted Mining (Drill) ═══")
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.BOLD, true));
        sender.sendMessage(Component.empty());
        
        sender.sendMessage(Component.text("✦ ")
            .color(NamedTextColor.YELLOW)
            .append(Component.text("Ride a horse, donkey, mule, or similar mount")
                .color(NamedTextColor.WHITE)));
        
        sender.sendMessage(Component.text("✦ ")
            .color(NamedTextColor.YELLOW)
            .append(Component.text("Hold any pickaxe in your main hand")
                .color(NamedTextColor.WHITE)));
        
        sender.sendMessage(Component.text("✦ ")
            .color(NamedTextColor.YELLOW)
            .append(Component.text("Move forward to automatically mine blocks!")
                .color(NamedTextColor.WHITE)));
        
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("The drill mines a 3x3x3 area in front of your mount,")
            .color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("never mining below the mount's hooves.")
            .color(NamedTextColor.GRAY));
        sender.sendMessage(Component.empty());
        
        sender.sendMessage(Component.text("Tip: ")
            .color(NamedTextColor.AQUA)
            .decoration(TextDecoration.BOLD, true)
            .append(Component.text("Use a Netherite Pickaxe with Efficiency V for maximum power!")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.BOLD, false)));
        sender.sendMessage(Component.empty());
        
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> subCommands = Arrays.asList("magnethopper", "orebell", "helmet", "escaperope", 
                    "cavecompass", "smelterpick", "goggles", "drill", "help");
            completions = subCommands.stream()
                .filter(cmd -> cmd.startsWith(partial))
                .collect(Collectors.toList());
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("magnethopper") || subCommand.equals("mh") || 
                subCommand.equals("orebell") || subCommand.equals("ob") ||
                subCommand.equals("helmet") || subCommand.equals("minershelmet") ||
                subCommand.equals("escaperope") || subCommand.equals("rope") || subCommand.equals("er") ||
                subCommand.equals("cavecompass") || subCommand.equals("compass") || subCommand.equals("cc") ||
                subCommand.equals("smelterpick") || subCommand.equals("smelter") || subCommand.equals("sp") ||
                subCommand.equals("goggles") || subCommand.equals("minersgoggles") || subCommand.equals("mg")) {
                String partial = args[1].toLowerCase();
                completions = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("magnethopper") || subCommand.equals("mh")) {
                completions.addAll(Arrays.asList("8", "16", "24", "32"));
            } else if (subCommand.equals("orebell") || subCommand.equals("ob")) {
                completions.addAll(Arrays.asList("16", "32", "48", "64"));
            } else if (subCommand.equals("goggles") || subCommand.equals("minersgoggles") || subCommand.equals("mg")) {
                completions.addAll(Arrays.asList("5", "10", "15", "20"));
            }
        } else if (args.length >= 4) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("orebell") || subCommand.equals("ob")) {
                String lastArg = args[args.length - 1].toLowerCase();
                String prevArg = args.length > 1 ? args[args.length - 2].toLowerCase() : "";
                
                if (prevArg.equals("--filter")) {
                    completions.addAll(Arrays.asList(
                        "DIAMOND_ORE", "IRON_ORE", "GOLD_ORE", "COAL_ORE", 
                        "COPPER_ORE", "EMERALD_ORE", "LAPIS_ORE", "REDSTONE_ORE",
                        "ANCIENT_DEBRIS", "NETHER_QUARTZ_ORE", "NETHER_GOLD_ORE"
                    ));
                    completions = completions.stream()
                        .filter(s -> s.toLowerCase().startsWith(lastArg))
                        .collect(Collectors.toList());
                } else if (prevArg.equals("--duration")) {
                    completions.addAll(Arrays.asList("60", "100", "200"));
                } else {
                    if ("--filter".startsWith(lastArg)) completions.add("--filter");
                    if ("--duration".startsWith(lastArg)) completions.add("--duration");
                }
            } else if (subCommand.equals("goggles") || subCommand.equals("minersgoggles") || subCommand.equals("mg")) {
                String lastArg = args[args.length - 1].toLowerCase();
                String prevArg = args.length > 1 ? args[args.length - 2].toLowerCase() : "";
                
                if (prevArg.equals("--filter")) {
                    completions.addAll(Arrays.asList(
                        "DIAMOND_ORE", "IRON_ORE", "GOLD_ORE", "COAL_ORE", 
                        "COPPER_ORE", "EMERALD_ORE", "LAPIS_ORE", "REDSTONE_ORE",
                        "ANCIENT_DEBRIS", "NETHER_QUARTZ_ORE", "NETHER_GOLD_ORE"
                    ));
                    completions = completions.stream()
                        .filter(s -> s.toLowerCase().startsWith(lastArg))
                        .collect(Collectors.toList());
                } else {
                    if ("--filter".startsWith(lastArg)) completions.add("--filter");
                }
            }
        }
        
        return completions;
    }
}
