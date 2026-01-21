package com.example.advancedachievements.managers;

import com.example.advancedachievements.AdvancedAchievements;
import com.example.advancedachievements.models.Achievement;
import com.example.advancedachievements.models.PlayerProgress;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class RewardManager {
    private final AdvancedAchievements plugin;

    public RewardManager(AdvancedAchievements plugin) {
        this.plugin = plugin;
    }

    public boolean claimReward(Player player, String achievementId) {
        Achievement achievement = plugin.getAchievementManager().getAchievement(achievementId);
        if (achievement == null) {
            return false;
        }

        PlayerProgress progress = plugin.getProgressManager().getPlayerProgress(player, achievementId);
        if (!progress.isUnlocked()) {
            plugin.getMessageManager().sendMessage(player, "reward-not-available");
            return false;
        }

        if (progress.isClaimed()) {
            plugin.getMessageManager().sendMessage(player, "reward-already-claimed");
            return false;
        }

        giveRewards(player, achievement);

        progress.setClaimed(true);
        plugin.getDatabaseManager().savePlayerProgress(progress);

        plugin.getMessageManager().sendMessage(player, "reward-claimed");
        return true;
    }

    public void giveRewards(Player player, Achievement achievement) {
        List<String> rewards = achievement.getRewards();
        for (String reward : rewards) {
            if (reward.isEmpty()) continue;
            String[] parts = reward.split(":", 3);
            if (parts.length < 2) continue;
            String type = parts[0].toUpperCase();
            switch (type) {
                case "ITEM":
                    giveItemReward(player, parts);
                    break;
                case "XP":
                case "EXPERIENCE":
                    giveXPReward(player, parts[1]);
                    break;
                case "MONEY":
                    giveMoneyReward(player, parts[1]);
                    break;
                case "COMMAND":
                    executeCommandReward(player, parts[1]);
                    break;
                case "TITLE":
                    giveTitleReward(player, parts[1]);
                    break;
                default:
                    if (parts.length == 2 && type.matches("[A-Z_]+") && parts[1].matches("\\d+")) {
                        giveItemReward(player, new String[]{"ITEM", type, parts[1]});
                    } else if ((type.equals("EXPERIENCE") || type.equals("XP")) && parts[1].matches("\\d+")) {
                        giveXPReward(player, parts[1]);
                    } else {
                        plugin.getLogger().warning("Unknown reward type: " + type);
                    }
                    break;
            }
        }
    }

    private void giveItemReward(Player player, String[] parts) {
        if (parts.length < 3) return;
        try {
            Material material = Material.valueOf(parts[1].toUpperCase());
            int amount = Integer.parseInt(parts[2]);
            ItemStack item = new ItemStack(material, amount);
            if (parts.length > 3) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(plugin.getMessageManager().colorize(parts[3]));
                    item.setItemMeta(meta);
                }
            }
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(item);
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid item amount: " + String.join(":", parts));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid item reward: " + String.join(":", parts));
        }
    }

    private void giveXPReward(Player player, String amountStr) {
        try {
            int amount = Integer.parseInt(amountStr);
            player.giveExp(amount);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid XP amount: " + amountStr);
        }
    }

    private void giveMoneyReward(Player player, String amountStr) {
        if (!plugin.hasEconomy()) {
            plugin.getLogger().warning("Economy not available for money reward");
            return;
        }
        try {
            double amount = Double.parseDouble(amountStr);
            boolean success = plugin.depositPlayer(player.getName(), amount);
            if (!success) {
                plugin.getLogger().warning("Failed to give money reward to " + player.getName());
            }
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid money amount: " + amountStr);
        }
    }

    private void executeCommandReward(Player player, String command) {
        String processedCommand = command.replace("{player}", player.getName());
        processedCommand = processedCommand.replace("{uuid}", player.getUniqueId().toString());
        final String finalCommand = processedCommand;
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
        });
    }

    private void giveTitleReward(Player player, String title) {
        plugin.getMessageManager().sendRawMessage(player, "§6Title Unlocked: §f" + title);
    }

    public boolean canClaimReward(Player player, String achievementId) {
        PlayerProgress progress = plugin.getProgressManager().getPlayerProgress(player, achievementId);
        return progress.isUnlocked() && !progress.isClaimed();
    }

    public boolean hasClaimedReward(Player player, String achievementId) {
        PlayerProgress progress = plugin.getProgressManager().getPlayerProgress(player, achievementId);
        return progress.isClaimed();
    }
}
