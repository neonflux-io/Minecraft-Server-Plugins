package de.railboost;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.util.Vector;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.Map;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class MinecartController implements Listener {

    private final RailBoostPlugin plugin;
    private final Map<UUID, BossBar> speedometers = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastEffectTime = new ConcurrentHashMap<>();
    private final Map<String, org.bukkit.inventory.Inventory> presetStorages = new ConcurrentHashMap<>();
    private final Map<UUID, Vector> lastDirection = new ConcurrentHashMap<>();
    private final Map<UUID, Vector> lastValidVelocity = new ConcurrentHashMap<>();

    private static final long EFFECT_COOLDOWN = 100;
    private static final double[] SPEED_LEVELS = {0.4, 0.8, 1.2, 2.0, 3.0, 4.0};

    public MinecartController(RailBoostPlugin plugin) {
        this.plugin = plugin;
        startTasks();
    }

    private void startTasks() {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateSpeedometers();
                handleMagnetism();
            }
        }.runTaskTimer(plugin, 0, 5);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Minecart)) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() != Material.STICK) return;

        Minecart minecart = (Minecart) event.getRightClicked();
        MinecartSettings settings = getSettings(item);

        if (settings != null) {
            applySettings(minecart, settings);
            player.sendMessage("§aApplied preset '" + settings.getName() + "' to minecart!");
            player.sendMessage("§7Speed: " + getSpeedDescription(settings.getSpeed()));
            event.setCancelled(true);
        }
    }

    private String getSpeedDescription(int speed) {
        switch (speed) {
            case 1: return "Slow (0.4x)";
            case 2: return "Normal (0.8x)";
            case 3: return "Fast (1.2x)";
            case 4: return "Very Fast (2.0x)";
            case 5: return "Super Fast (3.0x)";
            case 6: return "Ultra Fast (4.0x)";
            default: return "Unknown";
        }
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!(event.getVehicle() instanceof Minecart)) return;
        Minecart cart = (Minecart) event.getVehicle();
        MinecartSettings settings = getMinecartSettings(cart);

        handleAutoPickup(cart, settings);
        handleChunkLoading(cart, settings);
        handleEffects(cart, settings);

        if (settings == null) return;

        double speedFactor = SPEED_LEVELS[settings.getSpeed() - 1];
        Vector velocity = cart.getVelocity();

        boolean isStraightAndSafe = isStraightAndSafeTrack(cart, velocity);

        if (isStraightAndSafe && velocity.lengthSquared() > 0.001) {
            cart.setMaxSpeed(speedFactor * 3.0);
            Vector direction = velocity.normalize();
            Vector boostedVelocity = direction.multiply(speedFactor);
            cart.setVelocity(boostedVelocity);
        } else {
            cart.setMaxSpeed(0.4);
        }
    }

    private boolean isStraightAndSafeTrack(Minecart cart, Vector velocity) {
        if (velocity.lengthSquared() < 0.005) {
            return false;
        }

        Location loc = cart.getLocation();
        Block railBlock = getRailBlock(loc);

        if (railBlock == null || !isRailBlock(railBlock.getType())) {
            return false;
        }

        if (railBlock.getType() == Material.DETECTOR_RAIL ||
                railBlock.getType() == Material.ACTIVATOR_RAIL) {
            return false;
        }

        if (!isStraightRailShape(railBlock)) {
            return false;
        }

        UUID cartId = cart.getUniqueId();
        Vector lastDir = lastDirection.get(cartId);

        if (lastDir != null) {
            double directionChange = lastDir.angle(velocity.normalize());
            if (directionChange > 0.2) {
                lastDirection.put(cartId, velocity.normalize());
                return false;
            }
        }

        lastDirection.put(cartId, velocity.normalize());

        if (hasSteepTrackAhead(loc, velocity)) {
            return false;
        }

        return railBlock.getType() == Material.RAIL || railBlock.getType() == Material.POWERED_RAIL;
    }

    private boolean isStraightRailShape(Block railBlock) {
        if (!isRailBlock(railBlock.getType())) return false;

        org.bukkit.block.data.BlockData data = railBlock.getBlockData();
        if (data instanceof org.bukkit.block.data.Rail) {
            org.bukkit.block.data.Rail railData = (org.bukkit.block.data.Rail) data;
            org.bukkit.block.data.Rail.Shape shape = railData.getShape();

            return shape == org.bukkit.block.data.Rail.Shape.NORTH_SOUTH ||
                    shape == org.bukkit.block.data.Rail.Shape.EAST_WEST;
        }

        return false;
    }

    private boolean hasSteepTrackAhead(Location loc, Vector velocity) {
        Vector direction = velocity.normalize();

        for (int i = 1; i <= 3; i++) {
            Location checkLoc = loc.clone().add(direction.multiply(i));
            Block checkBlock = getRailBlock(checkLoc);

            if (checkBlock != null) {
                if (!isStraightRailShape(checkBlock)) {
                    return true;
                }
            }
        }

        return false;
    }

    private Block getRailBlock(Location loc) {
        Block block = loc.getBlock();
        if (isRailBlock(block.getType())) {
            return block;
        }

        Block below = block.getRelative(BlockFace.DOWN);
        if (isRailBlock(below.getType())) {
            return below;
        }

        Block above = block.getRelative(BlockFace.UP);
        if (isRailBlock(above.getType())) {
            return above;
        }

        return null;
    }

    private boolean isRailBlock(Material material) {
        return material == Material.RAIL ||
                material == Material.POWERED_RAIL ||
                material == Material.DETECTOR_RAIL ||
                material == Material.ACTIVATOR_RAIL;
    }

    private boolean isOnCurve(Block railBlock) {
        if (!isRailBlock(railBlock.getType())) return false;

        org.bukkit.block.data.BlockData data = railBlock.getBlockData();
        if (data instanceof org.bukkit.block.data.Rail) {
            org.bukkit.block.data.Rail railData = (org.bukkit.block.data.Rail) data;
            org.bukkit.block.data.Rail.Shape shape = railData.getShape();
            return shape == org.bukkit.block.data.Rail.Shape.NORTH_EAST ||
                    shape == org.bukkit.block.data.Rail.Shape.NORTH_WEST ||
                    shape == org.bukkit.block.data.Rail.Shape.SOUTH_EAST ||
                    shape == org.bukkit.block.data.Rail.Shape.SOUTH_WEST;
        }
        return false;
    }

    private Vector getCurveDirection(Block railBlock, Vector currentVelocity) {
        if (!isRailBlock(railBlock.getType())) return currentVelocity.normalize();

        org.bukkit.block.data.BlockData data = railBlock.getBlockData();
        if (data instanceof org.bukkit.block.data.Rail) {
            org.bukkit.block.data.Rail railData = (org.bukkit.block.data.Rail) data;
            org.bukkit.block.data.Rail.Shape shape = railData.getShape();

            switch (shape) {
                case NORTH_EAST:
                    if (Math.abs(currentVelocity.getZ()) > Math.abs(currentVelocity.getX())) {
                        return new Vector(1, 0, 0);
                    } else {
                        return new Vector(0, 0, -1);
                    }
                case NORTH_WEST:
                    if (Math.abs(currentVelocity.getZ()) > Math.abs(currentVelocity.getX())) {
                        return new Vector(-1, 0, 0);
                    } else {
                        return new Vector(0, 0, -1);
                    }
                case SOUTH_EAST:
                    if (Math.abs(currentVelocity.getZ()) > Math.abs(currentVelocity.getX())) {
                        return new Vector(1, 0, 0);
                    } else {
                        return new Vector(0, 0, 1);
                    }
                case SOUTH_WEST:
                    if (Math.abs(currentVelocity.getZ()) > Math.abs(currentVelocity.getX())) {
                        return new Vector(-1, 0, 0);
                    } else {
                        return new Vector(0, 0, 1);
                    }
                default:
                    return currentVelocity.normalize();
            }
        }
        return currentVelocity.normalize();
    }

    private BlockFace getDominantBlockFace(Vector dir) {
        double x = dir.getX();
        double z = dir.getZ();
        if (Math.abs(x) > Math.abs(z)) {
            return x > 0 ? BlockFace.EAST : BlockFace.WEST;
        } else {
            return z > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
        }
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getVehicle() instanceof Minecart) || !(event.getEntered() instanceof Player)) return;

        Player player = (Player) event.getEntered();
        Minecart minecart = (Minecart) event.getVehicle();
        MinecartSettings settings = getMinecartSettings(minecart);

        if (settings != null && settings.isSpeedometer()) {
            createSpeedometer(player);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isInsideVehicle() && player.getVehicle().equals(minecart)) {
                    if (settings != null && settings.getSpeed() > 1) {
                        player.sendMessage("§aRailBoost active! Speed: " + getSpeedDescription(settings.getSpeed()));

                        if (settings.getSpeed() >= 5) {
                            player.sendMessage("§cWarning: High speed! Drive carefully in curves!");
                        }
                    }

                    if (minecart instanceof org.bukkit.entity.minecart.StorageMinecart) {
                        player.sendMessage("§7Press §eF §7to open storage");
                    } else if (minecart instanceof org.bukkit.entity.minecart.HopperMinecart) {
                        player.sendMessage("§7Press §eF §7to open hopper");
                    }
                }
            }
        }.runTaskLater(plugin, 10);
    }

    @EventHandler
    public void onPlayerSwapHandItems(org.bukkit.event.player.PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        if (player.isInsideVehicle() && player.getVehicle() instanceof Minecart) {
            Minecart minecart = (Minecart) player.getVehicle();

            if (minecart instanceof org.bukkit.entity.minecart.StorageMinecart) {
                org.bukkit.entity.minecart.StorageMinecart storage = (org.bukkit.entity.minecart.StorageMinecart) minecart;
                player.openInventory(storage.getInventory());
                event.setCancelled(true);
            } else if (minecart instanceof org.bukkit.entity.minecart.HopperMinecart) {
                org.bukkit.entity.minecart.HopperMinecart hopper = (org.bukkit.entity.minecart.HopperMinecart) minecart;
                player.openInventory(hopper.getInventory());
                event.setCancelled(true);
            }
        }
    }

    private void handleAutoPickup(Minecart minecart, MinecartSettings settings) {
        if (settings == null || !settings.isAutoPickup()) return;

        Collection<Entity> nearbyEntities = minecart.getWorld().getNearbyEntities(
                minecart.getLocation(), settings.getPickupRadius(), 2, settings.getPickupRadius()
        );

        Set<Material> blacklist = settings.getBlacklist();

        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof Item)) continue;

            Item item = (Item) entity;
            ItemStack stack = item.getItemStack();

            if (blacklist.contains(stack.getType())) continue;

            boolean pickedUp = false;

            if (minecart instanceof org.bukkit.entity.minecart.StorageMinecart) {
                org.bukkit.entity.minecart.StorageMinecart storage = (org.bukkit.entity.minecart.StorageMinecart) minecart;
                Map<Integer, ItemStack> leftover = storage.getInventory().addItem(stack);
                if (leftover.isEmpty()) {
                    item.remove();
                    pickedUp = true;
                }
            } else if (minecart instanceof org.bukkit.entity.minecart.HopperMinecart) {
                org.bukkit.entity.minecart.HopperMinecart hopper = (org.bukkit.entity.minecart.HopperMinecart) minecart;
                Map<Integer, ItemStack> leftover = hopper.getInventory().addItem(stack);
                if (leftover.isEmpty()) {
                    item.remove();
                    pickedUp = true;
                }
            } else {
                org.bukkit.inventory.Inventory presetStorage = getOrCreatePresetStorage(settings.getName());
                if (presetStorage != null) {
                    Map<Integer, ItemStack> leftover = presetStorage.addItem(stack);
                    if (leftover.isEmpty()) {
                        item.remove();
                        pickedUp = true;
                    }
                }
            }

            if (pickedUp) {
                minecart.getWorld().spawnParticle(Particle.VILLAGER_HAPPY,
                        item.getLocation(), 3, 0.2, 0.2, 0.2, 0.1);
            }
        }
    }

    private void handleChunkLoading(Minecart minecart, MinecartSettings settings) {
        if (settings == null || !settings.isChunkLoad()) return;

        Chunk chunk = minecart.getLocation().getChunk();
        chunk.setForceLoaded(true);

        new BukkitRunnable() {
            @Override
            public void run() {
                chunk.setForceLoaded(false);
            }
        }.runTaskLater(plugin, 100);
    }

    private void handleEffects(Minecart minecart, MinecartSettings settings) {
        if (settings == null || !settings.isEffects()) return;

        UUID id = minecart.getUniqueId();
        long now = System.currentTimeMillis();

        if (lastEffectTime.containsKey(id) && now - lastEffectTime.get(id) < EFFECT_COOLDOWN) return;
        lastEffectTime.put(id, now);

        Vector velocity = minecart.getVelocity();
        if (velocity.lengthSquared() > 0.01) {
            try {
                Particle particle = Particle.valueOf(settings.getEffectType());
                Vector offset = velocity.normalize().multiply(-0.5);

                int particleCount = 3 + (settings.getSpeed() * 2);

                minecart.getWorld().spawnParticle(particle,
                        minecart.getLocation().add(offset),
                        particleCount, 0.1, 0.1, 0.1, 0.01);
            } catch (IllegalArgumentException e) {
                settings.setEffectType("FLAME");
            }
        }
    }

    private void handleMagnetism() {
        for (World world : Bukkit.getWorlds()) {
            Collection<Minecart> minecarts = world.getEntitiesByClass(Minecart.class);

            for (Minecart magnetCart : minecarts) {
                MinecartSettings magnetSettings = getMinecartSettings(magnetCart);
                if (magnetSettings == null || !magnetSettings.isMagnet()) continue;

                for (Minecart otherCart : minecarts) {
                    if (magnetCart.equals(otherCart)) continue;

                    double distance = magnetCart.getLocation().distance(otherCart.getLocation());

                    if (distance < 5.0 && distance > 0.5) {
                        Vector direction = magnetCart.getLocation().toVector()
                                .subtract(otherCart.getLocation().toVector()).normalize();

                        double strength = 0.03 / Math.max(distance, 1.0);
                        Vector pull = direction.multiply(strength);

                        otherCart.setVelocity(otherCart.getVelocity().add(pull));

                    } else if (distance <= 0.5 && distance > 0.1) {
                        Vector direction = magnetCart.getLocation().toVector()
                                .subtract(otherCart.getLocation().toVector()).normalize();

                        Vector slowPull = direction.multiply(0.01);
                        otherCart.setVelocity(otherCart.getVelocity().add(slowPull));

                    } else if (distance <= 0.1) {
                        otherCart.setVelocity(new Vector(0, 0, 0));
                    }
                }
            }
        }
    }

    private void updateSpeedometers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            BossBar bar = speedometers.get(player.getUniqueId());

            if (player.isInsideVehicle() && player.getVehicle() instanceof Minecart) {
                Minecart minecart = (Minecart) player.getVehicle();
                MinecartSettings settings = getMinecartSettings(minecart);

                if (settings != null && settings.isSpeedometer()) {
                    if (bar == null) {
                        createSpeedometer(player);
                        bar = speedometers.get(player.getUniqueId());
                    }

                    double speed = minecart.getVelocity().length() * 20 * 3.6;
                    bar.setTitle("§bSpeed: " + String.format("%.1f", speed) + " km/h");
                    bar.setProgress(Math.min(speed / 200.0, 1.0));
                    bar.setVisible(true);

                    if (speed > 100) {
                        bar.setColor(BarColor.RED);
                    } else if (speed > 50) {
                        bar.setColor(BarColor.YELLOW);
                    } else {
                        bar.setColor(BarColor.BLUE);
                    }
                } else if (bar != null) {
                    bar.setVisible(false);
                }
            } else if (bar != null) {
                bar.setVisible(false);
            }
        }
    }

    private void createSpeedometer(Player player) {
        BossBar bar = Bukkit.createBossBar("Speedometer", BarColor.BLUE, BarStyle.SOLID);
        bar.addPlayer(player);
        speedometers.put(player.getUniqueId(), bar);
    }

    public void applySettings(Minecart minecart, MinecartSettings settings) {
        NamespacedKey key = new NamespacedKey(plugin, "settings");
        minecart.getPersistentDataContainer().set(key, PersistentDataType.STRING, settings.serialize());
    }

    public MinecartSettings getMinecartSettings(Minecart minecart) {
        NamespacedKey key = new NamespacedKey(plugin, "settings");
        String data = minecart.getPersistentDataContainer().get(key, PersistentDataType.STRING);

        if (data != null) {
            return MinecartSettings.deserialize(data);
        }
        return null;
    }

    public MinecartSettings getSettings(ItemStack item) {
        if (item.getType() != Material.STICK || !item.hasItemMeta()) return null;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, "preset");
        String data = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);

        if (data != null) {
            return MinecartSettings.deserialize(data);
        }
        return null;
    }

    public org.bukkit.inventory.Inventory getOrCreatePresetStorage(String presetName) {
        return presetStorages.computeIfAbsent(presetName.toLowerCase(),
                k -> Bukkit.createInventory(null, 54, "§6Shared Storage: " + presetName));
    }

    public org.bukkit.inventory.Inventory getOrCreateStorage(Minecart minecart) {
        MinecartSettings settings = getMinecartSettings(minecart);
        if (settings != null) {
            return getOrCreatePresetStorage(settings.getName());
        }
        return null;
    }

    public org.bukkit.inventory.Inventory getStorageForPreset(String presetName) {
        return getOrCreatePresetStorage(presetName);
    }

    public static String[] getSpeedNames() {
        return new String[]{"Slow", "Normal", "Fast", "Very Fast", "Super Fast", "Ultra Fast"};
    }
}