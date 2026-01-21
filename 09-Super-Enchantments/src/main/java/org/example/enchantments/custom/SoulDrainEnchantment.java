package org.example.enchantments.custom;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.example.SuperEnchantments;
import org.example.enchantments.CustomEnchantment;

import java.util.Random;

public class SoulDrainEnchantment extends CustomEnchantment {

    private final Random random = new Random();
    private final SuperEnchantments plugin;

    public SoulDrainEnchantment(SuperEnchantments plugin) {
        super("soul_drain", "Soul Drain", 5);
        this.plugin = plugin;
    }

    @Override
    public void onTrigger(Player player, ItemStack item, int level, Event event) {
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;

            if (damageEvent.getDamager() == player && damageEvent.getEntity() instanceof LivingEntity) {
                double chanceBase = plugin.getConfig().getDouble("soul_drain.chance-base", 0.15);
                double chanceIncrease = plugin.getConfig().getDouble("soul_drain.chance-increase", 0.10);
                double chance = chanceBase + (level * chanceIncrease);

                if (random.nextDouble() < chance) {
                    double damage = damageEvent.getFinalDamage();
                    double healingMultiplierBase = plugin.getConfig().getDouble("soul_drain.healing-multiplier-base", 0.20);
                    double healingMultiplierIncrease = plugin.getConfig().getDouble("soul_drain.healing-multiplier-increase", 0.10);
                    double healingMultiplier = healingMultiplierBase + (level * healingMultiplierIncrease);
                    double healing = damage * healingMultiplier;

                    if (player.getHealth() < player.getMaxHealth()) {
                        double newHealth = Math.min(player.getHealth() + healing, player.getMaxHealth());
                        player.setHealth(newHealth);

                        double particleCountBase = plugin.getConfig().getDouble("soul_drain.particle-count-base", 3.0);
                        double particleCountIncrease = plugin.getConfig().getDouble("soul_drain.particle-count-increase", 2.0);
                        int particleCount = (int)(particleCountBase + level * particleCountIncrease);

                        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), particleCount);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.5f, 1.5f);
                    }
                }
            }
        }
    }

    @Override
    public boolean canApplyTo(ItemStack item) {
        Material type = item.getType();
        return type.name().contains("SWORD") || type.name().contains("AXE") ||
                type == Material.STICK;
    }
}