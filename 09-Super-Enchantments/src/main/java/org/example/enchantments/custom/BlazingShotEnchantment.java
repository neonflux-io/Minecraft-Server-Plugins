package org.example.enchantments.custom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Fireball;
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

public class BlazingShotEnchantment extends CustomEnchantment {

    private static final Map<UUID, Long> lastUsed = new HashMap<>();
    private final SuperEnchantments plugin;

    public BlazingShotEnchantment(SuperEnchantments plugin) {
        super("blazing_shot", "Blazing Shot", 5);
        this.plugin = plugin;
    }

    @Override
    public void onTrigger(Player player, ItemStack item, int level, Event event) {
        if (event instanceof PlayerInteractEvent) {
            PlayerInteractEvent interactEvent = (PlayerInteractEvent) event;
            if (interactEvent.getAction().name().contains("RIGHT_CLICK")) {
                UUID playerId = player.getUniqueId();
                long currentTime = System.currentTimeMillis();
                long cooldown = (plugin.getConfig().getInt("blazingshot.cooldown-base", 8) - level * plugin.getConfig().getInt("blazingshot.cooldown-reduction", 2)) * 1000L;

                if (lastUsed.containsKey(playerId) && currentTime - lastUsed.get(playerId) < cooldown) {
                    if (plugin.isCooldownDisplayEnabled()) {
                        double remainingTime = (cooldown - (currentTime - lastUsed.get(playerId))) / 1000.0;
                        player.sendMessage("Blazing Shot is on cooldown (" + String.format("%.1f", remainingTime) + "s)");
                    }
                    return;
                }

                lastUsed.put(playerId, currentTime);
                fireFireball(player, level);
                interactEvent.setCancelled(true);
            }
        }
    }

    private void fireFireball(Player player, int level) {
        Vector direction = player.getEyeLocation().getDirection().normalize();

        int fireballCount = plugin.getConfig().getInt("blazingshot.fireball-count-base", 1) + ((level - 1) / 2);
        float yield = (float) (plugin.getConfig().getDouble("blazingshot.yield-base", 0.5) + (level - 1) * plugin.getConfig().getDouble("blazingshot.yield-increase", 0.4));
        double velocityMultiplier = plugin.getConfig().getDouble("blazingshot.velocity-base", 1.2) + (level - 1) * plugin.getConfig().getDouble("blazingshot.velocity-increase", 0.15);
        double spawnDistance = 4.0;

        for (int i = 0; i < fireballCount; i++) {
            Vector fireballDirection = direction.clone();

            if (i > 0) {
                double spread = plugin.getConfig().getDouble("blazingshot.spread-base", 0.2) + (level - 1) * plugin.getConfig().getDouble("blazingshot.spread-reduction", 0.05);
                fireballDirection.add(new Vector(
                        (Math.random() - 0.5) * spread,
                        (Math.random() - 0.5) * spread * 0.3,
                        (Math.random() - 0.5) * spread
                ));
                fireballDirection = fireballDirection.normalize();
            }

            Location spawnLocation = player.getEyeLocation().add(fireballDirection.clone().multiply(spawnDistance));
            Fireball fireball = player.getWorld().spawn(spawnLocation, Fireball.class);
            fireball.setShooter(player);
            fireball.setDirection(fireballDirection);
            fireball.setYield(yield);
            fireball.setIsIncendiary(level >= 3);

            Vector velocity = fireballDirection.multiply(velocityMultiplier);
            fireball.setVelocity(velocity);
        }
    }

    @Override
    public boolean canApplyTo(ItemStack item) {
        Material type = item.getType();
        return type.name().contains("SWORD") || type.name().contains("AXE") ||
                type.name().contains("PICKAXE") || type.name().contains("SHOVEL") ||
                type.name().contains("HOE") || type == Material.STICK ||
                type == Material.BLAZE_ROD;
    }
}