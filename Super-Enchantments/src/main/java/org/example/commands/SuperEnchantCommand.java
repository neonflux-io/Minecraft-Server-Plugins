package org.example.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.example.SuperEnchantments;
import org.example.enchantments.CustomEnchantment;
import org.example.util.CustomEnchantmentManager;
import org.example.util.EnchantmentManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SuperEnchantCommand implements CommandExecutor, TabCompleter {

    private final SuperEnchantments plugin;
    private final EnchantmentManager enchantmentManager;
    private final CustomEnchantmentManager customEnchantmentManager;
    private final Map<String, Integer> maxEnchantLevels;

    public SuperEnchantCommand(SuperEnchantments plugin) {
        this.plugin = plugin;
        this.enchantmentManager = plugin.getEnchantmentManager();
        this.customEnchantmentManager = plugin.getCustomEnchantmentManager();
        this.maxEnchantLevels = initializeMaxEnchantLevels();
    }

    private Map<String, Integer> initializeMaxEnchantLevels() {
        Map<String, Integer> levels = new HashMap<>();

        levels.put("sharpness", 5);
        levels.put("smite", 5);
        levels.put("baneofarthropods", 5);
        levels.put("knockback", 2);
        levels.put("fireaspect", 2);
        levels.put("looting", 3);
        levels.put("sweepingedge", 3);
        levels.put("efficiency", 5);
        levels.put("silktouch", 1);
        levels.put("unbreaking", 3);
        levels.put("fortune", 3);
        levels.put("power", 5);
        levels.put("punch", 2);
        levels.put("flame", 1);
        levels.put("infinity", 1);
        levels.put("multishot", 1);
        levels.put("quickcharge", 3);
        levels.put("piercing", 4);
        levels.put("loyalty", 3);
        levels.put("impaling", 5);
        levels.put("riptide", 3);
        levels.put("channeling", 1);
        levels.put("protection", 4);
        levels.put("fireprotection", 4);
        levels.put("blastprotection", 4);
        levels.put("projectileprotection", 4);
        levels.put("thorns", 3);
        levels.put("respiration", 3);
        levels.put("depthstrider", 3);
        levels.put("aquaaffinity", 1);
        levels.put("featherfalling", 4);
        levels.put("respiration", 3);
        levels.put("aquaaffinity", 1);
        levels.put("depthstrider", 3);
        levels.put("frostwalker", 2);
        levels.put("soulspeed", 3);
        levels.put("mending", 1);
        levels.put("vanishingcurse", 1);
        levels.put("bindingcurse", 1);
        levels.put("swiftsneak", 3);
        levels.put("luckofthesea", 3);
        levels.put("lure", 3);
        levels.put("arrow_storm", 5);
        levels.put("blazing_shot", 5);
        levels.put("venom_strike", 5);
        levels.put("thunderbolt", 5);
        levels.put("frostbite", 5);
        levels.put("void_step", 5);
        levels.put("explosive_tip", 5);
        levels.put("soul_drain", 5);
        levels.put("pack_leader", 5);

        return levels;
    }

    private int getMaxLevel(String enchantmentName) {
        return maxEnchantLevels.getOrDefault(enchantmentName, 1);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length >= 1 && args[0].equalsIgnoreCase("togglecooldown")) {
            if (!player.hasPermission("superenchant.admin")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

            boolean newState = !plugin.isCooldownDisplayEnabled();
            plugin.setCooldownDisplayEnabled(newState);
            player.sendMessage(ChatColor.GREEN + "Cooldown display " + (newState ? "enabled" : "disabled") + ".");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage:");
            player.sendMessage(ChatColor.GOLD + "/" + label + " <Enchantment> [Level]" + ChatColor.WHITE + " - Apply vanilla enchantment");
            player.sendMessage(ChatColor.GOLD + "/" + label + " customs <CustomEnchantment> [Level]" + ChatColor.WHITE + " - Apply custom enchantment");
            return false;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "You must hold an item in your hand.");
            return true;
        }

        if (args[0].equalsIgnoreCase("customs")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /" + label + " customs <CustomEnchantment> [Level]");
                return false;
            }

            String customEnchantName = args[1].toLowerCase();
            Optional<CustomEnchantment> customEnchantment = customEnchantmentManager.getCustomEnchantment(customEnchantName);

            if (!customEnchantment.isPresent()) {
                player.sendMessage(ChatColor.RED + "Custom enchantment '" + customEnchantName + "' not found.");
                return true;
            }

            CustomEnchantment enchantment = customEnchantment.get();

            if (!enchantment.canApplyTo(item)) {
                player.sendMessage(ChatColor.RED + "The enchantment " + ChatColor.GOLD + enchantment.getDisplayName() +
                        ChatColor.RED + " cannot be applied to this item.");
                return true;
            }

            int level;
            if (args.length < 3) {
                level = enchantment.getMaxLevel();
                player.sendMessage(ChatColor.YELLOW + "No level specified, using maximum level: " + level);
            } else {
                try {
                    level = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "The level must be a number between 1 and " + enchantment.getMaxLevel() + ".");
                    return true;
                }

                if (level < 1 || level > enchantment.getMaxLevel()) {
                    player.sendMessage(ChatColor.RED + "The level must be between 1 and " + enchantment.getMaxLevel() + ".");
                    return true;
                }
            }

            ItemStack enchantedItem = customEnchantmentManager.applyCustomEnchantment(item, enchantment, level);
            player.getInventory().setItemInMainHand(enchantedItem);

            player.sendMessage(ChatColor.GREEN + "The custom enchantment " + ChatColor.GOLD + enchantment.getDisplayName() + " " + level +
                    ChatColor.GREEN + " was successfully applied!");

            return true;
        }

        String vanillaEnchantName = args[0].toLowerCase();
        Optional<Enchantment> enchOptional = enchantmentManager.getEnchantmentByName(vanillaEnchantName);
        if (!enchOptional.isPresent()) {
            player.sendMessage(ChatColor.RED + "Vanilla enchantment '" + vanillaEnchantName + "' not found.");
            player.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.GOLD + "/" + label + " customs <enchantment>" + ChatColor.YELLOW + " for custom enchantments.");
            return true;
        }

        Enchantment enchantment = enchOptional.get();

        if (!enchantment.canEnchantItem(item)) {
            String displayName = enchantmentManager.getDisplayName(enchantment);
            player.sendMessage(ChatColor.RED + "The enchantment " + ChatColor.GOLD + displayName +
                    ChatColor.RED + " cannot be applied to this item.");
            return true;
        }

        int level;
        if (args.length < 2) {
            String enchName = enchantment.getKey().getKey().replace("_", "").toLowerCase();
            level = getMaxLevel(enchName);
            player.sendMessage(ChatColor.YELLOW + "No level specified, using maximum vanilla level: " + level);
        } else {
            try {
                level = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "The level must be a number between 1 and 255.");
                return true;
            }

            if (level < 1 || level > 255) {
                player.sendMessage(ChatColor.RED + "The level must be between 1 and 255.");
                return true;
            }
        }

        ItemStack enchantedItem = enchantmentManager.applyEnchantment(item, enchantment, level);
        player.getInventory().setItemInMainHand(enchantedItem);

        String displayName = enchantmentManager.getDisplayName(enchantment);
        player.sendMessage(ChatColor.GREEN + "The enchantment " + ChatColor.GOLD + displayName + " " + level +
                ChatColor.GREEN + " was successfully applied!");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (args.length == 1) {
            if (item.getType().isAir()) {
                return Collections.emptyList();
            }

            String partial = args[0].toLowerCase();
            List<String> suggestions = new ArrayList<>();
            suggestions.addAll(enchantmentManager.getAvailableEnchantments(item));

            if ("customs".startsWith(partial)) {
                suggestions.add("customs");
            }

            return suggestions.stream()
                    .filter(suggestion -> suggestion.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("customs")) {
                if (item.getType().isAir()) {
                    return Collections.emptyList();
                }

                String partial = args[1].toLowerCase();
                return customEnchantmentManager.getAvailableCustomEnchantments(item).stream()
                        .filter(enchName -> enchName.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());
            } else {
                String enchantmentName = args[0].toLowerCase();
                Optional<Enchantment> enchOptional = enchantmentManager.getEnchantmentByName(enchantmentName);
                if (enchOptional.isPresent()) {
                    List<String> levels = new ArrayList<>();
                    String enchName = enchOptional.get().getKey().getKey().replace("_", "").toLowerCase();
                    int maxLevel = getMaxLevel(enchName);
                    for (int i = 1; i <= Math.min(maxLevel, 10); i++) {
                        levels.add(String.valueOf(i));
                    }
                    return levels;
                }
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("customs")) {
            String enchantmentName = args[1].toLowerCase();
            Optional<CustomEnchantment> customEnchantment = customEnchantmentManager.getCustomEnchantment(enchantmentName);
            if (customEnchantment.isPresent()) {
                List<String> levels = new ArrayList<>();
                int maxLevel = customEnchantment.get().getMaxLevel();
                for (int i = 1; i <= maxLevel; i++) {
                    levels.add(String.valueOf(i));
                }
                return levels;
            }
        }

        return Collections.emptyList();
    }
}