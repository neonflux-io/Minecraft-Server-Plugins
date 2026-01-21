package de.andidoescode.powermining.managers;

import de.andidoescode.powermining.PowerMining;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MagnetHopperManager implements Listener {

    private final PowerMining plugin;
    private final NamespacedKey magnetHopperKey;
    private final NamespacedKey radiusKey;
    private final Map<Location, MagnetHopperData> activeMagnetHoppers = new HashMap<>();
    private BukkitTask attractionTask;
    private BukkitTask particleTask;

    public MagnetHopperManager(PowerMining plugin) {
        this.plugin = plugin;
        this.magnetHopperKey = new NamespacedKey(plugin, "magnet_hopper");
        this.radiusKey = new NamespacedKey(plugin, "magnet_radius");
        
        startAttractionTask();
        startParticleTask();
        loadExistingHoppers();
    }

    public void shutdown() {
        if (attractionTask != null) {
            attractionTask.cancel();
        }
        if (particleTask != null) {
            particleTask.cancel();
        }
        activeMagnetHoppers.clear();
    }

    private void loadExistingHoppers() {
    }

    private void startAttractionTask() {
        if (!plugin.getConfig().getBoolean("magnet-hopper.enabled", true)) {
            return;
        }

        int interval = plugin.getConfig().getInt("magnet-hopper.attraction-interval", 5);
        double speed = plugin.getConfig().getDouble("magnet-hopper.attraction-speed", 0.5);

        attractionTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<Location, MagnetHopperData> entry : new HashMap<>(activeMagnetHoppers).entrySet()) {
                    Location hopperLoc = entry.getKey();
                    MagnetHopperData data = entry.getValue();
                    
                    Block block = hopperLoc.getBlock();
                    if (block.getType() != Material.HOPPER) {
                        activeMagnetHoppers.remove(hopperLoc);
                        continue;
                    }

                    Location attractionPoint = hopperLoc.clone().add(0.5, 1.0, 0.5);
                    
                    hopperLoc.getWorld().getNearbyEntities(attractionPoint, data.radius, data.radius, data.radius).stream()
                        .filter(entity -> entity instanceof Item)
                        .map(entity -> (Item) entity)
                        .filter(item -> !item.isDead() && item.getPickupDelay() <= 0)
                        .forEach(item -> {
                            Location itemLoc = item.getLocation();
                            Vector direction = attractionPoint.toVector().subtract(itemLoc.toVector());
                            double distance = direction.length();
                            
                            if (distance > 0.5) {
                                direction.normalize().multiply(Math.min(speed, distance));
                                item.setVelocity(direction);
                            }
                        });
                }
            }
        }.runTaskTimer(plugin, 0L, interval);
    }

    private void startParticleTask() {
        if (!plugin.getConfig().getBoolean("magnet-hopper.particles.enabled", true)) {
            return;
        }

        int interval = plugin.getConfig().getInt("magnet-hopper.particles.interval", 10);
        String particleTypeName = plugin.getConfig().getString("magnet-hopper.particles.type", "SOUL_FIRE_FLAME");
        int particleCount = plugin.getConfig().getInt("magnet-hopper.particles.count", 3);
        
        Particle particleType;
        try {
            particleType = Particle.valueOf(particleTypeName);
        } catch (IllegalArgumentException e) {
            particleType = Particle.SOUL_FIRE_FLAME;
            plugin.getLogger().warning("Invalid particle type in config: " + particleTypeName + ", using SOUL_FIRE_FLAME");
        }
        
        final Particle finalParticleType = particleType;

        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Location hopperLoc : activeMagnetHoppers.keySet()) {
                    if (hopperLoc.getBlock().getType() != Material.HOPPER) {
                        continue;
                    }
                    
                    Location particleLoc = hopperLoc.clone().add(0.5, 1.2, 0.5);
                    hopperLoc.getWorld().spawnParticle(
                        finalParticleType,
                        particleLoc,
                        particleCount,
                        0.3, 0.3, 0.3,
                        0.02
                    );
                }
            }
        }.runTaskTimer(plugin, 0L, interval);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getConfig().getBoolean("magnet-hopper.enabled", true)) {
            return;
        }

        ItemStack item = event.getItemInHand();
        if (item.getType() != Material.HOPPER) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (!container.has(magnetHopperKey, PersistentDataType.BOOLEAN)) {
            return;
        }

        if (!event.getPlayer().hasPermission("powermining.use.magnethopper")) {
            return;
        }

        int radius = container.getOrDefault(radiusKey, PersistentDataType.INTEGER, 
            plugin.getConfig().getInt("magnet-hopper.default-radius", 8));

        Location loc = event.getBlock().getLocation();
        activeMagnetHoppers.put(loc, new MagnetHopperData(radius));
        
        event.getPlayer().sendMessage(Component.text("Magnet Hopper activated with radius ")
            .color(NamedTextColor.GREEN)
            .append(Component.text(radius).color(NamedTextColor.YELLOW))
            .append(Component.text(" blocks!").color(NamedTextColor.GREEN)));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        
        if (activeMagnetHoppers.containsKey(loc)) {
            MagnetHopperData data = activeMagnetHoppers.remove(loc);
            
            event.setDropItems(false);
            
            ItemStack magnetHopper = createMagnetHopper(data.radius);
            event.getBlock().getWorld().dropItemNaturally(loc, magnetHopper);
        }
    }

    public ItemStack createMagnetHopper(int radius) {
        int maxRadius = plugin.getConfig().getInt("magnet-hopper.max-radius", 32);
        radius = Math.min(radius, maxRadius);
        
        ItemStack hopper = new ItemStack(Material.HOPPER);
        ItemMeta meta = hopper.getItemMeta();
        
        if (meta != null) {
            meta.displayName(Component.text("Magnet Hopper")
                .color(NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("✦ Attracts nearby items")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("✦ Radius: ")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text(radius + " blocks")
                    .color(NamedTextColor.AQUA)));
            lore.add(Component.empty());
            lore.add(Component.text("Place to activate!")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
            
            meta.lore(lore);
            
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(magnetHopperKey, PersistentDataType.BOOLEAN, true);
            container.set(radiusKey, PersistentDataType.INTEGER, radius);
            
            hopper.setItemMeta(meta);
        }
        
        return hopper;
    }

    public boolean isMagnetHopper(ItemStack item) {
        if (item == null || item.getType() != Material.HOPPER) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        return meta.getPersistentDataContainer().has(magnetHopperKey, PersistentDataType.BOOLEAN);
    }

    private static class MagnetHopperData {
        final int radius;
        
        MagnetHopperData(int radius) {
            this.radius = radius;
        }
    }
}
