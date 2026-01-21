package org.example.enchantments.custom;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.example.SuperEnchantments;
import org.example.enchantments.CustomEnchantment;

import java.util.Random;

public class PoisonEnchantment extends CustomEnchantment {

    private final Random random = new Random();
    private final SuperEnchantments plugin;

    public PoisonEnchantment(SuperEnchantments plugin) {
        super("venom_strike", "Venom Strike", 5);
        this.plugin = plugin;
    }

    @Override
    public void onTrigger(Player player, ItemStack item, int level, Event event) {
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;

            if (damageEvent.getDamager() == player && damageEvent.getEntity() instanceof LivingEntity) {
                double chanceBase = plugin.getConfig().getDouble("venom_strike.chance-base", 0.10);
                double chanceIncrease = plugin.getConfig().getDouble("venom_strike.chance-increase", 0.05);
                double chance = chanceBase + (level * chanceIncrease);

                if (random.nextDouble() < chance) {
                    LivingEntity target = (LivingEntity) damageEvent.getEntity();
                    poisonTarget(target, level);
                }
            }
        }
    }

    private void poisonTarget(LivingEntity target, int level) {
        double durationBase = plugin.getConfig().getDouble("venom_strike.duration-base", 3.0);
        double durationIncrease = plugin.getConfig().getDouble("venom_strike.duration-increase", 1.0);
        int duration = (int)((durationBase + level * durationIncrease) * 20);

        double intensityBase = plugin.getConfig().getDouble("venom_strike.intensity-base", 0.0);
        double intensityIncrease = plugin.getConfig().getDouble("venom_strike.intensity-increase", 1.0);
        int intensity = (int)(intensityBase + level * intensityIncrease);

        PotionEffectType poison = getPotionEffectType("POISON");
        if (poison != null) {
            target.addPotionEffect(new PotionEffect(poison, duration, intensity));
        }

        target.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, target.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1.0f, 1.5f);
    }

    @Override
    public boolean canApplyTo(ItemStack item) {
        Material type = item.getType();
        return type.name().contains("SWORD") || type.name().contains("AXE") ||
                type == Material.STICK || type == Material.SPIDER_EYE ||
                type == Material.FERMENTED_SPIDER_EYE;
    }

    private PotionEffectType getPotionEffectType(String name) {
        try {
            return (PotionEffectType) PotionEffectType.class.getField(name).get(null);
        } catch (Exception e) {
            return null;
        }
    }
}