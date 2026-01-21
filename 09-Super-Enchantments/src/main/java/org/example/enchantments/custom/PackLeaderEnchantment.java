package org.example.enchantments.custom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.example.SuperEnchantments;
import org.example.enchantments.CustomEnchantment;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class PackLeaderEnchantment extends CustomEnchantment {

    private final SuperEnchantments plugin;
    private final Random random = new Random();
    private static final Map<UUID, Long> lastUsed = new HashMap<>();

    public PackLeaderEnchantment(SuperEnchantments plugin) {
        super("pack_leader", "Pack Leader", 3);
        this.plugin = plugin;
    }

    @Override
    public void onTrigger(Player player, ItemStack item, int level, Event event) {
        if (event instanceof PlayerInteractEvent) {
            PlayerInteractEvent interactEvent = (PlayerInteractEvent) event;
            if (interactEvent.getAction().name().contains("RIGHT_CLICK")) {
                UUID playerId = player.getUniqueId();
                long currentTime = System.currentTimeMillis();
                long cooldown = (plugin.getConfig().getInt("packleader.cooldown-base", 15) - level * plugin.getConfig().getInt("packleader.cooldown-reduction", 3)) * 1000L;

                if (lastUsed.containsKey(playerId) && currentTime - lastUsed.get(playerId) < cooldown) {
                    if (plugin.isCooldownDisplayEnabled()) {
                        double remainingTime = (cooldown - (currentTime - lastUsed.get(playerId))) / 1000.0;
                        player.sendMessage("Pack Leader is on cooldown (" + String.format("%.1f", remainingTime) + "s)");
                    }
                    return;
                }

                lastUsed.put(playerId, currentTime);
                summonWolves(player, level);
                interactEvent.setCancelled(true);
            }
        }
    }

    private void summonWolves(Player player, int level) {
        int maxWolfCount = plugin.getConfig().getInt("packleader.wolf-count-base", 1) + (level - 1) * plugin.getConfig().getInt("packleader.wolf-count-increase", 1);
        int currentWolfCount = countPlayerWolves(player);
        int wolvesToSpawn = Math.max(0, maxWolfCount - currentWolfCount);

        if (wolvesToSpawn == 0) {
            player.sendMessage("You already have the maximum number of wolves!");
            return;
        }

        Location spawnLocation = player.getLocation();

        for (int i = 0; i < wolvesToSpawn; i++) {
            Vector offset = new Vector(
                    (random.nextDouble() - 0.5) * 4,
                    0,
                    (random.nextDouble() - 0.5) * 4
            );

            Location wolfSpawn = spawnLocation.clone().add(offset);
            wolfSpawn.setY(spawnLocation.getY());

            Wolf wolf = player.getWorld().spawn(wolfSpawn, Wolf.class);
            wolf.setOwner(player);
            wolf.setTamed(true);
            wolf.setAdult();
            wolf.setHealth(wolf.getMaxHealth());
            PotionEffectType speed = getPotionEffectType("SPEED");
            PotionEffectType strength = getPotionEffectType("STRENGTH");
            PotionEffectType resistance = getPotionEffectType("RESISTANCE");

            if (speed != null) {
                wolf.addPotionEffect(new PotionEffect(speed, 1200, level - 1));
            }
            if (strength != null) {
                wolf.addPotionEffect(new PotionEffect(strength, 1200, level - 1));
            }
            if (resistance != null) {
                wolf.addPotionEffect(new PotionEffect(resistance, 1200, 1));
            }

            LivingEntity target = findNearestHostile(player, 15.0);
            if (target != null) {
                wolf.setTarget(target);
            }

            wolf.getWorld().spawnParticle(Particle.CLOUD, wolf.getLocation().add(0, 1, 0), 10);

            new BukkitRunnable() {
                private int ticks = 0;

                @Override
                public void run() {
                    if (!wolf.isValid() || wolf.isDead() || ticks > 1200) {
                        if (wolf.isValid()) {
                            wolf.getWorld().spawnParticle(Particle.CLOUD, wolf.getLocation().add(0, 1, 0), 15);
                            wolf.remove();
                        }
                        cancel();
                        return;
                    }
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_HOWL, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0), 20);
    }

    private int countPlayerWolves(Player player) {
        int count = 0;
        for (org.bukkit.entity.Entity entity : player.getWorld().getEntities()) {
            if (entity instanceof Wolf) {
                Wolf wolf = (Wolf) entity;
                if (wolf.getOwner() != null && wolf.getOwner().equals(player)) {
                    count++;
                }
            }
        }
        return count;
    }

    private LivingEntity findNearestHostile(Player player, double radius) {
        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (org.bukkit.entity.Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
            if (entity instanceof LivingEntity && !(entity instanceof Player) && !(entity instanceof Wolf) && entity != player) {
                double distance = entity.getLocation().distance(player.getLocation());
                if (distance < nearestDistance) {
                    nearest = (LivingEntity) entity;
                    nearestDistance = distance;
                }
            }
        }

        return nearest;
    }

    @Override
    public boolean canApplyTo(ItemStack item) {
        Material type = item.getType();
        return type == Material.BONE || type == Material.STICK ||
                type.name().contains("SWORD") || type.name().contains("AXE");
    }

    private PotionEffectType getPotionEffectType(String name) {
        try {
            return (PotionEffectType) PotionEffectType.class.getField(name).get(null);
        } catch (Exception e) {
            return null;
        }
    }
}