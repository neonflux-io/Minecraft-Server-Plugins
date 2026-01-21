package arearewind.managers;

import arearewind.data.AreaBackup;
import arearewind.data.BlockInfo;
import arearewind.data.ProtectedArea;
import arearewind.listeners.PlayerInteractionListener;
import arearewind.util.ConfigurationManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.Banner;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BackupManager {

    private final JavaPlugin plugin;
    private final ConfigurationManager configManager;
    private final FileManager fileManager;
    private PlayerInteractionListener playerListener;
    final Map<String, List<AreaBackup>> backupHistory;
    private final Map<String, Integer> undoPointers;
    private final Map<String, AreaBackup> beforeRestoreBackups;

    public BackupManager(JavaPlugin plugin, ConfigurationManager configManager, FileManager fileManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.fileManager = fileManager;
        this.backupHistory = new ConcurrentHashMap<>();
        this.undoPointers = new ConcurrentHashMap<>();
        this.beforeRestoreBackups = new ConcurrentHashMap<>();
    }

    public void setPlayerInteractionListener(PlayerInteractionListener playerListener) {
        this.playerListener = playerListener;
    }

    private boolean isProgressLoggingEnabledForPlayer(Player player) {
        if (playerListener != null) {
            return playerListener.getPlayerProgressLoggingMode(player);
        }
        return true;
    }

    private final Set<Material> POI_BLOCKS = Set.of(
            Material.CARTOGRAPHY_TABLE, Material.FLETCHING_TABLE,
            Material.SMITHING_TABLE, Material.LOOM, Material.STONECUTTER,
            Material.GRINDSTONE, Material.BARREL, Material.SMOKER, Material.BLAST_FURNACE,
            Material.FURNACE, Material.COMPOSTER, Material.BELL);

    private final Set<Material> BED_BLOCKS = Set.of(
            Material.WHITE_BED, Material.ORANGE_BED, Material.MAGENTA_BED, Material.LIGHT_BLUE_BED,
            Material.YELLOW_BED, Material.LIME_BED, Material.PINK_BED, Material.GRAY_BED,
            Material.LIGHT_GRAY_BED, Material.CYAN_BED, Material.PURPLE_BED, Material.BLUE_BED,
            Material.BROWN_BED, Material.GREEN_BED, Material.RED_BED, Material.BLACK_BED);

    private BlockInfo createBlockInfo(Block block) {
        try {
            if (!Bukkit.isPrimaryThread()) {
                plugin.getLogger()
                        .warning("createBlockInfo called from async thread for block at " + block.getLocation());
                return new BlockInfo(block.getType(), block.getBlockData());
            }

            BlockInfo blockInfo = new BlockInfo(block.getType(), block.getBlockData());

            try {
                if (block.getState() instanceof Banner) {
                    Banner banner = (Banner) block.getState();
                    if (banner.getPatterns() != null) {
                        blockInfo.setBannerPatterns(new ArrayList<>(banner.getPatterns()));
                    }
                } else if (block.getState() instanceof Sign) {
                    Sign sign = (Sign) block.getState();
                    try {
                        String[] lines = new String[4];
                        for (int i = 0; i < 4; i++) {
                            lines[i] = sign.getSide(org.bukkit.block.sign.Side.FRONT).getLine(i);
                        }
                        blockInfo.setSignLines(lines);
                    } catch (Exception e) {
                        @SuppressWarnings("deprecation")
                        String[] legacyLines = sign.getLines();
                        if (legacyLines != null) {
                            blockInfo.setSignLines(legacyLines);
                        }
                    }
                } else if (block.getState() instanceof org.bukkit.block.Container) {
                    org.bukkit.block.Container container = (org.bukkit.block.Container) block.getState();
                    if (container.getInventory() != null) {
                        ItemStack[] contents = container.getInventory().getContents();
                        blockInfo.setContainerContents(contents);

                        plugin.getLogger().fine("Backup: Container (" + block.getType() + ") at " +
                                block.getLocation() + " has " + (contents != null ? contents.length : 0) +
                                " slots with items: " + getContainerSummary(contents));
                    }
                } else if (block.getState() instanceof org.bukkit.block.Jukebox) {
                    org.bukkit.block.Jukebox jukebox = (org.bukkit.block.Jukebox) block.getState();
                    if (jukebox.getRecord() != null) {
                        blockInfo.setJukeboxRecord(jukebox.getRecord());
                    }
                } else if (block.getState() instanceof org.bukkit.block.Skull) {
                    org.bukkit.block.Skull skull = (org.bukkit.block.Skull) block.getState();
                    if (skull.getOwningPlayer() != null) {
                        blockInfo.setSkullOwner(skull.getOwningPlayer().getName());
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to read special block state for " + block.getType() +
                        " at " + block.getLocation() + ": " + e.getMessage() +
                        " (Block data will be preserved but special properties may be lost)");
            }

            return blockInfo;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create BlockInfo for block at " + block.getLocation() +
                    ": " + e.getMessage() + " - Using fallback");

            try {
                return new BlockInfo(block.getType(), block.getBlockData());
            } catch (Exception fallbackError) {
                plugin.getLogger().severe("Critical error creating BlockInfo fallback: " + fallbackError.getMessage());
                return new BlockInfo(Material.AIR, Material.AIR.createBlockData());
            }
        }
    }

    public AreaBackup createBackupFromArea(ProtectedArea area) {
        Map<String, BlockInfo> blocks = new HashMap<>();
        Location min = area.getMin();
        Location max = area.getMax();
        World world = min.getWorld();

        int totalBlocks = area.getSize();
        int processedBlocks = 0;
        int errorBlocks = 0;

        plugin.getLogger().info("Creating backup for area '" + area.getName() + "' with " + totalBlocks + " blocks");

        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    try {
                        Block block = world.getBlockAt(x, y, z);
                        String key = x + "," + y + "," + z;

                        BlockInfo blockInfo = createBlockInfo(block);
                        blocks.put(key, blockInfo);
                        processedBlocks++;

                    } catch (Exception e) {
                        errorBlocks++;
                        plugin.getLogger().warning("Failed to backup block at " + x + "," + y + "," + z +
                                ": " + e.getMessage());

                        String key = x + "," + y + "," + z;
                        blocks.put(key, new BlockInfo(Material.AIR, Material.AIR.createBlockData()));
                    }
                }
            }
        }

        plugin.getLogger().info("Backup completed: " + processedBlocks + " blocks processed, " +
                errorBlocks + " errors (replaced with air)");

        Map<String, Object> entities = backupEntitiesInArea(area);
        plugin.getLogger().info("Backup completed: " + entities.size() + " entities captured");

        return new AreaBackup(LocalDateTime.now(), blocks, entities);
    }

    private AreaBackup createHiddenBackup(ProtectedArea area) {
        AreaBackup backup = createBackupFromArea(area);
        backup.setHidden(true);
        return backup;
    }

    public void restoreFromBackup(ProtectedArea area, AreaBackup backup) {
        restoreFromBackup(area, backup, null);
    }

    public void restoreFromBackup(ProtectedArea area, AreaBackup backup, Player player) {
        restoreFromBackup(area, backup, player, "✓ Restoration complete!");
    }

    public void restoreFromBackup(ProtectedArea area, AreaBackup backup, Player player, String completionMessage) {
        restoreFromBackupOptimized(area, backup, player, completionMessage);
    }

    private void restoreFromBackupOptimized(ProtectedArea area, AreaBackup backup, Player player,
            String completionMessage) {
        Location min = area.getMin();
        Location max = area.getMax();
        World world = min.getWorld();
        Map<String, BlockInfo> blocks = backup.getBlocks();
        final int total = blocks.size();
        final int batchSize = area.hasCustomRestoreSpeed() ? area.getCustomRestoreSpeed()
                : calculateOptimalBatchSize(total);

        if (player != null) {
            if (isProgressLoggingEnabledForPlayer(player)) {
                player.sendMessage(ChatColor.YELLOW + "Starting optimized restoration of " + total + " blocks...");
                player.sendMessage(ChatColor.GRAY + "Using batch size: " + batchSize + " blocks/tick" +
                        (area.hasCustomRestoreSpeed() ? " (custom speed)" : " (dynamic)"));
            } else {
                player.sendMessage(ChatColor.YELLOW + "Starting restoration of " + total + " blocks...");
            }
        }

        Set<org.bukkit.Chunk> chunksToLoad = preloadChunks(world, min, max);
        plugin.getLogger().info("Pre-loaded " + chunksToLoad.size() + " chunks for restoration");
        List<String> regularBlocks = new ArrayList<>();
        List<String> specialBlocks = new ArrayList<>();
        List<String> containerBlocks = new ArrayList<>();

        for (Map.Entry<String, BlockInfo> entry : blocks.entrySet()) {
            String key = entry.getKey();
            BlockInfo info = entry.getValue();

            if (info.hasContainerContents()) {
                containerBlocks.add(key);
            } else if (hasSpecialProperties(info)) {
                specialBlocks.add(key);
            } else {
                regularBlocks.add(key);
            }
        }

        new BukkitRunnable() {
            int regularIndex = 0;
            int specialIndex = 0;
            int containerIndex = 0;
            int phase = 1;
            int containerCount = 0;
            long startTime = System.currentTimeMillis();
            long lastProgressTime = startTime;

            @Override
            public void run() {
                try {
                    if (phase == 1) {
                        int processed = processRegularBlocks(regularBlocks, blocks, world, regularIndex, batchSize);
                        regularIndex += processed;

                        if (regularIndex >= regularBlocks.size()) {
                            phase = 2;
                            if (player != null && isProgressLoggingEnabledForPlayer(player)) {
                                if (!specialBlocks.isEmpty()) {
                                    player.sendMessage(
                                            ChatColor.YELLOW + "Regular blocks complete! Processing special blocks...");
                                } else if (!containerBlocks.isEmpty()) {
                                    player.sendMessage(
                                            ChatColor.YELLOW
                                                    + "Regular blocks complete! Loading container contents...");
                                } else {
                                    player.sendMessage(ChatColor.YELLOW + "Regular blocks complete!");
                                }
                            }
                        }
                    } else if (phase == 2) {
                        int specialBatchSize = Math.max(10, batchSize / 4);
                        int processed = processSpecialBlocks(specialBlocks, blocks, world, specialIndex,
                                specialBatchSize);
                        specialIndex += processed;

                        if (specialIndex >= specialBlocks.size()) {
                            phase = 3;
                            if (player != null && isProgressLoggingEnabledForPlayer(player)) {
                                if (!containerBlocks.isEmpty()) {
                                    player.sendMessage(
                                            ChatColor.YELLOW
                                                    + "Special blocks complete! Loading container contents...");
                                } else {
                                    player.sendMessage(ChatColor.YELLOW + "Special blocks complete!");
                                }
                            }
                        }
                    } else if (phase == 3) {
                        int containerBatchSize = Math.max(5, batchSize / 8);
                        int processed = processContainerBlocks(containerBlocks, blocks, world, containerIndex,
                                containerBatchSize);
                        containerIndex += processed;
                        containerCount += processed;

                        if (containerIndex >= containerBlocks.size()) {
                            restoreEntitiesInArea(area, backup.getEntities());

                            long duration = System.currentTimeMillis() - startTime;
                            if (player != null) {
                                player.sendMessage(ChatColor.GREEN + completionMessage);
                                if (isProgressLoggingEnabledForPlayer(player)) {
                                    player.sendMessage(ChatColor.GRAY + "Restored: " + total + " blocks, " +
                                            containerCount + " containers, " + backup.getEntities().size()
                                            + " entities in " + duration + "ms");
                                    player.sendMessage(ChatColor.GRAY + "Performance: " +
                                            String.format("%.1f", (total * 1000.0) / duration) + " blocks/second");
                                }
                            }
                            plugin.getLogger().info("Optimized restoration completed: " + total + " blocks, " +
                                    containerCount + " containers in " + duration + "ms");
                            this.cancel();
                        }
                    }

                    if (player != null && isProgressLoggingEnabledForPlayer(player)) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastProgressTime >= 1000) {
                            int totalProcessed = regularIndex + specialIndex + containerIndex;
                            int totalToProcess = regularBlocks.size() + specialBlocks.size() + containerBlocks.size();
                            int progress = (int) ((double) totalProcessed / totalToProcess * 100);

                            player.sendMessage(ChatColor.YELLOW + "Progress: " + progress + "% (" +
                                    totalProcessed + "/" + totalToProcess + " blocks)");
                            lastProgressTime = currentTime;
                        }
                    }

                } catch (Exception e) {
                    plugin.getLogger().severe("Error during optimized restoration: " + e.getMessage());
                    e.printStackTrace();
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private int calculateOptimalBatchSize(int totalBlocks) {
        int minBatch = configManager.getRestoreMinBatchSize();
        int maxBatch = configManager.getRestoreMaxBatchSize();

        if (totalBlocks < 1000)
            return minBatch;
        if (totalBlocks < 10000)
            return minBatch + 50;
        if (totalBlocks < 50000)
            return Math.min(maxBatch - 50, minBatch + 150);
        return maxBatch;
    }

    private Set<org.bukkit.Chunk> preloadChunks(World world, Location min, Location max) {
        Set<org.bukkit.Chunk> chunks = new HashSet<>();

        int minChunkX = min.getBlockX() >> 4;
        int maxChunkX = max.getBlockX() >> 4;
        int minChunkZ = min.getBlockZ() >> 4;
        int maxChunkZ = max.getBlockZ() >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                org.bukkit.Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                if (!chunk.isLoaded()) {
                    chunk.load();
                }
                chunks.add(chunk);
            }
        }

        return chunks;
    }

    private boolean hasSpecialProperties(BlockInfo info) {
        return info.getBannerPatterns() != null ||
                info.getSignLines() != null ||
                info.getJukeboxRecord() != null ||
                info.getSkullOwner() != null;
    }

    private int processRegularBlocks(List<String> blockKeys, Map<String, BlockInfo> blocks,
            World world, int startIndex, int batchSize) {
        int processed = 0;
        int index = startIndex;

        while (index < blockKeys.size() && processed < batchSize) {
            String key = blockKeys.get(index);
            BlockInfo info = blocks.get(key);
            String[] parts = key.split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);

            try {
                Block block = world.getBlockAt(x, y, z);
                block.setType(info.getMaterial(), false);
                block.setBlockData(info.getBlockData(), false);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to restore regular block at " + x + "," + y + "," + z +
                        ": " + e.getMessage());
            }

            index++;
            processed++;
        }

        return processed;
    }

    private int processSpecialBlocks(List<String> blockKeys, Map<String, BlockInfo> blocks,
            World world, int startIndex, int batchSize) {
        int processed = 0;
        int index = startIndex;

        while (index < blockKeys.size() && processed < batchSize) {
            String key = blockKeys.get(index);
            BlockInfo info = blocks.get(key);
            String[] parts = key.split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);

            try {
                Block block = world.getBlockAt(x, y, z);
                block.setType(info.getMaterial(), false);
                block.setBlockData(info.getBlockData(), false);
                restoreNonContainerSpecialData(block, info);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to restore special block at " + x + "," + y + "," + z +
                        ": " + e.getMessage());
            }

            index++;
            processed++;
        }

        return processed;
    }

    private int processContainerBlocks(List<String> blockKeys, Map<String, BlockInfo> blocks,
            World world, int startIndex, int batchSize) {
        int processed = 0;
        int index = startIndex;

        while (index < blockKeys.size() && processed < batchSize) {
            String key = blockKeys.get(index);
            BlockInfo info = blocks.get(key);
            String[] parts = key.split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);

            try {
                Block block = world.getBlockAt(x, y, z);
                block.setType(info.getMaterial(), false);
                block.setBlockData(info.getBlockData(), false);
                restoreContainerContents(block, info);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to restore container block at " + x + "," + y + "," + z +
                        ": " + e.getMessage());
            }

            index++;
            processed++;
        }

        return processed;
    }

    private void restoreNonContainerSpecialData(Block block, BlockInfo info) {
        try {
            if (block.getState() instanceof Banner && info.getBannerPatterns() != null) {
                Banner banner = (Banner) block.getState();
                banner.setPatterns(info.getBannerPatterns());
                banner.update(true, false);
            } else if (block.getState() instanceof Sign && info.getSignLines() != null) {
                Sign sign = (Sign) block.getState();
                String[] lines = info.getSignLines();
                for (int i = 0; i < lines.length && i < 4; i++) {
                    if (lines[i] != null) {
                        try {
                            sign.getSide(org.bukkit.block.sign.Side.FRONT).setLine(i,
                                    ChatColor.translateAlternateColorCodes('&', lines[i]));
                        } catch (Exception e) {
                            String line = lines[i];
                            try {
                                java.lang.reflect.Method setLineMethod = sign.getClass().getMethod("setLine", int.class,
                                        String.class);
                                setLineMethod.invoke(sign, i, ChatColor.translateAlternateColorCodes('&', line));
                            } catch (Exception ex) {
                                plugin.getLogger().fine("Could not restore sign line " + i + ": " + ex.getMessage());
                            }
                        }
                    }
                }
                sign.update(true, false);
            } else if (block.getState() instanceof org.bukkit.block.Jukebox && info.getJukeboxRecord() != null) {
                org.bukkit.block.Jukebox jukebox = (org.bukkit.block.Jukebox) block.getState();
                jukebox.setRecord(info.getJukeboxRecord());
                jukebox.update(true, false);
            } else if (block.getState() instanceof org.bukkit.block.Skull && info.getSkullOwner() != null) {
                org.bukkit.block.Skull skull = (org.bukkit.block.Skull) block.getState();
                try {
                    java.util.UUID uuid = java.util.UUID.fromString(info.getSkullOwner());
                    org.bukkit.OfflinePlayer owner = Bukkit.getOfflinePlayer(uuid);
                    skull.setOwningPlayer(owner);
                } catch (IllegalArgumentException e) {
                    @SuppressWarnings("deprecation")
                    org.bukkit.OfflinePlayer owner = Bukkit.getOfflinePlayer(info.getSkullOwner());
                    skull.setOwningPlayer(owner);
                }
                skull.update(true, false);
            }

            if (isPOIBlock(block.getType())) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    try {
                        block.getState().update(true, true);
                        block.getChunk().load();
                    } catch (Exception e) {
                    }
                }, 2L);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to restore special data for block at " +
                    block.getLocation() + ": " + e.getMessage());
        }
    }

    private boolean isPOIBlock(Material material) {
        return POI_BLOCKS.contains(material) || BED_BLOCKS.contains(material);
    }

    public Map<String, Object> backupEntitiesInArea(ProtectedArea area) {
        Map<String, Object> entities = new HashMap<>();
        Location min = area.getMin();
        Location max = area.getMax();
        World world = min.getWorld();

        try {
            world.getEntities().stream()
                    .filter(entity -> {
                        Location loc = entity.getLocation();
                        return loc.getBlockX() >= min.getBlockX() && loc.getBlockX() <= max.getBlockX() &&
                                loc.getBlockY() >= min.getBlockY() && loc.getBlockY() <= max.getBlockY() &&
                                loc.getBlockZ() >= min.getBlockZ() && loc.getBlockZ() <= max.getBlockZ();
                    })
                    .forEach(entity -> {
                        if (entity instanceof org.bukkit.entity.ItemFrame) {
                            org.bukkit.entity.ItemFrame frame = (org.bukkit.entity.ItemFrame) entity;

                            Map<String, Object> frameData = new HashMap<>();
                            frameData.put("type", "ITEM_FRAME");
                            frameData.put("x", entity.getLocation().getX());
                            frameData.put("y", entity.getLocation().getY());
                            frameData.put("z", entity.getLocation().getZ());
                            frameData.put("facing", frame.getFacing().name());
                            frameData.put("rotation", frame.getRotation().name());

                            if (frame.getItem() != null && frame.getItem().getType() != Material.AIR) {
                                frameData.put("item", frame.getItem());
                            }

                            String key = "frame_" + entity.getLocation().getBlockX() + "_" +
                                    entity.getLocation().getBlockY() + "_" +
                                    entity.getLocation().getBlockZ();
                            entities.put(key, frameData);
                        } else if (entity instanceof org.bukkit.entity.ArmorStand) {
                        }
                    });

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to backup entities: " + e.getMessage());
        }

        return entities;
    }

    public void restoreEntitiesInArea(ProtectedArea area, Map<String, Object> entityData) {
        if (entityData == null || entityData.isEmpty())
            return;

        Location min = area.getMin();
        Location max = area.getMax();
        World world = min.getWorld();

        world.getEntities().stream()
                .filter(entity -> {
                    Location loc = entity.getLocation();
                    return loc.getBlockX() >= min.getBlockX() && loc.getBlockX() <= max.getBlockX() &&
                            loc.getBlockY() >= min.getBlockY() && loc.getBlockY() <= max.getBlockY() &&
                            loc.getBlockZ() >= min.getBlockZ() && loc.getBlockZ() <= max.getBlockZ();
                })
                .filter(entity -> entity instanceof org.bukkit.entity.ItemFrame)
                .forEach(org.bukkit.entity.Entity::remove);

        for (Map.Entry<String, Object> entry : entityData.entrySet()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) entry.getValue();

                if ("ITEM_FRAME".equals(data.get("type"))) {
                    double x = (Double) data.get("x");
                    double y = (Double) data.get("y");
                    double z = (Double) data.get("z");

                    Location loc = new Location(world, x, y, z);
                    org.bukkit.block.BlockFace facing = org.bukkit.block.BlockFace.valueOf((String) data.get("facing"));

                    org.bukkit.entity.ItemFrame frame = world.spawn(loc, org.bukkit.entity.ItemFrame.class,
                            itemFrame -> {
                                itemFrame.setFacingDirection(facing);

                                if (data.containsKey("rotation")) {
                                    org.bukkit.Rotation rotation = org.bukkit.Rotation
                                            .valueOf((String) data.get("rotation"));
                                    itemFrame.setRotation(rotation);
                                }

                                if (data.containsKey("item")) {
                                    ItemStack item = (ItemStack) data.get("item");
                                    itemFrame.setItem(item);
                                }
                            });

                    plugin.getLogger().fine("Restored item frame at " + loc + " with item: " +
                            (frame.getItem() != null ? frame.getItem().getType() : "none"));
                }

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to restore entity " + entry.getKey() + ": " + e.getMessage());
            }
        }
    }

    private boolean restoreContainerContents(Block block, BlockInfo info) {
        if (!info.hasContainerContents()) {
            return false;
        }

        try {
            if (block.getState() instanceof org.bukkit.block.Container) {
                plugin.getLogger().info("Restoring container at " + block.getLocation() +
                        " - Type: " + block.getType() + ", State: " + block.getState().getClass().getSimpleName());

                org.bukkit.block.Container container = (org.bukkit.block.Container) block.getState();
                ItemStack[] contents = info.getContainerContents();

                if (contents != null) {
                    container.getInventory().clear();

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        try {
                            org.bukkit.block.Container freshContainer = (org.bukkit.block.Container) block.getState();

                            plugin.getLogger()
                                    .info("Restoring container contents (" + block.getType() + ") at "
                                            + block.getLocation());

                            ItemStack[] safeCopy = new ItemStack[freshContainer.getInventory().getSize()];
                            for (int i = 0; i < safeCopy.length && i < contents.length; i++) {
                                if (contents[i] != null && contents[i].getType() != Material.AIR) {
                                    safeCopy[i] = contents[i].clone();
                                }
                            }

                            freshContainer.getInventory().setContents(safeCopy);
                            freshContainer.update(true, true);

                            try {
                                freshContainer.getInventory().clear();
                                for (int i = 0; i < contents.length
                                        && i < freshContainer.getInventory().getSize(); i++) {
                                    if (contents[i] != null && contents[i].getType() != Material.AIR) {
                                        freshContainer.getInventory().setItem(i, contents[i].clone());
                                    }
                                }
                            } catch (Exception e2) {
                                plugin.getLogger().warning("Alternative restore method failed: " + e2.getMessage());
                            }

                            ItemStack[] afterContents = freshContainer.getInventory().getContents();
                            plugin.getLogger().info("Container after restore: " + getContainerSummary(afterContents));

                            block.getState().update(true, true);

                            if (block.getChunk().isLoaded()) {
                                for (int dx = -1; dx <= 1; dx++) {
                                    for (int dy = -1; dy <= 1; dy++) {
                                        for (int dz = -1; dz <= 1; dz++) {
                                            Block neighbor = block.getRelative(dx, dy, dz);
                                            if (neighbor.getType() != Material.AIR) {
                                                neighbor.getState().update(false, false);
                                            }
                                        }
                                    }
                                }
                            }

                            plugin.getLogger().info("Successfully restored container (" + block.getType() +
                                    ") at " + block.getLocation() + " with " + getContainerSummary(contents));

                        } catch (Exception e) {
                            plugin.getLogger().severe("Failed to restore container contents (delayed) at " +
                                    block.getLocation() + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    }, 2L);

                    return true;
                }
            } else {
                plugin.getLogger().warning("Block at " + block.getLocation() +
                        " is not a supported container type but has container contents! Type: " + block.getType() +
                        ", State: " + block.getState().getClass().getSimpleName());
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to restore container contents at " +
                    block.getLocation() + ": " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    private String getContainerSummary(ItemStack[] contents) {
        if (contents == null)
            return "empty";

        int itemCount = 0;
        Map<Material, Integer> materialCounts = new HashMap<>();

        for (ItemStack item : contents) {
            if (item != null && item.getType() != Material.AIR) {
                itemCount++;
                materialCounts.put(item.getType(),
                        materialCounts.getOrDefault(item.getType(), 0) + item.getAmount());
            }
        }

        if (itemCount == 0)
            return "empty";

        StringBuilder summary = new StringBuilder();
        summary.append(itemCount).append(" items (");

        int count = 0;
        for (Map.Entry<Material, Integer> entry : materialCounts.entrySet()) {
            if (count > 0)
                summary.append(", ");
            summary.append(entry.getKey()).append(" x").append(entry.getValue());
            count++;
            if (count >= 3) {
                if (materialCounts.size() > 3) {
                    summary.append(", +").append(materialCounts.size() - 3).append(" more");
                }
                break;
            }
        }
        summary.append(")");

        return summary.toString();
    }

    public void createBackup(String areaName, ProtectedArea area) {
        try {
            if (!Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTask(plugin, () -> createBackup(areaName, area));
                return;
            }

            plugin.getLogger().info("Starting backup creation for area: " + areaName);

            AreaBackup backup = createBackupFromArea(area);

            if (!backupHistory.containsKey(areaName)) {
                backupHistory.put(areaName, new ArrayList<>());
            }

            List<AreaBackup> backups = backupHistory.get(areaName);
            backups.add(backup);

            undoPointers.put(areaName, backups.size() - 1);
            plugin.getLogger().info("Set current state pointer to newest backup for area: " + areaName);

            int maxBackups = configManager.getMaxBackupsPerArea();
            if (backups.size() > maxBackups) {
                AreaBackup removed = backups.remove(0);
                fileManager.deleteBackupFile(areaName, removed.getId());
                plugin.getLogger().info("Removed oldest backup for area: " + areaName);

                Integer currentPointer = undoPointers.get(areaName);
                if (currentPointer != null && currentPointer > 0) {
                    undoPointers.put(areaName, currentPointer - 1);
                }
            }

            try {
                fileManager.saveBackupToFile(areaName, backup);
                plugin.getLogger().info("Successfully saved backup for area: " + areaName +
                        " (ID: " + backup.getId() + ", Blocks: " + backup.getBlocks().size() + ")");
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save backup file for area: " + areaName +
                        " - " + e.getMessage());
                e.printStackTrace();
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Critical error during backup creation for area: " + areaName +
                    " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<AreaBackup> getBackupHistory(String areaName) {
        List<AreaBackup> allBackups = backupHistory.getOrDefault(areaName, new ArrayList<>());
        return allBackups.stream()
                .filter(backup -> !backup.isHidden())
                .collect(java.util.stream.Collectors.toList());
    }

    public List<AreaBackup> getAllBackups(String areaName) {
        return backupHistory.getOrDefault(areaName, new ArrayList<>());
    }

    public AreaBackup getBackup(String areaName, int index) {
        List<AreaBackup> visibleBackups = getBackupHistory(areaName);
        if (visibleBackups == null || index < 0 || index >= visibleBackups.size()) {
            return null;
        }
        return visibleBackups.get(index);
    }

    public boolean restoreArea(String areaName, ProtectedArea area, int backupIndex) {
        return restoreArea(areaName, area, backupIndex, true);
    }

    public boolean restoreArea(String areaName, ProtectedArea area, int backupIndex, boolean createBackupFirst) {
        return restoreArea(areaName, area, backupIndex, createBackupFirst, null);
    }

    public boolean restoreArea(String areaName, ProtectedArea area, int backupIndex, boolean createBackupFirst,
            Player player) {
        AreaBackup backup = getBackup(areaName, backupIndex);
        if (backup == null)
            return false;

        AreaBackup beforeRestore = createHiddenBackup(area);
        beforeRestoreBackups.put(areaName, beforeRestore);

        if (createBackupFirst) {
            createBackup(areaName, area);
        }

        restoreFromBackup(area, backup, player);
        undoPointers.put(areaName, backupIndex);
        plugin.getLogger()
                .info("Set current state pointer to restored backup #" + backupIndex + " for area: " + areaName);

        return true;
    }

    public boolean undoArea(String areaName, ProtectedArea area) {
        return undoArea(areaName, area, null);
    }

    public boolean undoArea(String areaName, ProtectedArea area, Player player) {
        AreaBackup beforeRestoreBackup = beforeRestoreBackups.get(areaName);
        if (beforeRestoreBackup == null) {
            return false;
        }

        restoreFromBackup(area, beforeRestoreBackup, player,
                "✓ Undo successful! Restored to state before last backup restore.");

        beforeRestoreBackups.remove(areaName);
        plugin.getLogger().info("Undo completed for area: " + areaName + " - cleared undo state");

        return true;
    }

    public boolean redoArea(String areaName, ProtectedArea area) {
        return redoArea(areaName, area, null);
    }

    public boolean redoArea(String areaName, ProtectedArea area, Player player) {
        return false;
    }

    public AreaBackup findClosestBackup(String areaName, LocalDateTime targetTime) {
        List<AreaBackup> backups = backupHistory.get(areaName);
        if (backups == null || backups.isEmpty())
            return null;

        AreaBackup closestBackup = null;
        long closestDiff = Long.MAX_VALUE;

        for (AreaBackup backup : backups) {
            long diff = Math.abs(java.time.Duration.between(backup.getTimestamp(), targetTime).toMinutes());
            if (diff < closestDiff) {
                closestDiff = diff;
                closestBackup = backup;
            }
        }

        return closestBackup;
    }

    public int cleanupBackups(String areaName, int daysOld) {
        if (!backupHistory.containsKey(areaName))
            return 0;

        List<AreaBackup> backups = backupHistory.get(areaName);
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysOld);

        int removedCount = 0;
        List<AreaBackup> toRemove = new ArrayList<>();
        for (AreaBackup backup : backups) {
            if (backup.getTimestamp().isBefore(cutoffTime)) {
                toRemove.add(backup);
            }
        }

        if (toRemove.size() >= backups.size()) {
            toRemove.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
            for (int i = 0; i < toRemove.size() - 1; i++) {
                AreaBackup backup = toRemove.get(i);
                fileManager.deleteBackupFile(areaName, backup.getId());
                backups.remove(backup);
                removedCount++;
            }
        } else {
            for (AreaBackup backup : toRemove) {
                fileManager.deleteBackupFile(areaName, backup.getId());
                backups.remove(backup);
                removedCount++;
            }
        }

        return removedCount;
    }

    public boolean deleteBackup(String areaName, int backupIndex) {
        List<AreaBackup> backups = backupHistory.get(areaName);
        if (backups == null || backupIndex < 0 || backupIndex >= backups.size()) {
            return false;
        }

        AreaBackup backupToDelete = backups.remove(backupIndex);
        fileManager.deleteBackupFile(areaName, backupToDelete.getId());

        Integer undoPointer = undoPointers.get(areaName);
        if (undoPointer != null) {
            if (undoPointer == backupIndex) {
                undoPointers.remove(areaName);
                beforeRestoreBackups.remove(areaName);
            } else if (undoPointer > backupIndex) {
                undoPointers.put(areaName, undoPointer - 1);
            }
        }

        plugin.getLogger().info("Deleted backup #" + backupIndex + " for area: " + areaName);
        return true;
    }

    public void deleteAllBackups(String areaName) {
        List<AreaBackup> backups = backupHistory.remove(areaName);
        if (backups != null) {
            for (AreaBackup backup : backups) {
                fileManager.deleteBackupFile(areaName, backup.getId());
            }
        }
        undoPointers.remove(areaName);
        beforeRestoreBackups.remove(areaName);
    }

    public int deleteAllBackupsExceptLast(String areaName) {
        List<AreaBackup> backups = backupHistory.get(areaName);
        if (backups == null || backups.size() <= 1) {
            return 0;
        }

        backups.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));

        int lastIndex = backups.size() - 1;
        AreaBackup lastBackup = backups.get(lastIndex);

        int removedCount = 0;
        for (int i = 0; i < lastIndex; i++) {
            AreaBackup toRemove = backups.get(i);
            try {
                fileManager.deleteBackupFile(areaName, toRemove.getId());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to delete backup file for area " + areaName + ": " + e.getMessage());
            }
            removedCount++;
        }
        List<AreaBackup> newList = new ArrayList<>();
        newList.add(lastBackup);
        backupHistory.put(areaName, newList);
        undoPointers.put(areaName, 0);
        beforeRestoreBackups.remove(areaName);

        plugin.getLogger().info("Deleted " + removedCount + " backups for area: " + areaName + " (kept latest)");
        return removedCount;
    }

    public void renameAreaBackups(String oldName, String newName) {
        List<AreaBackup> backups = backupHistory.remove(oldName);
        if (backups != null) {
            backupHistory.put(newName, backups);
            fileManager.renameBackupFiles(oldName, newName);
        }

        Integer undoPointer = undoPointers.remove(oldName);
        if (undoPointer != null) {
            undoPointers.put(newName, undoPointer);
        }

        AreaBackup beforeRestore = beforeRestoreBackups.remove(oldName);
        if (beforeRestore != null) {
            beforeRestoreBackups.put(newName, beforeRestore);
        }
    }

    public List<String> compareBackups(AreaBackup backup1, AreaBackup backup2) {
        List<String> differences = new ArrayList<>();
        Map<String, BlockInfo> blocks1 = backup1.getBlocks();
        Map<String, BlockInfo> blocks2 = backup2.getBlocks();

        for (String pos : blocks1.keySet()) {
            if (!blocks2.containsKey(pos)) {
                differences.add(ChatColor.RED + "[-] " + pos + " - " + blocks1.get(pos).getMaterial());
            } else if (!blocks1.get(pos).equals(blocks2.get(pos))) {
                differences.add(ChatColor.YELLOW + "[~] " + pos + " - " +
                        blocks1.get(pos).getMaterial() + " → " + blocks2.get(pos).getMaterial());
            }
        }

        for (String pos : blocks2.keySet()) {
            if (!blocks1.containsKey(pos)) {
                differences.add(ChatColor.GREEN + "[+] " + pos + " - " + blocks2.get(pos).getMaterial());
            }
        }

        return differences;
    }

    public List<String> getDifferencesFromLast(String areaName, ProtectedArea area) {
        List<AreaBackup> backups = backupHistory.get(areaName);
        if (backups == null || backups.isEmpty()) {
            return new ArrayList<>();
        }

        AreaBackup currentState = createBackupFromArea(area);
        AreaBackup lastBackup = backups.get(backups.size() - 1);

        return compareBackups(lastBackup, currentState);
    }

    public void loadBackups() {
        plugin.getLogger().info("Loading backup history from files...");

        File backupFolder = fileManager.getBackupFolder();
        if (!backupFolder.exists()) {
            plugin.getLogger().info("No backup folder found");
            return;
        }

        File[] backupFiles = backupFolder.listFiles((dir, name) -> name.endsWith(".yml.gz") || name.endsWith(".yml"));
        if (backupFiles == null || backupFiles.length == 0) {
            plugin.getLogger().info("No backup files found");
            return;
        }

        plugin.getLogger().info("Found " + backupFiles.length + " backup files to process");

        int totalLoaded = 0;
        int totalFailed = 0;
        Map<String, Integer> areaBackupCounts = new HashMap<>();

        for (File file : backupFiles) {
            try {
                String fileName = file.getName();
                if (fileName.endsWith(".yml.gz")) {
                    fileName = fileName.replace(".yml.gz", "");
                } else if (fileName.endsWith(".yml")) {
                    fileName = fileName.replace(".yml", "");
                }
                int lastUnderscoreIndex = fileName.lastIndexOf("_");

                if (lastUnderscoreIndex > 0 && lastUnderscoreIndex < fileName.length() - 1) {
                    String areaName = fileName.substring(0, lastUnderscoreIndex);
                    String backupId = fileName.substring(lastUnderscoreIndex + 1);

                    AreaBackup backup = fileManager.loadBackupFromFile(areaName, backupId);
                    if (backup != null) {
                        if (!backupHistory.containsKey(areaName)) {
                            backupHistory.put(areaName, new ArrayList<>());
                        }

                        List<AreaBackup> areaBackups = backupHistory.get(areaName);

                        boolean exists = false;
                        for (AreaBackup existing : areaBackups) {
                            if (existing.getId().equals(backup.getId())) {
                                exists = true;
                                break;
                            }
                        }

                        if (!exists) {
                            areaBackups.add(backup);
                            totalLoaded++;
                            areaBackupCounts.put(areaName, areaBackupCounts.getOrDefault(areaName, 0) + 1);
                        }
                    } else {
                        plugin.getLogger().warning("Failed to load backup from file: " + file.getName() +
                                " (Area: " + areaName + ", BackupID: " + backupId + ")");
                    }
                } else {
                    plugin.getLogger().warning("Invalid backup filename format: " + file.getName() +
                            " (Expected format: areaName_backupId.yml)");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load backup file " + file.getName() + ": " + e.getMessage());
                totalFailed++;
                e.printStackTrace();
            }
        }

        for (Map.Entry<String, List<AreaBackup>> entry : backupHistory.entrySet()) {
            String areaName = entry.getKey();
            List<AreaBackup> areaBackups = entry.getValue();

            areaBackups.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));

            if (!areaBackups.isEmpty()) {
                undoPointers.put(areaName, areaBackups.size() - 1);
            }
        }

        plugin.getLogger().info("Successfully loaded " + totalLoaded + " backups for " +
                backupHistory.size() + " areas (" + totalFailed + " failed)");

        for (Map.Entry<String, Integer> entry : areaBackupCounts.entrySet()) {
            plugin.getLogger().info("Area '" + entry.getKey() + "': " + entry.getValue() + " backups loaded");
        }
    }

    public long parseTimeString(String timeStr) {
        try {
            char unit = timeStr.charAt(timeStr.length() - 1);
            int amount = Integer.parseInt(timeStr.substring(0, timeStr.length() - 1));

            switch (unit) {
                case 'm':
                    return amount;
                case 'h':
                    return amount * 60L;
                case 'd':
                    return amount * 60L * 24;
                case 'w':
                    return amount * 60L * 24 * 7;
                default:
                    return -1;
            }
        } catch (Exception e) {
            return -1;
        }
    }

    public int getTotalBackupCount() {
        return backupHistory.values().stream().mapToInt(List::size).sum();
    }

    public String getBackupStatistics() {
        int totalAreas = backupHistory.size();
        int totalBackups = getTotalBackupCount();
        long totalFileSize = fileManager.getTotalBackupFileSize();

        return String.format("Areas with backups: %d | Total backups: %d | Storage used: %s",
                totalAreas, totalBackups, formatFileSize(totalFileSize));
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024)
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    public int getUndoPointer(String areaName) {
        List<AreaBackup> backups = backupHistory.get(areaName);
        if (backups == null)
            return -1;
        return undoPointers.getOrDefault(areaName, backups.size() - 1);
    }

    public boolean canUndo(String areaName) {
        return beforeRestoreBackups.containsKey(areaName);
    }

    public boolean canRedo(String areaName) {
        return false;
    }
}
