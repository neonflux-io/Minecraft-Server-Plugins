package org.example.enchantments.custom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.example.SuperEnchantments;
import org.example.enchantments.CustomEnchantment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VoidStepEnchantment extends CustomEnchantment {

    private static final Map<UUID, Long> lastUsed = new HashMap<>();
    private final SuperEnchantments plugin;

    public VoidStepEnchantment(SuperEnchantments plugin) {
        super("void_step", "Void Step", 3);
        this.plugin = plugin;
    }

    @Override
    public void onTrigger(Player player, ItemStack item, int level, Event event) {
        if (event instanceof PlayerInteractEvent) {
            PlayerInteractEvent interactEvent = (PlayerInteractEvent) event;
            if (interactEvent.getAction().name().contains("RIGHT_CLICK")) {
                UUID playerId = player.getUniqueId();
                long currentTime = System.currentTimeMillis();
                long cooldown = (plugin.getConfig().getInt("voidstep.cooldown-base", 10) - level * plugin.getConfig().getInt("voidstep.cooldown-reduction", 2)) * 1000L;

                if (lastUsed.containsKey(playerId) && currentTime - lastUsed.get(playerId) < cooldown) {
                    if (plugin.isCooldownDisplayEnabled()) {
                        double remainingTime = (cooldown - (currentTime - lastUsed.get(playerId))) / 1000.0;
                        player.sendMessage("Void Step is on cooldown (" + String.format("%.1f", remainingTime) + "s)");
                    }
                    return;
                }

                lastUsed.put(playerId, currentTime);
                teleportPlayer(player, level);
                interactEvent.setCancelled(true);
            }
        }
    }

    private void teleportPlayer(Player player, int level) {
        Vector direction = player.getEyeLocation().getDirection().normalize();
        double maxDistance = plugin.getConfig().getDouble("voidstep.distance-base", 5) + (level - 1) * plugin.getConfig().getDouble("voidstep.distance-increase", 5);

        Location startLocation = player.getLocation();
        Location targetLocation = null;

        for (double dist = 1.0; dist <= maxDistance; dist += 1.0) {
            Location checkLocation = startLocation.clone().add(direction.clone().multiply(dist));
            Location safeLoc = findSafeLocation(checkLocation);
            if (safeLoc != null) {
                targetLocation = safeLoc;
            }
        }

        if (targetLocation != null) {
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 20);
            player.teleport(targetLocation);
            player.getWorld().spawnParticle(Particle.PORTAL, targetLocation.add(0, 1, 0), 20);
            player.getWorld().playSound(targetLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        }
    }

    private Location findSafeLocation(Location target) {
        for (int y = target.getBlockY(); y >= target.getBlockY() - 10 && y > 0; y--) {
            Location check = target.clone();
            check.setY(y);

            if (check.getBlock().getType().isSolid() &&
                    check.clone().add(0, 1, 0).getBlock().getType().isAir() &&
                    check.clone().add(0, 2, 0).getBlock().getType().isAir()) {
                return check.add(0.5, 1, 0.5);
            }
        }
        return null;
    }

    @Override
    public boolean canApplyTo(ItemStack item) {
        Material type = item.getType();
        return type == Material.ENDER_PEARL || type == Material.CHORUS_FRUIT ||
                type == Material.STICK || type.name().contains("SWORD") ||
                type.name().contains("AXE");
    }
}