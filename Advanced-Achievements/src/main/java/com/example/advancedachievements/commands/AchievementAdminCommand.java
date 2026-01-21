package com.example.advancedachievements.commands;

import com.example.advancedachievements.AdvancedAchievements;
import com.example.advancedachievements.enums.TaskType;
import com.example.advancedachievements.models.Achievement;
import com.example.advancedachievements.models.PlayerProgress;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AchievementAdminCommand implements CommandExecutor, TabCompleter {
    
    private final AdvancedAchievements plugin;
    
    public AchievementAdminCommand(AdvancedAchievements plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("advancedachievements.admin")) {
            if (sender instanceof Player) {
                plugin.getMessageManager().sendMessage((Player) sender, "no-permission");
            } else {
                sender.sendMessage("§cYou don't have permission to use this command!");
            }
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "create":
                return handleCreate(sender, args);
            case "delete":
                return handleDelete(sender, args);
            case "give":
                return handleGive(sender, args);
            case "reset":
                return handleReset(sender, args);
            case "reload":
                return handleReload(sender, args);
            case "edit":
                return handleEdit(sender, args);
            case "list":
                return handleAdminList(sender, args);
            case "info":
                return handleAdminInfo(sender, args);
            case "resetall":
                return handleResetAll(sender, args);
            case "progress":
                return handleSetProgress(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    private boolean handleCreate(CommandSender sender, String[] args) {
        if (args.length < 8) {
            sender.sendMessage("§cUsage: /achadmin create <id> <title> <description> <icon> <tasktype> <target> <amount> [rewards...]");
            return true;
        }

        String id = args[1];
        String title = args[2].replace("_", " ");
        String description = args[3].replace("_", " ");

        Material icon;
        try {
            icon = Material.valueOf(args[4].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid material: " + args[4]);
            return true;
        }

        TaskType taskType = TaskType.fromId(args[5]);
        if (taskType == null) {
            sender.sendMessage("§cInvalid task type: " + args[5]);
            sender.sendMessage("§7Available types: " + TaskType.getAvailableTypes());
            return true;
        }

        String target = args[6];

        int amount;
        try {
            amount = Integer.parseInt(args[7]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid amount: " + args[7]);
            return true;
        }

        List<String> rewards = new ArrayList<>();
        for (int i = 8; i < args.length; i++) {
            rewards.add(args[i]);
        }
        if (plugin.getAchievementManager().achievementExists(id)) {
            if (sender instanceof Player) {
                plugin.getMessageManager().sendMessage((Player) sender, "achievement-exists");
            } else {
                sender.sendMessage("§cAn achievement with this ID already exists!");
            }
            return true;
        }

        Achievement achievement = plugin.getAchievementManager().createAchievement(
            id, title, description, icon, taskType, target, amount, rewards, false, new ArrayList<>()
        );

        if (sender instanceof Player) {
            plugin.getMessageManager().sendMessage((Player) sender, "achievement-created");
        } else {
            sender.sendMessage("§aAchievement created successfully!");
        }
        sender.sendMessage("§aCreated achievement: §f" + achievement.getTitle() + " §7(" + id + ")");

        return true;
    }
    
    private boolean handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /achadmin delete <id>");
            return true;
        }
        
        String id = args[1];
        Achievement achievement = plugin.getAchievementManager().getAchievement(id);
          if (achievement == null) {
            if (sender instanceof Player) {
                plugin.getMessageManager().sendMessage((Player) sender, "invalid-achievement");
            } else {
                sender.sendMessage("§cAchievement not found!");
            }
            return true;
        }
        
        if (plugin.getAchievementManager().deleteAchievement(id)) {
            if (sender instanceof Player) {
                plugin.getMessageManager().sendMessage((Player) sender, "achievement-deleted");
            } else {
                sender.sendMessage("§aAchievement deleted successfully!");
            }
            sender.sendMessage("§aDeleted achievement: §f" + achievement.getTitle() + " §7(" + id + ")");
        } else {
            sender.sendMessage("§cFailed to delete achievement!");
        }
        
        return true;
    }
    
    private boolean handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /achadmin give <player> <achievement_id>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[1]);
            return true;
        }
          String achievementId = args[2];
        Achievement achievement = plugin.getAchievementManager().getAchievement(achievementId);
        
        if (achievement == null) {
            if (sender instanceof Player) {
                plugin.getMessageManager().sendMessage((Player) sender, "invalid-achievement");
            } else {
                sender.sendMessage("§cAchievement not found!");
            }
            return true;
        }
        
        plugin.getProgressManager().giveAchievement(target, achievementId);
        sender.sendMessage("§aGave achievement §f" + achievement.getTitle() + " §ato §f" + target.getName());
        
        return true;
    }
    
    private boolean handleReset(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /achadmin reset <player> <achievement_id>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[1]);
            return true;
        }
          String achievementId = args[2];
        Achievement achievement = plugin.getAchievementManager().getAchievement(achievementId);
        
        if (achievement == null) {
            if (sender instanceof Player) {
                plugin.getMessageManager().sendMessage((Player) sender, "invalid-achievement");
            } else {
                sender.sendMessage("§cAchievement not found!");
            }
            return true;
        }
        
        plugin.getProgressManager().resetPlayerProgress(target, achievementId);
        if (sender instanceof Player) {
            plugin.getMessageManager().sendMessage((Player) sender, "achievement-reset");
        } else {
            sender.sendMessage("§aProgress reset successfully!");
        }
        sender.sendMessage("§aReset achievement §f" + achievement.getTitle() + " §afor §f" + target.getName());
        
        return true;
    }
    
    private boolean handleReload(CommandSender sender, String[] args) {
        plugin.getConfigManager().reloadConfigs();
        plugin.getAchievementManager().reloadAchievements();
        sender.sendMessage("§aAdvanced Achievements reloaded!");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6Advanced Achievements Admin Commands:");
        sender.sendMessage("§7/achadmin create <id> <title> <desc> <icon> <tasktype> <target> <amount> [rewards...] §f- Create achievement");
        sender.sendMessage("§7/achadmin delete <id> §f- Delete achievement");
        sender.sendMessage("§7/achadmin edit <id> <property> <value> §f- Edit achievement property");
        sender.sendMessage("§7/achadmin give <player> <id> §f- Give achievement to player");
        sender.sendMessage("§7/achadmin reset <player> <id> §f- Reset player's achievement progress");
        sender.sendMessage("§7/achadmin resetall <player> [confirm] §f- Reset ALL player's achievements");
        sender.sendMessage("§7/achadmin progress <player> <id> <amount> §f- Set player's achievement progress");
        sender.sendMessage("§7/achadmin list §f- List all achievements");
        sender.sendMessage("§7/achadmin info <id> §f- Get detailed achievement info");
        sender.sendMessage("§7/achadmin reload §f- Reload plugin");
        sender.sendMessage("");
        sender.sendMessage("§6Task Types: §f" + TaskType.getAvailableTypes());
        sender.sendMessage("§6Editable Properties: §ftitle, description, icon, target, amount, rewards, hidden");
        sender.sendMessage("§6Reward Format: §fTYPE:VALUE:AMOUNT");
        sender.sendMessage("§7Examples: ITEM:DIAMOND:5, XP:100, MONEY:1000, COMMAND:give_{player}_diamond_1");
    }
    
    private boolean handleEdit(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /achadmin edit <id> <property> <value>");
            sender.sendMessage("§7Properties: title, description, icon, target, amount, rewards, hidden");
            return true;
        }
        
        String id = args[1];
        Achievement achievement = plugin.getAchievementManager().getAchievement(id);
        
        if (achievement == null) {
            if (sender instanceof Player) {
                plugin.getMessageManager().sendMessage((Player) sender, "invalid-achievement");
            } else {
                sender.sendMessage("§cAchievement not found!");
            }
            return true;
        }
        
        String property = args[2].toLowerCase();
        String value = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        
        try {
            switch (property) {
                case "title":
                    achievement.setTitle(value.replace("_", " "));
                    break;
                case "description":
                    achievement.setDescription(value.replace("_", " "));
                    break;
                case "icon":
                    achievement.setIcon(Material.valueOf(value.toUpperCase()));
                    break;
                case "target":
                    achievement.setTaskTarget(value);
                    break;
                case "amount":
                    achievement.setRequiredAmount(Integer.parseInt(value));
                    break;
                case "hidden":
                    achievement.setHidden(Boolean.parseBoolean(value));
                    break;
                case "rewards":
                    achievement.setRewards(Arrays.asList(value.split(",")));
                    break;
                default:
                    sender.sendMessage("§cInvalid property! Available: title, description, icon, target, amount, rewards, hidden");
                    return true;
            }
            
            plugin.getDatabaseManager().saveAchievement(achievement);
            sender.sendMessage("§aAchievement updated successfully!");
            
        } catch (Exception e) {
            sender.sendMessage("§cInvalid value for property " + property + ": " + e.getMessage());
        }
        
        return true;
    }
    
    private boolean handleAdminList(CommandSender sender, String[] args) {
        List<Achievement> achievements = new ArrayList<>(plugin.getAchievementManager().getAllAchievements());
        sender.sendMessage("§6All Achievements (" + achievements.size() + "):");
        
        for (Achievement achievement : achievements) {
            String hidden = achievement.isHidden() ? " §7(Hidden)" : "";
            sender.sendMessage("§7- §f" + achievement.getId() + " §7: §e" + achievement.getTitle() + hidden);
        }
        
        return true;
    }
    
    private boolean handleAdminInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /achadmin info <id>");
            return true;
        }
        
        String achievementId = args[1];
        Achievement achievement = plugin.getAchievementManager().getAchievement(achievementId);
        
        if (achievement == null) {
            if (sender instanceof Player) {
                plugin.getMessageManager().sendMessage((Player) sender, "invalid-achievement");
            } else {
                sender.sendMessage("§cAchievement not found!");
            }
            return true;
        }
        
        sender.sendMessage("§6=== Achievement Information ===");
        sender.sendMessage("§7ID: §f" + achievement.getId());
        sender.sendMessage("§7Title: §f" + achievement.getTitle());
        sender.sendMessage("§7Description: §f" + achievement.getDescription());
        sender.sendMessage("§7Icon: §f" + achievement.getIcon().name());
        sender.sendMessage("§7Task Type: §f" + achievement.getTaskType().getDisplayName());
        sender.sendMessage("§7Task Target: §f" + achievement.getTaskTarget());
        sender.sendMessage("§7Required Amount: §f" + achievement.getRequiredAmount());
        sender.sendMessage("§7Hidden: §f" + (achievement.isHidden() ? "Yes" : "No"));
        
        if (!achievement.getRewards().isEmpty()) {
            sender.sendMessage("§7Rewards: §f" + String.join(", ", achievement.getRewards()));
        }
        
        if (!achievement.getPrerequisites().isEmpty()) {
            sender.sendMessage("§7Prerequisites: §f" + String.join(", ", achievement.getPrerequisites()));
        }
        
        return true;
    }
    
    private boolean handleResetAll(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /achadmin resetall <player>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[1]);
            return true;
        }
        
        sender.sendMessage("§cAre you sure you want to reset ALL achievements for " + target.getName() + "?");
        sender.sendMessage("§cType '/achadmin resetall " + target.getName() + " confirm' to proceed.");
        
        if (args.length > 2 && args[2].equalsIgnoreCase("confirm")) {
            for (Achievement achievement : plugin.getAchievementManager().getAllAchievements()) {
                plugin.getProgressManager().resetPlayerProgress(target, achievement.getId());
            }
            
            sender.sendMessage("§aReset ALL achievements for §f" + target.getName());
            target.sendMessage("§cAll your achievement progress has been reset by an administrator.");
        }
        
        return true;
    }
    
    private boolean handleSetProgress(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /achadmin progress <player> <achievement_id> <amount>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[1]);
            return true;
        }
        
        String achievementId = args[2];
        Achievement achievement = plugin.getAchievementManager().getAchievement(achievementId);
        
        if (achievement == null) {
            if (sender instanceof Player) {
                plugin.getMessageManager().sendMessage((Player) sender, "invalid-achievement");
            } else {
                sender.sendMessage("§cAchievement not found!");
            }
            return true;
        }
        
        try {
            int amount = Integer.parseInt(args[3]);
            PlayerProgress progress = plugin.getProgressManager().getPlayerProgress(target, achievementId);
            progress.setProgress(amount);
            
            if (amount >= achievement.getRequiredAmount() && !progress.isUnlocked()) {
                progress.setUnlocked(true);
                plugin.getProgressManager().notifyAchievementUnlocked(target, achievement);
            }
            
            plugin.getDatabaseManager().savePlayerProgress(progress);
            sender.sendMessage("§aSet progress for §f" + achievement.getTitle() + " §ato §f" + amount + " §afor §f" + target.getName());
            
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid amount: " + args[3]);
        }
          return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("advancedachievements.admin")) {
            return new ArrayList<>();
        }
        
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList(
                "create", "delete", "give", "reset", "reload", "edit", 
                "list", "info", "resetall", "progress"
            );
            
            return subCommands.stream()
                .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "delete":
                case "edit":
                case "info":
                    return plugin.getAchievementManager().getAllAchievements().stream()
                        .map(Achievement::getId)
                        .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());

                case "give":
                case "reset":
                case "resetall":
                case "progress":
                    return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());

                case "create":
                    return Arrays.asList("<id>");
            }
        }
        
        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "give":
                case "reset":
                case "progress":
                    return plugin.getAchievementManager().getAllAchievements().stream()
                        .map(Achievement::getId)
                        .filter(id -> id.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());

                case "edit":
                    return Arrays.asList("title", "description", "icon", "target", "amount", "rewards", "hidden").stream()
                        .filter(prop -> prop.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());

                case "create":
                    return Arrays.asList("<title>");
            }
        }
        
        if (args.length == 4) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "create":
                    return Arrays.asList("<description>");
                case "progress":
                    return Arrays.asList("<amount>");
            }
        }
        
        if (args.length == 5) {
            String subCommand = args[0].toLowerCase();
            
            if ("create".equals(subCommand)) {
        
            }
        }
        
        if (args.length == 6) {
            String subCommand = args[0].toLowerCase();
            
            if ("create".equals(subCommand)) {
                return Arrays.stream(Material.values())
                    .map(Material::name)
                    .filter(name -> name.toLowerCase().startsWith(args[5].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
          if (args.length == 7) {
            String subCommand = args[0].toLowerCase();
            
            if ("create".equals(subCommand)) {
                return TaskType.getAvailableTypesList().stream()
                    .filter(type -> type.toLowerCase().startsWith(args[6].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        return completions;
    }
}