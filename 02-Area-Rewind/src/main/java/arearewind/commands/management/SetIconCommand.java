package arearewind.commands.management;

import arearewind.commands.base.BaseCommand;
import arearewind.data.AreaBackup;
import arearewind.data.ProtectedArea;
import arearewind.managers.*;
import arearewind.util.ConfigurationManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SetIconCommand extends BaseCommand {

    public SetIconCommand(JavaPlugin plugin, AreaManager areaManager, BackupManager backupManager,
            GUIManager guiManager, VisualizationManager visualizationManager,
            PermissionManager permissionManager, ConfigurationManager configManager,
            FileManager fileManager, IntervalManager intervalManager) {
        super(plugin, areaManager, backupManager, guiManager, visualizationManager,
                permissionManager, configManager, fileManager, intervalManager);
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: " + getUsage());
            return true;
        }

        if (args[0].equalsIgnoreCase("backup")) {
            return handleBackupIcon(player, args);
        }

        return handleAreaIcon(player, args);
    }

    private boolean handleAreaIcon(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "Usage: /rewind seticon <area> <material|hand>");
            return true;
        }

        String areaName = args[0];
        String materialName = args[1];
        ProtectedArea area = areaManager.getArea(areaName);

        if (area == null) {
            player.sendMessage(ChatColor.RED + "Area '" + areaName + "' not found!");
            return true;
        }

        if (!permissionManager.canModifyBoundaries(player, area)) {
            player.sendMessage(ChatColor.RED + "You don't have permission to modify this area!");
            return true;
        }

        if (materialName.equalsIgnoreCase("hand") || materialName.equalsIgnoreCase("held")) {
            ItemStack handItem = player.getInventory().getItemInMainHand();
            if (handItem == null || handItem.getType() == Material.AIR) {
                player.sendMessage(ChatColor.RED + "You must hold an item to use as an icon!");
                return true;
            }

            if (isCustomPlayerHead(handItem)) {
                area.setIconItem(handItem);
                player.sendMessage(ChatColor.GREEN + "Set custom icon for area '" + areaName + "' to " +
                        ChatColor.YELLOW + "custom player head" + ChatColor.GREEN + "!");
            } else {
                area.setIcon(handItem.getType());
                player.sendMessage(ChatColor.GREEN + "Set icon for area '" + areaName + "' to " +
                        ChatColor.YELLOW + handItem.getType().name() + ChatColor.GREEN + "!");
            }

            areaManager.saveAreas();
            return true;
        }

        materialName = materialName.toUpperCase();
        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid material: " + materialName);
            player.sendMessage(ChatColor.GRAY + "Example materials: DIAMOND_BLOCK, EMERALD_BLOCK, REDSTONE_BLOCK");
            return true;
        }

        if (!material.isItem() || material == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Material " + materialName + " cannot be used as an icon!");
            return true;
        }

        area.setIcon(material);
        areaManager.saveAreas();

        player.sendMessage(ChatColor.GREEN + "Set icon for area '" + areaName + "' to " +
                ChatColor.YELLOW + material.name() + ChatColor.GREEN + "!");

        return true;
    }

    private boolean handleBackupIcon(Player player, String[] args) {
        if (args.length != 4) {
            player.sendMessage(ChatColor.RED + "Usage: /rewind seticon backup <area> <backup_id> <material|hand>");
            return true;
        }

        String areaName = args[1];
        String backupId = args[2];
        String materialName = args[3];

        ProtectedArea area = areaManager.getArea(areaName);
        if (area == null) {
            player.sendMessage(ChatColor.RED + "Area '" + areaName + "' not found!");
            return true;
        }

        if (!permissionManager.canModifyBoundaries(player, area)) {
            player.sendMessage(ChatColor.RED + "You don't have permission to modify this area!");
            return true;
        }

        List<AreaBackup> backups = backupManager.getBackupHistory(areaName);
        AreaBackup targetBackup = null;

        for (AreaBackup backup : backups) {
            if (backup.getId().equals(backupId)) {
                targetBackup = backup;
                break;
            }
        }

        if (targetBackup == null) {
            player.sendMessage(ChatColor.RED + "Backup '" + backupId + "' not found for area '" + areaName + "'!");
            return true;
        }

        if (materialName.equalsIgnoreCase("hand") || materialName.equalsIgnoreCase("held")) {
            ItemStack handItem = player.getInventory().getItemInMainHand();
            if (handItem == null || handItem.getType() == Material.AIR) {
                player.sendMessage(ChatColor.RED + "You must hold an item to use as an icon!");
                return true;
            }

            if (isCustomPlayerHead(handItem)) {
                targetBackup.setIconItem(handItem);
                player.sendMessage(ChatColor.GREEN + "Set custom icon for backup '" + backupId + "' in area '"
                        + areaName + "' to " +
                        ChatColor.YELLOW + "custom player head" + ChatColor.GREEN + "!");
            } else {
                targetBackup.setIcon(handItem.getType());
                player.sendMessage(
                        ChatColor.GREEN + "Set icon for backup '" + backupId + "' in area '" + areaName + "' to " +
                                ChatColor.YELLOW + handItem.getType().name() + ChatColor.GREEN + "!");
            }

            fileManager.saveBackupToFile(areaName, targetBackup);
            return true;
        }

        materialName = materialName.toUpperCase();
        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid material: " + materialName);
            player.sendMessage(ChatColor.GRAY + "Example materials: CHEST, ENDER_CHEST, SHULKER_BOX");
            return true;
        }

        if (!material.isItem() || material == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Material " + materialName + " cannot be used as an icon!");
            return true;
        }

        targetBackup.setIcon(material);
        fileManager.saveBackupToFile(areaName, targetBackup);

        player.sendMessage(ChatColor.GREEN + "Set icon for backup '" + backupId + "' in area '" + areaName + "' to " +
                ChatColor.YELLOW + material.name() + ChatColor.GREEN + "!");

        return true;
    }

    @Override
    public List<String> getTabCompletions(Player player, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("backup");
            completions.addAll(getAreaCompletions().stream()
                    .filter(area -> area.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList()));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("backup")) {
                completions.addAll(getAreaCompletions().stream()
                        .filter(area -> area.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList()));
            } else {
                completions.add("hand");
                completions.addAll(getCommonMaterials().stream()
                        .filter(material -> material.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList()));
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("backup")) {
            String areaName = args[1];
            ProtectedArea area = areaManager.getArea(areaName);
            if (area != null && permissionManager.hasAreaPermission(player, area)) {
                List<AreaBackup> backups = backupManager.getBackupHistory(areaName);
                completions.addAll(backups.stream()
                        .map(AreaBackup::getId)
                        .filter(id -> id.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList()));
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("backup")) {
            completions.add("hand");
            completions.addAll(getCommonMaterials().stream()
                    .filter(material -> material.toLowerCase().startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList()));
        }

        return completions;
    }

    private List<String> getCommonMaterials() {
        return Arrays.asList(
                "GRASS_BLOCK", "DIAMOND_BLOCK", "EMERALD_BLOCK", "GOLD_BLOCK", "IRON_BLOCK",
                "REDSTONE_BLOCK", "LAPIS_BLOCK", "COAL_BLOCK", "QUARTZ_BLOCK", "OBSIDIAN",
                "BEDROCK", "STONE", "COBBLESTONE", "BRICKS", "NETHER_BRICKS",

                "CHEST", "ENDER_CHEST", "BARREL", "SHULKER_BOX", "WHITE_SHULKER_BOX",
                "ORANGE_SHULKER_BOX", "MAGENTA_SHULKER_BOX", "LIGHT_BLUE_SHULKER_BOX",
                "YELLOW_SHULKER_BOX", "LIME_SHULKER_BOX", "PINK_SHULKER_BOX", "GRAY_SHULKER_BOX",
                "LIGHT_GRAY_SHULKER_BOX", "CYAN_SHULKER_BOX", "PURPLE_SHULKER_BOX", "BLUE_SHULKER_BOX",
                "BROWN_SHULKER_BOX", "GREEN_SHULKER_BOX", "RED_SHULKER_BOX", "BLACK_SHULKER_BOX",

                "COMPASS", "CLOCK", "BOOK", "WRITABLE_BOOK", "ENCHANTED_BOOK",
                "PAPER", "MAP", "FILLED_MAP", "NAME_TAG", "LEAD");
    }

    private boolean isCustomPlayerHead(ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD) {
            return false;
        }

        if (item.hasItemMeta() && item.getItemMeta() instanceof org.bukkit.inventory.meta.SkullMeta) {
            org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) item.getItemMeta();

            return skullMeta.hasOwner() ||
                    (skullMeta.getOwnerProfile() != null && skullMeta.getOwnerProfile().getTextures() != null) ||
                    skullMeta.getOwningPlayer() != null;
        }

        return false;
    }

    @Override
    public String getName() {
        return "seticon";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("icon", "setitem");
    }

    @Override
    public String getDescription() {
        return "Set custom icons for areas and backups";
    }

    @Override
    public String getUsage() {
        return "/rewind seticon <area> <material|hand> OR /rewind seticon backup <area> <backup_id> <material|hand>";
    }

    @Override
    public String getRequiredPermission() {
        return "arearewind.manage";
    }
}
