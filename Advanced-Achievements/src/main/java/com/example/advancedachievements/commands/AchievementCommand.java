package com.example.advancedachievements.commands;

import com.example.advancedachievements.AdvancedAchievements;
import com.example.advancedachievements.models.Achievement;
import com.example.advancedachievements.models.PlayerProgress;
import com.example.advancedachievements.enums.TaskType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class AchievementCommand implements CommandExecutor, TabCompleter {

    private final AdvancedAchievements plugin;
    private final Map<UUID, CreationSession> creationSessions = new HashMap<>();

    public AchievementCommand(AdvancedAchievements plugin) {
        this.plugin = plugin;
    }

    public static class CreationSession {
        public enum CreationStep {
            TASK_TYPE, TITLE, DESCRIPTION, ICON, TARGET, AMOUNT, REWARDS, CONFIRM
        }
        public TaskType taskType;
        public CreationStep step = CreationStep.TASK_TYPE;
        public String title;
        public String id;
        public String description;
        public Material icon;
        public String target;
        public int amount;
        public List<String> rewards = new ArrayList<>();
        public int points = 0;
        public boolean hidden = false;
        public CreationStep getNextStep(CreationStep current) {
            List<CreationStep> steps = new ArrayList<>();
            steps.add(CreationStep.TASK_TYPE);
            steps.add(CreationStep.TITLE);
            steps.add(CreationStep.DESCRIPTION);
            steps.add(CreationStep.ICON);
            if (taskType != TaskType.ENCHANTING && taskType != TaskType.TRADING) {
                steps.add(CreationStep.TARGET);
            }
            steps.add(CreationStep.AMOUNT);
            steps.add(CreationStep.REWARDS);
            int idx = steps.indexOf(current);
            if (idx == -1 || idx + 1 >= steps.size()) return null;
            return steps.get(idx + 1);
        }
    }

    private final Map<UUID, Integer> playerPages = new HashMap<>();

    private int getCurrentPage(Player player) {
        return playerPages.getOrDefault(player.getUniqueId(), 0);
    }

    private void setCurrentPage(Player player, int page) {
        playerPages.put(player.getUniqueId(), page);
    }

    private String generateAchievementId(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "untitled_achievement";
        }

        String slug = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");

        if (slug.isEmpty()) {
            slug = "untitled_achievement";
        }

        String baseSlug = slug;
        int counter = 1;
        while (plugin.getAchievementManager().getAchievement(slug) != null) {
            slug = baseSlug + "_" + counter;
            counter++;
        }

        return slug;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            return handleList(player, args);
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list":
                return handleList(player, args);
            case "progress":
                return handleProgress(player, args);
            case "info":
                return handleInfo(player, args);
            case "stats":
                return handleStats(player, args);
            case "gui":
                return handleGui(player, args);
            case "create":
                return handleCreate(player, args);
            case "delete":
                return handleDelete(player, args);
            case "give":
                return handleGive(player, args);
            case "reset":
                return handleReset(player, args);
            case "reload":
                return handleReload(player, args);
            case "edit":
                return handleEdit(player, args);
            case "help":
                return handleHelp(player, args);
            default:
                return handleList(player, args);
        }
    }

    private boolean handleList(Player player, String[] args) {
        String category = args.length > 1 && !args[1].matches("\\d+") ? args[1] : null;
        int page = 0;

        if (args.length > 1) {
            String lastArg = args[args.length - 1];
            if (lastArg.matches("\\d+")) {
                page = Integer.parseInt(lastArg) - 1;
                if (page < 0) page = 0;
            }
        }
        if (args.length > 1) {
            if (args[1].equals("<")) {
                page = Math.max(0, getCurrentPage(player) - 1);
            } else if (args[1].equals(">")) {
                page = getCurrentPage(player) + 1;
            } else if (args[1].matches("\\d+")) {
                page = Integer.parseInt(args[1]) - 1;
                if (page < 0) page = 0;
            }
        }

        List<Achievement> achievements;

        if (category != null) {
            achievements = plugin.getAchievementManager().getAchievementsByCategory(category);
        } else {
            achievements = new ArrayList<>(plugin.getAchievementManager().getAllAchievements());
        }

        achievements = achievements.stream()
                .filter(achievement -> !achievement.isHidden() ||
                        plugin.getProgressManager().getPlayerProgress(player, achievement.getId()).isUnlocked())
                .collect(Collectors.toList());
        int itemsPerPage = 5;
        int totalPages = (int) Math.ceil((double) achievements.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;
        if (page >= totalPages) page = totalPages - 1;

        setCurrentPage(player, page);

        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, achievements.size());

        player.sendMessage("§6Your Achievements" + (category != null ? " - " + category : "") +
                " §7(Page " + (page + 1) + "/" + totalPages + "):");
        player.sendMessage("§8" + "=".repeat(50));

        if (achievements.isEmpty()) {
            player.sendMessage("§7No achievements found.");
            return true;
        }

        for (int i = startIndex; i < endIndex; i++) {
            Achievement achievement = achievements.get(i);
            PlayerProgress progress = plugin.getProgressManager().getPlayerProgress(player, achievement.getId());

            String titleColor = progress.isUnlocked() ? "§a" : "§7";

            String mainLine = titleColor + achievement.getTitle() +
                    " (" + progress.getProgress() + "/" + achievement.getRequiredAmount() + ") - " +
                    achievement.getDescription();
            String rewardLine = "§6Reward: ";
            if (achievement.getRewards().isEmpty()) {
                rewardLine += "-";
            } else {
                List<String> formattedRewards = new ArrayList<>();
                for (String reward : achievement.getRewards()) {
                    formattedRewards.add(formatRewardCompact(reward));
                }
                rewardLine += String.join(" + ", formattedRewards);
            }

            player.sendMessage(mainLine);
            player.sendMessage(rewardLine);

            if (i < endIndex - 1) {
                player.sendMessage("§8" + "-".repeat(30));
            }
        }

        player.sendMessage("§8" + "=".repeat(50));
        if (totalPages > 1) {
            String nav = "§7Navigation: ";
            if (page > 0) {
                nav += "§e/achievementadmin list " + (category != null ? category + " " : "") + "< §7(Previous) ";
            }
            nav += "§f" + (page + 1) + "/" + totalPages + " ";
            if (page < totalPages - 1) {
                nav += "§e/achievementadmin list " + (category != null ? category + " " : "") + "> §7(Next)";
            }
            player.sendMessage(nav);

            if (page < totalPages - 1) {
                player.sendMessage("§7Use §e/achievementadmin list " + (page + 2) + " §7for the next 5 achievements");
            }
        }

        return true;
    }
    private boolean handleProgress(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /achievementadmin progress <achievement_id>");
            return true;
        }

        String achievementId = args[1];
        Achievement achievement = plugin.getAchievementManager().getAchievement(achievementId);

        if (achievement == null) {
            plugin.getMessageManager().sendMessage(player, "invalid-achievement");
            return true;
        }

        if (achievement.isHidden() && !plugin.getProgressManager().getPlayerProgress(player, achievementId).isUnlocked()) {
            plugin.getMessageManager().sendMessage(player, "invalid-achievement");
            return true;
        }

        PlayerProgress progress = plugin.getProgressManager().getPlayerProgress(player, achievementId);

        player.sendMessage("§6Achievement Progress:");
        player.sendMessage("§7Title: §f" + achievement.getTitle());
        player.sendMessage("§7Description: §f" + achievement.getDescription());
        player.sendMessage("§7Progress: §f" + progress.getProgress() + "/" + achievement.getRequiredAmount());
        if (progress.isUnlocked()) {
            player.sendMessage("§7Status: §aUnlocked ✓");
            if (progress.getUnlockedDate() != 0) {
                player.sendMessage("§7Unlocked: §f" + new java.util.Date(progress.getUnlockedDate()));
            }
        } else {
            double percentage = (double) progress.getProgress() / achievement.getRequiredAmount() * 100;
            player.sendMessage("§7Status: §e" + String.format("%.1f", percentage) + "% Complete");
        }

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /achievementadmin info <achievement_id>");
            return true;
        }

        String achievementId = args[1];
        Achievement achievement = plugin.getAchievementManager().getAchievement(achievementId);

        if (achievement == null) {
            plugin.getMessageManager().sendMessage(player, "invalid-achievement");
            return true;
        }

        if (achievement.isHidden() && !plugin.getProgressManager().getPlayerProgress(player, achievementId).isUnlocked()) {
            plugin.getMessageManager().sendMessage(player, "invalid-achievement");
            return true;
        }

        PlayerProgress progress = plugin.getProgressManager().getPlayerProgress(player, achievementId);

        player.sendMessage("§6=== Achievement Information ===");
        player.sendMessage("§7ID: §f" + achievement.getId());
        player.sendMessage("§7Title: §f" + achievement.getTitle());
        player.sendMessage("§7Description: §f" + achievement.getDescription());
        player.sendMessage("§7Task: §f" + achievement.getTaskType().getDisplayName());
        player.sendMessage("§7Required: §f" + achievement.getRequiredAmount());
        player.sendMessage("§7Points: §f" + achievement.getPoints());
        if (progress.isUnlocked()) {
            player.sendMessage("§7Status: §aUnlocked ✓");
            if (progress.getUnlockedDate() != 0) {
                player.sendMessage("§7Unlocked: §f" + new java.util.Date(progress.getUnlockedDate()));
            }
        } else {
            player.sendMessage("§7Your Progress: §f" + progress.getProgress() + "/" + achievement.getRequiredAmount());
        }

        if (!achievement.getRewards().isEmpty()) {
            player.sendMessage("§7Rewards: §f" + String.join(", ", achievement.getRewards()));
        }

        return true;
    }

    private boolean handleStats(Player player, String[] args) {
        List<Achievement> allAchievements = new ArrayList<>(plugin.getAchievementManager().getAllAchievements());
        int totalAchievements = allAchievements.size();
        int unlockedCount = 0;
        int totalPoints = 0;

        for (Achievement achievement : allAchievements) {
            PlayerProgress progress = plugin.getProgressManager().getPlayerProgress(player, achievement.getId());
            if (progress.isUnlocked()) {
                unlockedCount++;
                totalPoints += achievement.getPoints();
            }
        }

        double percentage = totalAchievements > 0 ? (double) unlockedCount / totalAchievements * 100 : 0;

        player.sendMessage("§6=== Achievement Statistics ===");
        player.sendMessage("§7Unlocked: §f" + unlockedCount + "/" + totalAchievements + " §7(" + String.format("%.1f", percentage) + "%)");
        player.sendMessage("§7Total Points: §f" + totalPoints);

        return true;
    }

    private boolean handleGui(Player player, String[] args) {
        String category = args.length > 1 ? args[1] : null;
        plugin.getAchievementGUI().openGUI(player, category, 0);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        Player player = (Player) sender;
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("list", "progress", "info", "stats", "gui", "help");

            if (player.hasPermission("advancedachievements.admin")) {
                subCommands = Arrays.asList("list", "progress", "info", "stats", "gui", "create",
                        "delete", "give", "reset", "reload", "edit", "help");
            }

            return subCommands.stream()
                    .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        CreationSession session = creationSessions.get(player.getUniqueId());
        if (session != null && args[0].equalsIgnoreCase("create")) {
            switch (session.step) {
                case TASK_TYPE:
                    TaskType[] workingTypes = {
                            TaskType.BLOCK_BREAK, TaskType.ITEM_PICKUP, TaskType.MOB_KILL,
                            TaskType.ITEM_CRAFT, TaskType.FISHING,
                            TaskType.EATING, TaskType.ENCHANTING, TaskType.TRADING,
                            TaskType.MINING, TaskType.BREEDING, TaskType.TAMING, TaskType.DEATH
                    };

                    return Arrays.stream(workingTypes)
                            .map(TaskType::getId)
                            .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());

                case ICON:
                    return Arrays.stream(Material.values())
                            .map(Material::name)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .limit(20)
                            .collect(Collectors.toList());

                case CONFIRM:
                    return Arrays.asList("confirm", "cancel").stream()
                            .filter(cmd -> cmd.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
            }
            return new ArrayList<>();
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "progress":
                case "info":
                case "delete":
                case "edit":
                    return plugin.getAchievementManager().getAllAchievements().stream()
                            .filter(achievement -> !achievement.isHidden() ||
                                    plugin.getProgressManager().getPlayerProgress(player, achievement.getId()).isUnlocked())
                            .map(Achievement::getId)
                            .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());

                case "list":
                case "gui":
                    return plugin.getAchievementManager().getCategories().stream()
                            .filter(cat -> cat.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());

                case "give":
                case "reset":
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "give":
                case "reset":
                    return plugin.getAchievementManager().getAllAchievements().stream()
                            .map(Achievement::getId)
                            .filter(id -> id.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());

                case "edit":
                    return Arrays.asList("title", "description", "category", "icon", "target",
                                    "amount", "rewards", "points", "hidden").stream()
                            .filter(prop -> prop.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
            }
        }

        return completions;
    }

    private String formatReward(String reward) {
        String[] parts = reward.split(":");
        if (parts.length < 2) return reward;

        String type = parts[0].toUpperCase();
        String value = parts[1];
        String amount = parts.length > 2 ? parts[2] : "1";

        switch (type) {
            case "ITEM":
                String materialName = value.replace("_", " ");
                String[] words = materialName.split(" ");
                StringBuilder formattedName = new StringBuilder();
                for (String word : words) {
                    if (formattedName.length() > 0) formattedName.append(" ");
                    formattedName.append(Character.toUpperCase(word.charAt(0)))
                            .append(word.substring(1).toLowerCase());
                }
                return amount + "x " + formattedName.toString();
            case "XP":
                return value + " XP";
            case "MONEY":
                return "$" + value;
            case "COMMAND":
                return "Special Reward";
            default:
                return reward;
        }
    }

    private String formatMaterialName(String materialName) {
        if (materialName.equalsIgnoreCase("ANY") || materialName.equalsIgnoreCase("HOSTILE")) {
            return materialName.substring(0, 1).toUpperCase() + materialName.substring(1).toLowerCase();
        }

        String formatted = materialName.replace("_", " ");
        String[] words = formatted.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0) result.append(" ");
            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase());
        }
        return result.toString();
    }

    private boolean handleCreate(Player player, String[] args) {
        if (!player.hasPermission("advancedachievements.admin")) {
            plugin.getMessageManager().sendMessage(player, "no-permission");
            return true;
        }

        CreationSession session = creationSessions.get(player.getUniqueId());
        if (session == null) {
            session = new CreationSession();
            creationSessions.put(player.getUniqueId(), session);
            player.sendMessage("§6=== Achievement Creation Started ===");
            player.sendMessage("§7The achievement ID will be generated from the title.");
            player.sendMessage("§7Only the essential steps will be shown!");
            player.sendMessage("");
            player.sendMessage("§7Step 1: Choose a task type");
            player.sendMessage("§7Available types:");
            player.sendMessage("");
            TaskType[] workingTypes = {
                    TaskType.BLOCK_BREAK, TaskType.ITEM_PICKUP, TaskType.MOB_KILL,
                    TaskType.ITEM_CRAFT, TaskType.FISHING,
                    TaskType.EATING, TaskType.ENCHANTING, TaskType.TRADING,
                    TaskType.MINING, TaskType.BREEDING, TaskType.TAMING, TaskType.DEATH
            };

            for (TaskType type : workingTypes) {
                player.sendMessage("§f- §e" + type.getId() + " §7(" + type.getDisplayName() + ")");
            }

            player.sendMessage("");
            player.sendMessage("§aSimply type the task ID in chat!");
            player.sendMessage("§7Example: §fblock_break");
            player.sendMessage("§cTo cancel type: §fcancel");
            return true;
        } else {
            showCurrentStepHelp(player, session);
            return true;
        }
    }

    private boolean handleCreationStep(Player player, CreationSession session, String input) {
        if (input.equalsIgnoreCase("cancel")) {
            creationSessions.remove(player.getUniqueId());
            player.sendMessage("§cAchievement creation cancelled.");
            return true;
        }

        switch (session.step) {
            case TASK_TYPE:
                TaskType taskType = TaskType.fromId(input);
                TaskType[] workingTypes = {
                        TaskType.BLOCK_BREAK, TaskType.ITEM_PICKUP, TaskType.MOB_KILL,
                        TaskType.ITEM_CRAFT, TaskType.FISHING,
                        TaskType.EATING, TaskType.ENCHANTING, TaskType.TRADING,
                        TaskType.MINING, TaskType.BREEDING, TaskType.TAMING, TaskType.DEATH
                };

                boolean isWorkingType = false;
                for (TaskType workingType : workingTypes) {
                    if (workingType == taskType) {
                        isWorkingType = true;
                        break;
                    }
                }

                if (taskType == null || !isWorkingType) {
                    player.sendMessage("§cInvalid task type! Available types:");
                    player.sendMessage("");
                    for (TaskType type : workingTypes) {
                        player.sendMessage("§f- §e" + type.getId() + " §7(" + type.getDisplayName() + ")");
                    }
                    player.sendMessage("");
                    player.sendMessage("§aType the task ID in chat!");
                    return true;
                }
                session.taskType = taskType;
                session.step = session.getNextStep(CreationSession.CreationStep.TASK_TYPE);
                player.sendMessage("§aTask type set: " + taskType.getDisplayName());
                player.sendMessage("");

                showCurrentStepHelp(player, session);
                return true;
            case TITLE:
                session.title = input;
                session.id = generateAchievementId(input);
                player.sendMessage("§aTitle set: " + input);
                player.sendMessage("§7Achievement ID: §f" + session.id);
                player.sendMessage("");
                if (checkAndAutoCreate(player, session)) return true;
                showCurrentStepHelp(player, session);
                break;
            case DESCRIPTION:
                session.description = input;
                player.sendMessage("§aDescription set: " + input);
                player.sendMessage("");
                if (checkAndAutoCreate(player, session)) return true;
                showCurrentStepHelp(player, session);
                break;
            case ICON:
                try {
                    session.icon = Material.valueOf(input.toUpperCase());
                    player.sendMessage("§aIcon set: " + formatMaterialName(session.icon.name()));
                    player.sendMessage("");
                    if (checkAndAutoCreate(player, session)) return true;
                    showCurrentStepHelp(player, session);
                } catch (IllegalArgumentException e) {
                    player.sendMessage("§cInvalid material name! Please enter a valid Minecraft item name.");
                    player.sendMessage("§7Example: §f" + getStepExample(session.taskType, CreationSession.CreationStep.ICON));
                }
                break;
            case TARGET:
                boolean validTarget = false;
                if (session.taskType == TaskType.BLOCK_BREAK || session.taskType == TaskType.MINING || session.taskType == TaskType.ITEM_CRAFT) {
                    try {
                        Material.valueOf(input.toUpperCase());
                        session.target = input.toLowerCase();
                        validTarget = true;
                    } catch (IllegalArgumentException e) {
                        player.sendMessage("§cInvalid block/item name! Please enter a valid Minecraft material.");
                        player.sendMessage("§7Example: §f" + getStepExample(session.taskType, CreationSession.CreationStep.TARGET));
                    }
                } else if (session.taskType == TaskType.MOB_KILL || session.taskType == TaskType.BREEDING || session.taskType == TaskType.TAMING) {
                    try {
                        org.bukkit.entity.EntityType.valueOf(input.toUpperCase());
                        session.target = input.toLowerCase();
                        validTarget = true;
                    } catch (IllegalArgumentException e) {
                        player.sendMessage("§cInvalid entity name! Please enter a valid Minecraft entity.");
                        player.sendMessage("§7Example: §f" + getStepExample(session.taskType, CreationSession.CreationStep.TARGET));
                    }
                } else if (session.taskType == TaskType.FISHING) {
                    try {
                        Material.valueOf(input.toUpperCase());
                        session.target = input.toLowerCase();
                        validTarget = true;
                    } catch (IllegalArgumentException e) {
                        player.sendMessage("§cInvalid fish/item name! Please enter a valid Minecraft material.");
                        player.sendMessage("§7Example: §f" + getStepExample(session.taskType, CreationSession.CreationStep.TARGET));
                    }
                } else {
                    session.target = input;
                    validTarget = true;
                }
                if (validTarget) {
                    player.sendMessage("§aTarget set: " + formatMaterialName(input));
                    player.sendMessage("");
                    session.step = session.getNextStep(CreationSession.CreationStep.TARGET);
                    showCurrentStepHelp(player, session);
                }
                break;
            case AMOUNT:
                try {
                    session.amount = Integer.parseInt(input);
                    if (session.amount <= 0) {
                        player.sendMessage("§cAmount must be greater than 0!");
                        player.sendMessage("§7Example: §f" + getStepExample(session.taskType, CreationSession.CreationStep.AMOUNT));
                        return true;
                    }
                    player.sendMessage("§aAmount set: " + session.amount);
                    player.sendMessage("");
                    if (checkAndAutoCreate(player, session)) return true;
                    showCurrentStepHelp(player, session);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cPlease enter a valid number!");
                    player.sendMessage("§7Example: §f" + getStepExample(session.taskType, CreationSession.CreationStep.AMOUNT));
                }
                break;
            case REWARDS:
                if (!input.equalsIgnoreCase("skip")) {
                    List<String> rewards = Arrays.asList(input.split(","));
                    List<String> validRewards = new ArrayList<>();
                    boolean invalid = false;
                    for (String reward : rewards) {
                        String trimmed = reward.trim().replaceAll("\\s+", "");
                        String[] parts = trimmed.split(":");
                        if (parts.length != 2) {
                            invalid = true;
                            break;
                        }
                        String type = parts[0].toLowerCase();
                        String value = parts[1];
                        boolean typeValid = false;
                        if (type.equals("experience") || type.equals("money") || type.equals("command")) {
                            typeValid = true;
                        } else {
                            try {
                                Material.valueOf(type.toUpperCase());
                                typeValid = true;
                            } catch (IllegalArgumentException ignored) {}
                        }
                        if (!typeValid) {
                            invalid = true;
                            break;
                        }
                        if (!value.matches("\\d+")) {
                            invalid = true;
                            break;
                        }
                        validRewards.add(type + ":" + value);
                    }
                    if (invalid) {
                        player.sendMessage("§cInvalid rewards format! Use: diamond:3,experience:30 (no spaces, allowed types: any item/block, experience, money, command)");
                        player.sendMessage("§7Example: §fdiamond:3,experience:30");
                        return true;
                    }
                    session.rewards = validRewards;
                }
                player.sendMessage("§aRewards set!");
                player.sendMessage("");
                player.sendMessage("§6Creating your achievement...");
                showCreationSummary(player, session);
                player.sendMessage("");
                createAchievement(player, session);
                creationSessions.remove(player.getUniqueId());
                break;
        }
        return true;
    }

    private void showCreationSummary(Player player, CreationSession session) {
        player.sendMessage("§6=== Achievement Summary ===");
        player.sendMessage("§7ID: §f" + session.id);
        player.sendMessage("§7Title: §f" + session.title);
        player.sendMessage("§7Description: §f" + session.description);
        player.sendMessage("§7Icon: §f" + formatMaterialName(session.icon.name()));
        player.sendMessage("§7Task: §f" + session.taskType.getDisplayName());
        if (session.target != null) {
            player.sendMessage("§7Target: §f" + formatMaterialName(session.target));
        }
        player.sendMessage("§7Amount: §f" + session.amount);
        player.sendMessage("§7Points: §f" + session.points);
        if (session.rewards != null && !session.rewards.isEmpty()) {
            List<String> formatted = new ArrayList<>();
            for (String reward : session.rewards) {
                if (!reward.trim().isEmpty()) {
                    formatted.add(formatRewardCompact(reward));
                }
            }
            if (!formatted.isEmpty()) {
                player.sendMessage("§7Rewards: §f" + String.join(", ", formatted));
            }
        }
    }

    private void createAchievement(Player player, CreationSession session) {
        if (session.description == null || session.description.trim().isEmpty()) {
            session.description = "Complete this achievement to unlock rewards!";
        }
        if (session.icon == null) {
            switch (session.taskType) {
                case MOB_KILL:
                    session.icon = Material.DIAMOND_SWORD;
                    break;
                case BLOCK_BREAK:
                    session.icon = Material.DIAMOND_PICKAXE;
                    break;
                case EATING:
                    session.icon = Material.APPLE;
                    break;
                case ITEM_CRAFT:
                    session.icon = Material.CRAFTING_TABLE;
                    break;
                case FISHING:
                    session.icon = Material.FISHING_ROD;
                    break;
                case MINING:
                    session.icon = Material.IRON_PICKAXE;
                    break;
                case ITEM_PICKUP:
                    session.icon = Material.CHEST;
                    break;
                case ENCHANTING:
                    session.icon = Material.ENCHANTING_TABLE;
                    break;
                case TRADING:
                    session.icon = Material.EMERALD;
                    break;
                case BREEDING:
                    session.icon = Material.WHEAT;
                    break;
                case TAMING:
                    session.icon = Material.BONE;
                    break;
                case DEATH:
                    session.icon = Material.SKELETON_SKULL;
                    break;
                default:
                    session.icon = Material.EMERALD;
            }
        }
        if (session.target == null || session.target.trim().isEmpty()) {
            session.target = "ANY";
        }
        if (session.amount <= 0) {
            session.amount = 1;
        }

        Achievement achievement = plugin.getAchievementManager().createAchievement(
                session.id, session.title, session.description, null,
                session.icon, session.taskType, session.target, session.amount,
                session.rewards, session.points, session.hidden, new ArrayList<>()
        );

        player.sendMessage("§a=== Achievement created successfully! ===");
        player.sendMessage("§aCreated: §f" + achievement.getTitle() + " §7(" + session.id + ")");
        player.sendMessage("§7The achievement is now available for all players!");
    }

    private boolean handleDelete(Player player, String[] args) {
        if (!player.hasPermission("advancedachievements.admin")) {
            plugin.getMessageManager().sendMessage(player, "no-permission");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /achievementadmin delete <id>");
            return true;
        }

        String id = args[1];
        Achievement achievement = plugin.getAchievementManager().getAchievement(id);

        if (achievement == null) {
            plugin.getMessageManager().sendMessage(player, "invalid-achievement");
            return true;
        }

        if (plugin.getAchievementManager().deleteAchievement(id)) {
            player.sendMessage("§aDeleted achievement: §f" + achievement.getTitle() + " §7(" + id + ")");
        } else {
            player.sendMessage("§cFailed to delete achievement!");
        }

        return true;
    }

    private boolean handleGive(Player player, String[] args) {
        if (!player.hasPermission("advancedachievements.admin")) {
            plugin.getMessageManager().sendMessage(player, "no-permission");
            return true;
        }

        if (args.length < 3) {
            player.sendMessage("§cUsage: /achievementadmin give <player> <achievement_id>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found: " + args[1]);
            return true;
        }

        String achievementId = args[2];
        Achievement achievement = plugin.getAchievementManager().getAchievement(achievementId);

        if (achievement == null) {
            plugin.getMessageManager().sendMessage(player, "invalid-achievement");
            return true;
        }

        plugin.getProgressManager().giveAchievement(target, achievementId);
        player.sendMessage("§aGave achievement §f" + achievement.getTitle() + " §ato §f" + target.getName());

        return true;
    }

    private boolean handleReset(Player player, String[] args) {
        if (!player.hasPermission("advancedachievements.admin")) {
            plugin.getMessageManager().sendMessage(player, "no-permission");
            return true;
        }

        if (args.length < 3) {
            player.sendMessage("§cUsage: /achievementadmin reset <player> <achievement_id>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found: " + args[1]);
            return true;
        }

        String achievementId = args[2];
        Achievement achievement = plugin.getAchievementManager().getAchievement(achievementId);

        if (achievement == null) {
            plugin.getMessageManager().sendMessage(player, "invalid-achievement");
            return true;
        }

        plugin.getProgressManager().resetPlayerProgress(target, achievementId);
        player.sendMessage("§aReset achievement §f" + achievement.getTitle() + " §afor §f" + target.getName());

        return true;
    }

    private boolean handleReload(Player player, String[] args) {
        if (!player.hasPermission("advancedachievements.admin")) {
            plugin.getMessageManager().sendMessage(player, "no-permission");
            return true;
        }

        plugin.getConfigManager().reloadConfigs();
        plugin.getAchievementManager().reloadAchievements();
        player.sendMessage("§aAdvanced Achievements reloaded!");
        return true;
    }

    private boolean handleEdit(Player player, String[] args) {
        if (!player.hasPermission("advancedachievements.admin")) {
            plugin.getMessageManager().sendMessage(player, "no-permission");
            return true;
        }

        if (args.length < 4) {
            player.sendMessage("§cUsage: /achievementadmin edit <id> <property> <value>");
            player.sendMessage("§7Properties: title, description, category, icon, target, amount, rewards, points, hidden");
            return true;
        }

        String id = args[1];
        Achievement achievement = plugin.getAchievementManager().getAchievement(id);

        if (achievement == null) {
            plugin.getMessageManager().sendMessage(player, "invalid-achievement");
            return true;
        }

        String property = args[2].toLowerCase();
        String value = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

        try {
            switch (property) {
                case "title":
                    achievement.setTitle(value);
                    break;
                case "description":
                    achievement.setDescription(value);
                    break;
                case "category":
                    achievement.setCategory(value);
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
                case "points":
                    achievement.setPoints(Integer.parseInt(value));
                    break;
                case "hidden":
                    achievement.setHidden(Boolean.parseBoolean(value));
                    break;
                case "rewards":
                    achievement.setRewards(Arrays.asList(value.split(",")));
                    break;
                default:
                    player.sendMessage("§cInvalid property! Available: title, description, category, icon, target, amount, rewards, points, hidden");
                    return true;
            }

            plugin.getDatabaseManager().saveAchievement(achievement);
            player.sendMessage("§aAchievement updated successfully!");

        } catch (Exception e) {
            player.sendMessage("§cInvalid value for property " + property + ": " + e.getMessage());
        }

        return true;
    }

    private boolean handleHelp(Player player, String[] args) {
        player.sendMessage("§6=== Achievement Admin Commands ===");
        player.sendMessage("§7/achievementadmin §f- Show your achievements");
        player.sendMessage("§7/achievementadmin list [category] [page] §f- List achievements");
        player.sendMessage("§7/achievementadmin list < §f- Previous page");
        player.sendMessage("§7/achievementadmin list > §f- Next page");
        player.sendMessage("§7/achievementadmin progress <id> §f- Show achievement progress");
        player.sendMessage("§7/achievementadmin info <id> §f- Show achievement details");
        player.sendMessage("§7/achievementadmin stats §f- Show your statistics");
        player.sendMessage("§7/achievementadmin gui [category] §f- Open achievement GUI");

        if (player.hasPermission("advancedachievements.admin")) {
            player.sendMessage("");
            player.sendMessage("§6=== Admin Commands ===");
            player.sendMessage("§7/achievementadmin create §f- Create new achievement (interactive)");
            player.sendMessage("§7/achievementadmin delete <id> §f- Delete achievement");
            player.sendMessage("§7/achievementadmin edit <id> <property> <value> §f- Edit achievement");
            player.sendMessage("§7/achievementadmin give <player> <id> §f- Give achievement to player");
            player.sendMessage("§7/achievementadmin reset <player> <id> §f- Reset player's progress");
            player.sendMessage("§7/achievementadmin reload §f- Reload plugin");
        }

        return true;
    }

    private void showCurrentStepHelp(Player player, CreationSession session) {
        switch (session.step) {
            case TASK_TYPE:
                player.sendMessage("§7Step 1: Choose a task type");
                player.sendMessage("§7Example: §fblock_break");
                break;
            case TITLE:
                player.sendMessage("§7Step 2: Set a title");
                player.sendMessage("§7Example: §f" + getStepExample(session.taskType, CreationSession.CreationStep.TITLE));
                break;
            case DESCRIPTION:
                player.sendMessage("§7Step 3: Set a description");
                player.sendMessage("§7Example: §f" + getStepExample(session.taskType, CreationSession.CreationStep.DESCRIPTION));
                break;
            case ICON:
                player.sendMessage("§7Step 4: Set an icon");
                player.sendMessage("§7Example: §f" + getStepExample(session.taskType, CreationSession.CreationStep.ICON));
                break;
            case TARGET:
                String targetStep = "Step 5: Set the target";
                if (session.taskType != null) {
                    switch (session.taskType) {
                        case BLOCK_BREAK:
                            targetStep = "Step 5: Which block should be broken?";
                            break;
                        case ITEM_PICKUP:
                            targetStep = "Step 5: Which item should be picked up?";
                            break;
                        case MOB_KILL:
                            targetStep = "Step 5: Which mob should be killed?";
                            break;
                        case ITEM_CRAFT:
                            targetStep = "Step 5: Which item should be crafted?";
                            break;
                        case FISHING:
                            targetStep = "Step 5: Which fish or item should be caught?";
                            break;
                        case EATING:
                            targetStep = "Step 5: Which food should be eaten?";
                            break;
                        case ENCHANTING:
                            targetStep = "Step 5: Target is not required for enchanting achievements.";
                            break;
                        case TRADING:
                            targetStep = "Step 5: Target is not required for trading achievements.";
                            break;
                        case MINING:
                            targetStep = "Step 5: Which ore or block should be mined?";
                            break;
                        case BREEDING:
                            targetStep = "Step 5: Which animal should be bred?";
                            break;
                        case TAMING:
                            targetStep = "Step 5: Which animal should be tamed?";
                            break;
                        case DEATH:
                            targetStep = "Step 5: For which player should this achievement count?";
                            break;
                        default:
                            targetStep = "Step 5: Set the target";
                    }
                }
                player.sendMessage("§7" + targetStep);
                player.sendMessage("§7Example: §f" + getStepExample(session.taskType, CreationSession.CreationStep.TARGET));
                break;
            case AMOUNT:
                player.sendMessage("§7Step 6: Set the amount");
                player.sendMessage("§7Example: §f" + getStepExample(session.taskType, CreationSession.CreationStep.AMOUNT));
                break;
            case REWARDS:
                player.sendMessage("§7Step 7: Set rewards (or skip)");
                player.sendMessage("§7Example: §fdiamond:3,experience:30");
                break;
            case CONFIRM:
                player.sendMessage("§7Type 'confirm' to create or 'cancel' to abort");
                break;
        }
    }

    private String capitalizeExample(String input) {
        if (input == null || input.isEmpty()) return input;
        return formatMaterialName(input);
    }

    public boolean hasActiveCreationSession(UUID playerId) {
        return creationSessions.containsKey(playerId);
    }

    public void handleChatInput(Player player, String input) {
        CreationSession session = creationSessions.get(player.getUniqueId());
        if (session != null) {
            handleCreationStep(player, session, input);
        }
    }

    private void showTargetStep(Player player, CreationSession session) {
        if (session.taskType == TaskType.ENCHANTING || session.taskType == TaskType.TRADING) {
            session.target = "ANY";
            session.step = session.getNextStep(CreationSession.CreationStep.TARGET);
            showAmountStep(player, session);
            return;
        }
        player.sendMessage("§7Step: Choose what exactly needs to be " + session.taskType.getDisplayName().toLowerCase());
        player.sendMessage("");
        switch (session.taskType) {
            case DEATH:
                player.sendMessage("§7Type the player name for whom the achievement should count (e.g., Max)");
                player.sendMessage("§7Example: §f" + getStepExample(session.taskType, CreationSession.CreationStep.TARGET));
                break;

        }
        player.sendMessage("");
        player.sendMessage("§7Can't find what you need? Type the exact name (e.g., ZOMBIE, STONE, DIAMOND)");
    }

    private void showAmountStep(Player player, CreationSession session) {
        player.sendMessage("§7Step: Set the required amount to complete this achievement");
        player.sendMessage("");

        if (session.target != null) {
            String targetName = formatMaterialName(session.target).toLowerCase();
            if (targetName.equals("any")) {
                targetName = "times";
            }
            player.sendMessage("§7How many " + targetName + " must the player " + session.taskType.getDisplayName().toLowerCase() + "?");
        } else {
            player.sendMessage("§7How many times must the player complete this task?");
        }

        player.sendMessage("");
        player.sendMessage("§aType a number!");
    }

    private String formatRewardCompact(String reward) {
        String[] parts = reward.split(":");
        if (parts.length < 2) return reward;

        String type = parts[0].trim().toLowerCase();
        String value = parts[1].trim();
        String amount = parts.length > 2 ? parts[2].trim() : value;

        switch (type) {
            case "item":
                return amount + "x " + formatMaterialName(value);
            case "experience":
            case "xp":
                return amount + " XP";
            case "money":
                return "$" + amount;
            case "command":
                return "Special Reward";
            default:
                if (value.matches("\\d+") && type.matches("[a-z_]+")) {
                    if (type.equals("experience")) {
                        return value + " XP";
                    } else {
                        return value + "x " + formatMaterialName(type);
                    }
                }
                return reward;
        }
    }

    private boolean checkAndAutoCreate(Player player, CreationSession session) {
        CreationSession.CreationStep nextStep = session.getNextStep(session.step);
        if (nextStep == null) {
            player.sendMessage("");
            player.sendMessage("§6Creating your achievement...");
            showCreationSummary(player, session);
            player.sendMessage("");
            createAchievement(player, session);
            creationSessions.remove(player.getUniqueId());
            return true;
        } else {
            session.step = nextStep;
            return false;
        }
    }

    private static final Map<TaskType, Map<CreationSession.CreationStep, String>> STEP_EXAMPLES = new HashMap<>();
    static {
        String rewardsExample = "diamond:3,experience:30";
        putStepExample(TaskType.BLOCK_BREAK, CreationSession.CreationStep.TITLE, "Stone Crusher");
        putStepExample(TaskType.BLOCK_BREAK, CreationSession.CreationStep.DESCRIPTION, "Break 100 stone blocks");
        putStepExample(TaskType.BLOCK_BREAK, CreationSession.CreationStep.ICON, "stone");
        putStepExample(TaskType.BLOCK_BREAK, CreationSession.CreationStep.TARGET, "stone");
        putStepExample(TaskType.BLOCK_BREAK, CreationSession.CreationStep.AMOUNT, "100");
        putStepExample(TaskType.BLOCK_BREAK, CreationSession.CreationStep.REWARDS, rewardsExample);
        putStepExample(TaskType.MOB_KILL, CreationSession.CreationStep.TITLE, "Zombie Slayer");
        putStepExample(TaskType.MOB_KILL, CreationSession.CreationStep.DESCRIPTION, "Kill 10 zombies");
        putStepExample(TaskType.MOB_KILL, CreationSession.CreationStep.ICON, "iron_sword");
        putStepExample(TaskType.MOB_KILL, CreationSession.CreationStep.TARGET, "zombie");
        putStepExample(TaskType.MOB_KILL, CreationSession.CreationStep.AMOUNT, "10");
        putStepExample(TaskType.MOB_KILL, CreationSession.CreationStep.REWARDS, rewardsExample);
        putStepExample(TaskType.ITEM_PICKUP, CreationSession.CreationStep.TITLE, "Diamond Collector");
        putStepExample(TaskType.ITEM_PICKUP, CreationSession.CreationStep.DESCRIPTION, "Pick up 5 diamonds");
        putStepExample(TaskType.ITEM_PICKUP, CreationSession.CreationStep.ICON, "diamond");
        putStepExample(TaskType.ITEM_PICKUP, CreationSession.CreationStep.TARGET, "diamond");
        putStepExample(TaskType.ITEM_PICKUP, CreationSession.CreationStep.AMOUNT, "5");
        putStepExample(TaskType.ITEM_PICKUP, CreationSession.CreationStep.REWARDS, rewardsExample);
        putStepExample(TaskType.ITEM_CRAFT, CreationSession.CreationStep.TITLE, "Master Crafter");
        putStepExample(TaskType.ITEM_CRAFT, CreationSession.CreationStep.DESCRIPTION, "Craft 3 diamond swords");
        putStepExample(TaskType.ITEM_CRAFT, CreationSession.CreationStep.ICON, "crafting_table");
        putStepExample(TaskType.ITEM_CRAFT, CreationSession.CreationStep.TARGET, "diamond sword");
        putStepExample(TaskType.ITEM_CRAFT, CreationSession.CreationStep.AMOUNT, "3");
        putStepExample(TaskType.ITEM_CRAFT, CreationSession.CreationStep.REWARDS, rewardsExample);
        putStepExample(TaskType.FISHING, CreationSession.CreationStep.TITLE, "Fisherman");
        putStepExample(TaskType.FISHING, CreationSession.CreationStep.DESCRIPTION, "Catch 10 fish");
        putStepExample(TaskType.FISHING, CreationSession.CreationStep.ICON, "fishing_rod");
        putStepExample(TaskType.FISHING, CreationSession.CreationStep.TARGET, "cod");
        putStepExample(TaskType.FISHING, CreationSession.CreationStep.AMOUNT, "10");
        putStepExample(TaskType.FISHING, CreationSession.CreationStep.REWARDS, rewardsExample);
        putStepExample(TaskType.EATING, CreationSession.CreationStep.TITLE, "Apple Eater");
        putStepExample(TaskType.EATING, CreationSession.CreationStep.DESCRIPTION, "Eat 5 apples");
        putStepExample(TaskType.EATING, CreationSession.CreationStep.ICON, "apple");
        putStepExample(TaskType.EATING, CreationSession.CreationStep.TARGET, "apple");
        putStepExample(TaskType.EATING, CreationSession.CreationStep.AMOUNT, "5");
        putStepExample(TaskType.EATING, CreationSession.CreationStep.REWARDS, rewardsExample);
        putStepExample(TaskType.ENCHANTING, CreationSession.CreationStep.TITLE, "Enchanter");
        putStepExample(TaskType.ENCHANTING, CreationSession.CreationStep.DESCRIPTION, "Enchant 2 items");
        putStepExample(TaskType.ENCHANTING, CreationSession.CreationStep.ICON, "enchanting_table");
        putStepExample(TaskType.ENCHANTING, CreationSession.CreationStep.TARGET, "any");
        putStepExample(TaskType.ENCHANTING, CreationSession.CreationStep.AMOUNT, "2");
        putStepExample(TaskType.ENCHANTING, CreationSession.CreationStep.REWARDS, rewardsExample);
        putStepExample(TaskType.TRADING, CreationSession.CreationStep.TITLE, "Trader");
        putStepExample(TaskType.TRADING, CreationSession.CreationStep.DESCRIPTION, "Trade with villagers 3 times");
        putStepExample(TaskType.TRADING, CreationSession.CreationStep.ICON, "emerald");
        putStepExample(TaskType.TRADING, CreationSession.CreationStep.TARGET, "any");
        putStepExample(TaskType.TRADING, CreationSession.CreationStep.AMOUNT, "3");
        putStepExample(TaskType.TRADING, CreationSession.CreationStep.REWARDS, rewardsExample);
        putStepExample(TaskType.MINING, CreationSession.CreationStep.TITLE, "Ore Miner");
        putStepExample(TaskType.MINING, CreationSession.CreationStep.DESCRIPTION, "Mine 20 iron ore");
        putStepExample(TaskType.MINING, CreationSession.CreationStep.ICON, "iron_pickaxe");
        putStepExample(TaskType.MINING, CreationSession.CreationStep.TARGET, "iron ore");
        putStepExample(TaskType.MINING, CreationSession.CreationStep.AMOUNT, "20");
        putStepExample(TaskType.MINING, CreationSession.CreationStep.REWARDS, rewardsExample);
        putStepExample(TaskType.BREEDING, CreationSession.CreationStep.TITLE, "Animal Breeder");
        putStepExample(TaskType.BREEDING, CreationSession.CreationStep.DESCRIPTION, "Breed 5 cows");
        putStepExample(TaskType.BREEDING, CreationSession.CreationStep.ICON, "wheat");
        putStepExample(TaskType.BREEDING, CreationSession.CreationStep.TARGET, "cow");
        putStepExample(TaskType.BREEDING, CreationSession.CreationStep.AMOUNT, "5");
        putStepExample(TaskType.BREEDING, CreationSession.CreationStep.REWARDS, rewardsExample);
        putStepExample(TaskType.TAMING, CreationSession.CreationStep.TITLE, "Tamer");
        putStepExample(TaskType.TAMING, CreationSession.CreationStep.DESCRIPTION, "Tame 2 wolves");
        putStepExample(TaskType.TAMING, CreationSession.CreationStep.ICON, "bone");
        putStepExample(TaskType.TAMING, CreationSession.CreationStep.TARGET, "wolf");
        putStepExample(TaskType.TAMING, CreationSession.CreationStep.AMOUNT, "2");
        putStepExample(TaskType.TAMING, CreationSession.CreationStep.REWARDS, rewardsExample);
        putStepExample(TaskType.DEATH, CreationSession.CreationStep.TITLE, "Unfortunate Soul");
        putStepExample(TaskType.DEATH, CreationSession.CreationStep.DESCRIPTION, "Die 5 times");
        putStepExample(TaskType.DEATH, CreationSession.CreationStep.ICON, "skeleton_skull");
        putStepExample(TaskType.DEATH, CreationSession.CreationStep.TARGET, "player1");
        putStepExample(TaskType.DEATH, CreationSession.CreationStep.AMOUNT, "5");
        putStepExample(TaskType.DEATH, CreationSession.CreationStep.REWARDS, rewardsExample);
    }

    private static void putStepExample(TaskType type, CreationSession.CreationStep step, String example) {
        STEP_EXAMPLES.computeIfAbsent(type, k -> new HashMap<>()).put(step, example);
    }

    private String getStepExample(TaskType type, CreationSession.CreationStep step) {
        if (type == null) return "";
        Map<CreationSession.CreationStep, String> map = STEP_EXAMPLES.get(type);
        if (map == null) return "";
        return map.getOrDefault(step, "");
    }
}