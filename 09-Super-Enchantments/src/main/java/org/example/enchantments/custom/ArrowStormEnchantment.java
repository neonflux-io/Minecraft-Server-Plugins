package org.example.enchantments.custom;

import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.example.SuperEnchantments;
import org.example.enchantments.CustomEnchantment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArrowStormEnchantment extends CustomEnchantment {

    private static final Map<UUID, Long> lastUsed = new HashMap<>();
    private final SuperEnchantments plugin;

    public ArrowStormEnchantment(SuperEnchantments plugin) {
        super("arrow_storm", "Arrow Storm", 5);
        this.plugin = plugin;
    }

    @Override
    public void onTrigger(Player player, ItemStack item, int level, Event event) {
        if (event instanceof PlayerInteractEvent) {
            PlayerInteractEvent interactEvent = (PlayerInteractEvent) event;
            if (interactEvent.getAction().name().contains("RIGHT_CLICK")) {
                UUID playerId = player.getUniqueId();
                long currentTime = System.currentTimeMillis();
                long cooldown = (plugin.getConfig().getInt("arrowstorm.cooldown-base", 6) - level * plugin.getConfig().getInt("arrowstorm.cooldown-reduction", 1)) * 1000L;

                if (lastUsed.containsKey(playerId) && currentTime - lastUsed.get(playerId) < cooldown) {
                    if (plugin.isCooldownDisplayEnabled()) {
                        double remainingTime = (cooldown - (currentTime - lastUsed.get(playerId))) / 1000.0;
                        player.sendMessage("Arrow Storm is on cooldown (" + String.format("%.1f", remainingTime) + "s)");
                    }
                    return;
                }

                lastUsed.put(playerId, currentTime);
                fireBolt(player, level);
                interactEvent.setCancelled(true);
            }
        }
    }

    private void fireBolt(Player player, int level) {
        Vector direction = player.getEyeLocation().getDirection();

        int arrowCount = Math.min(plugin.getConfig().getInt("arrowstorm.arrow-count-base", 1) + (level - 1) * plugin.getConfig().getInt("arrowstorm.arrow-count-increase", 1), 5);
        float speed = (float) (plugin.getConfig().getDouble("arrowstorm.speed-base", 1.5) + (level - 1) * plugin.getConfig().getDouble("arrowstorm.speed-increase", 0.3));
        double damage = plugin.getConfig().getDouble("arrowstorm.damage-base", 2.0) + (level - 1) * plugin.getConfig().getDouble("arrowstorm.damage-increase", 0.8);

        for (int i = 0; i < arrowCount; i++) {
            Vector arrowDirection = direction.clone();
            if (level > 1) {
                double spread = plugin.getConfig().getDouble("arrowstorm.spread-base", 0.1) - (level - 1) * plugin.getConfig().getDouble("arrowstorm.spread-reduction", 0.05);
                arrowDirection.add(new Vector(
                        (Math.random() - 0.5) * spread,
                        (Math.random() - 0.5) * spread * 0.5,
                        (Math.random() - 0.5) * spread
                ));
            }

            Arrow arrow = player.getWorld().spawnArrow(
                    player.getEyeLocation().add(direction.clone().multiply(0.5)),
                    arrowDirection,
                    speed,
                    0.0f
            );
            arrow.setShooter(player);
            arrow.setDamage(damage);
            arrow.setCritical(level >= 4);
        }
    }

    @Override
    public boolean canApplyTo(ItemStack item) {
        Material type = item.getType();
        return type.name().contains("SWORD") || type.name().contains("AXE") ||
                type.name().contains("PICKAXE") || type.name().contains("SHOVEL") ||
                type.name().contains("HOE") || type == Material.STICK;
    }
}