package arearewind.managers;
import arearewind.data.AreaBackup;
import arearewind.data.BlockInfo;
import arearewind.data.ProtectedArea;
import arearewind.util.ConfigurationManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class FileManager {
    private final JavaPlugin plugin;
    private final ConfigurationManager configManager;
    private File dataFolder;
    private File backupFolder;
    private File areasFile;
    private File exportsFolder;

    public FileManager(JavaPlugin plugin, ConfigurationManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void setupFiles() {
        dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        backupFolder = new File(dataFolder, "backups");
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }
        exportsFolder = new File(dataFolder, "exports");
        if (!exportsFolder.exists()) {
            exportsFolder.mkdirs();
        }
        areasFile = new File(dataFolder, "areas.yml");
        plugin.getLogger().info("File structure initialized");
    }

    public void saveBackupToFile(String areaName, AreaBackup backup) {
        try {
            File backupFile = new File(backupFolder, areaName + "_" + backup.getId() + ".yml");
            backupFile.getParentFile().mkdirs();
            YamlConfiguration config = new YamlConfiguration();
            config.set("backup", backup);
            config.save(backupFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save backup to file: " + e.getMessage());
        }
    }

    public AreaBackup loadBackupFromFile(String areaName, String backupId) {
        try {
            File backupFile = new File(backupFolder, areaName + "_" + backupId + ".yml");
            if (!backupFile.exists()) return null;
            YamlConfiguration config = new YamlConfiguration();
            config.load(backupFile);
            return (AreaBackup) config.get("backup");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load backup from file: " + e.getMessage());
            return null;
        }
    }

    public void deleteBackupFile(String areaName, String backupId) {
        File backupFile = new File(backupFolder, areaName + "_" + backupId + ".yml");
        if (backupFile.exists()) {
            if (backupFile.delete()) {
                plugin.getLogger().fine("Deleted backup file: " + backupFile.getName());
            } else {
                plugin.getLogger().warning("Failed to delete backup file: " + backupFile.getName());
            }
        }
    }

    public void deleteBackupFiles(String areaName) {
        File[] files = backupFolder.listFiles((dir, name) ->
                name.startsWith(areaName + "_") && name.endsWith(".yml"));

        if (files != null) {
            int deletedCount = 0;
            for (File file : files) {
                if (file.delete()) {
                    deletedCount++;
                }
            }
            plugin.getLogger().info("Deleted " + deletedCount + " backup files for area: " + areaName);
        }
    }

    public void renameBackupFiles(String oldName, String newName) {
        File[] files = backupFolder.listFiles((dir, name) ->
                name.startsWith(oldName + "_") && name.endsWith(".yml"));

        if (files != null) {
            int renamedCount = 0;
            for (File file : files) {
                String fileName = file.getName();
                String newFileName = fileName.replace(oldName + "_", newName + "_");
                File newFile = new File(backupFolder, newFileName);
                if (file.renameTo(newFile)) {
                    renamedCount++;
                }
            }
            plugin.getLogger().info("Renamed " + renamedCount + " backup files from '" + oldName + "' to '" + newName + "'");
        }
    }

    public long getTotalBackupFileSize() {
        long totalSize = 0;
        File[] files = backupFolder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    totalSize += file.length();
                }
            }
        }

        return totalSize;
    }

    public File[] getBackupFiles(String areaName) {
        return backupFolder.listFiles((dir, name) ->
                name.startsWith(areaName + "_") && name.endsWith(".yml"));
    }

    public File[] getAllBackupFiles() {
        return backupFolder.listFiles((dir, name) -> name.endsWith(".yml"));
    }

    public String getDiskUsageStats() {
        long backupSize = getTotalBackupFileSize();
        long freeSpace = dataFolder.getFreeSpace();
        long totalSpace = dataFolder.getTotalSpace();

        return String.format("Backup storage: %s | Free space: %s | Total space: %s",
                formatFileSize(backupSize),
                formatFileSize(freeSpace),
                formatFileSize(totalSpace));
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    public File getDataFolder() { return dataFolder; }
    public File getBackupFolder() { return backupFolder; }
    public File getAreasFile() { return areasFile; }
    public File getExportsFolder() { return exportsFolder; }
}