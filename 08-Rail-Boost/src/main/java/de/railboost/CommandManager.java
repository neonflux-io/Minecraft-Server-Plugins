package de.railboost;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Material;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

public class CommandManager {

    private final RailBoostPlugin plugin;
    private final MinecartController controller;
    private final PresetManager presetManager;

    public CommandManager(RailBoostPlugin plugin, MinecartController controller, PresetManager presetManager) {
        this.plugin = plugin;
        this.controller = controller;
        this.presetManager = presetManager;
    }

    public boolean handleCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        String command = args[0].toLowerCase();

        switch (command) {
            case "help":
                showHelp(player);
                return true;
            case "preset":
                return handlePreset(player, args);
            case "give":
                return handleGive(player, args);
            case "create":
                return handleCreate(player, args);
            case "edit":
                return handleEdit(player, args);
            case "list":
                return handleList(player);
            case "delete":
                return handleDelete(player, args);
            default:
                player.sendMessage("§cUnknown command. Use /railboost help");
                return true;
        }
    }

    private boolean handlePreset(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c/railboost preset <name>");
            return true;
        }

        String presetName = args[1].toLowerCase();
        presetManager.givePresetStick(player, presetName);
        return true;
    }

    private boolean handleGive(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§c/railboost give <player> <preset>");
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }

        String presetName = args[2].toLowerCase();
        presetManager.givePresetStick(target, presetName);
        player.sendMessage("§aGave preset stick '" + presetName + "' to " + target.getName());
        return true;
    }

    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c/railboost create <name>");
            return true;
        }

        String name = args[1].toLowerCase();
        MinecartSettings settings = new MinecartSettings(name);
        presetManager.savePreset(name, settings);
        player.sendMessage("§aCreated preset: " + name);
        return true;
    }

    private boolean handleEdit(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§c/railboost edit <preset> <setting> [value]");
            player.sendMessage("§7Settings: speed, autopickup, radius, speedometer, chunkload, magnet, effects, effecttype, blacklist, storage");
            return true;
        }

        String presetName = args[1].toLowerCase();
        String setting = args[2].toLowerCase();

        MinecartSettings settings = presetManager.getPreset(presetName);
        if (settings == null) {
            player.sendMessage("§cPreset not found: " + presetName);
            return true;
        }

        if (setting.equals("storage")) {
            org.bukkit.inventory.Inventory storage = controller.getStorageForPreset(presetName);
            player.openInventory(storage);
            player.sendMessage("§aOpened shared storage for preset: " + presetName);
            player.sendMessage("§7All minecarts with this preset share this storage!");
            return true;
        }

        if (args.length < 4) {
            player.sendMessage("§c/railboost edit <preset> <setting> <value>");
            return true;
        }

        String value = args[3];

        switch (setting) {
            case "speed":
                try {
                    int speed = Integer.parseInt(value);
                    if (speed < 1 || speed > 6) {
                        player.sendMessage("§cSpeed must be between 1 and 6!");
                        return true;
                    }
                    settings.setSpeed(speed);
                    player.sendMessage("§aSet speed to: " + speed + " (" + MinecartController.getSpeedNames()[speed - 1] + ")");
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid number!");
                }
                break;
            case "autopickup":
                boolean pickup = parseBoolean(value);
                settings.setAutoPickup(pickup);
                player.sendMessage("§aAuto pickup: " + (pickup ? "enabled" : "disabled"));
                break;
            case "radius":
                try {
                    int radius = Integer.parseInt(value);
                    if (radius < 1 || radius > 5) {
                        player.sendMessage("§cRadius must be between 1 and 5!");
                        return true;
                    }
                    settings.setPickupRadius(radius);
                    player.sendMessage("§aSet pickup radius to: " + radius);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid number!");
                }
                break;
            case "speedometer":
                boolean speedometer = parseBoolean(value);
                settings.setSpeedometer(speedometer);
                player.sendMessage("§aSpeedometer: " + (speedometer ? "enabled" : "disabled"));
                break;
            case "chunkload":
                boolean chunkload = parseBoolean(value);
                settings.setChunkLoad(chunkload);
                player.sendMessage("§aChunk loading: " + (chunkload ? "enabled" : "disabled"));
                break;
            case "magnet":
                boolean magnet = parseBoolean(value);
                settings.setMagnet(magnet);
                player.sendMessage("§aMagnet: " + (magnet ? "enabled" : "disabled"));
                break;
            case "effects":
                boolean effects = parseBoolean(value);
                settings.setEffects(effects);
                player.sendMessage("§aEffects: " + (effects ? "enabled" : "disabled"));
                break;
            case "effecttype":
                try {
                    org.bukkit.Particle.valueOf(value.toUpperCase());
                    settings.setEffectType(value.toUpperCase());
                    player.sendMessage("§aSet effect type to: " + value.toUpperCase());
                } catch (IllegalArgumentException e) {
                    player.sendMessage("§cInvalid particle type!");
                }
                break;
            case "blacklist":
                if (args.length < 5) {
                    player.sendMessage("§c/railboost edit <preset> blacklist <add|remove> <material>");
                    return true;
                }
                String action = args[3].toLowerCase();
                String materialInput = args[4];

                String materialName = materialInput.toUpperCase().replace(" ", "_");
                Material material = Material.matchMaterial(materialName);
                if (material == null) {
                    player.sendMessage("§cUnknown material: " + materialInput);
                    return true;
                }

                Set<Material> blacklist = settings.getBlacklist();
                String displayName = formatMaterialName(material.name());

                if (action.equals("add")) {
                    blacklist.add(material);
                    player.sendMessage("§aAdded §e" + displayName + " §ato blacklist");
                } else if (action.equals("remove")) {
                    if (blacklist.remove(material)) {
                        player.sendMessage("§aRemoved §e" + displayName + " §afrom blacklist");
                    } else {
                        player.sendMessage("§c" + displayName + " is not in blacklist");
                    }
                } else {
                    player.sendMessage("§cUse 'add' or 'remove'");
                    return true;
                }
                break;
            default:
                player.sendMessage("§cUnknown setting: " + setting);
                return true;
        }

        presetManager.savePreset(presetName, settings);
        return true;
    }

    private boolean handleList(Player player) {
        Set<String> presets = presetManager.getPresetNames();
        if (presets.isEmpty()) {
            player.sendMessage("§7No presets available");
            return true;
        }

        player.sendMessage("§e--- Available Presets ---");
        for (String presetName : presets) {
            MinecartSettings settings = presetManager.getPreset(presetName);
            player.sendMessage(settings.toString());
        }
        return true;
    }

    private boolean handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c/railboost delete <preset>");
            return true;
        }

        String presetName = args[1].toLowerCase();
        if (presetManager.getPreset(presetName) == null) {
            player.sendMessage("§cPreset not found: " + presetName);
            return true;
        }

        presetManager.deletePreset(presetName);
        player.sendMessage("§aDeleted preset: " + presetName);
        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage("§e--- RailBoost Commands ---");
        player.sendMessage("§7/railboost preset <name> - Get a preset stick");
        player.sendMessage("§7/railboost create <name> - Create new preset");
        player.sendMessage("§7/railboost edit <preset> <setting> <value> - Edit preset");
        player.sendMessage("§7/railboost edit <preset> storage - Edit preset storage");
        player.sendMessage("§7/railboost list - List all presets");
        player.sendMessage("§7/railboost delete <preset> - Delete preset");
        player.sendMessage("§7/railboost give <player> <preset> - Give stick to player");
        player.sendMessage("");
        player.sendMessage("§7Use the stick on any minecart to apply settings!");
        player.sendMessage("§7For Chest/Hopper minecarts: Press F to access inventory");
    }

    private boolean parseBoolean(String value) {
        return value.equalsIgnoreCase("true") ||
                value.equalsIgnoreCase("on") ||
                value.equals("1");
    }

    private String formatMaterialName(String materialName) {
        StringBuilder result = new StringBuilder();
        String[] parts = materialName.split("_");

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) result.append("_");

            String part = parts[i].toLowerCase();
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1));
            }
        }

        return result.toString();
    }

    public List<String> handleTabComplete(CommandSender sender, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.addAll(Arrays.asList("preset", "create", "edit", "list", "delete", "give", "help"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("preset") || args[0].equalsIgnoreCase("edit") ||
                    args[0].equalsIgnoreCase("delete")) {
                suggestions.addAll(presetManager.getPresetNames());
            } else if (args[0].equalsIgnoreCase("give")) {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    suggestions.add(player.getName());
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("edit")) {
                suggestions.addAll(Arrays.asList("speed", "autopickup", "radius", "speedometer",
                        "chunkload", "magnet", "effects", "effecttype", "blacklist", "storage"));
            } else if (args[0].equalsIgnoreCase("give")) {
                suggestions.addAll(presetManager.getPresetNames());
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("edit")) {
                String setting = args[2].toLowerCase();
                switch (setting) {
                    case "speed":
                        suggestions.addAll(Arrays.asList("1", "2", "3", "4", "5", "6"));
                        break;
                    case "radius":
                        suggestions.addAll(Arrays.asList("1", "2", "3", "4", "5"));
                        break;
                    case "effecttype":
                        suggestions.addAll(Arrays.asList("FLAME", "END_ROD", "PORTAL", "CLOUD",
                                "VILLAGER_HAPPY", "CRIT", "SPELL_WITCH", "DRAGON_BREATH"));
                        break;
                    case "blacklist":
                        suggestions.addAll(Arrays.asList("add", "remove"));
                        break;
                    default:
                        suggestions.addAll(Arrays.asList("true", "false"));
                        break;
                }
            }
        } else if (args.length == 5) {
            if (args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("blacklist")) {
                String input = args[4].toLowerCase().replace(" ", "_");

                for (Material material : Material.values()) {
                    String materialName = formatMaterialName(material.name());
                    String materialLower = materialName.toLowerCase();

                    if (materialLower.startsWith(input) || materialLower.contains(input)) {
                        suggestions.add(materialName);
                        if (suggestions.size() >= 50) break;
                    }
                }
            }
        }

        return suggestions;
    }
}