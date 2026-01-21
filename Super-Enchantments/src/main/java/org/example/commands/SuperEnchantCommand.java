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
import org.example.util.EnchantmentManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SuperEnchantCommand implements CommandExecutor, TabCompleter {

    private final SuperEnchantments plugin;
    private final EnchantmentManager enchantmentManager;
    private final Map<String, Integer> maxEnchantLevels;

    public SuperEnchantCommand(SuperEnchantments plugin) {
        this.plugin = plugin;
        this.enchantmentManager = plugin.getEnchantmentManager();
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

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " <Enchantment> [Level]");
            return false;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "You must hold an item in your hand.");
            return true;
        }

        Optional<Enchantment> enchOptional = enchantmentManager.getEnchantmentByName(args[0]);
        if (!enchOptional.isPresent()) {
            player.sendMessage(ChatColor.RED + "Enchantment '" + args[0] + "' not found.");
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
            List<String> availableEnchants = enchantmentManager.getAvailableEnchantments(item);

            return availableEnchants.stream()
                    .filter(enchName -> enchName.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
