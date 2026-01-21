package org.example.enchantments.custom;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
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

public class FrostBiteEnchantment extends CustomEnchantment {

    private final Random random = new Random();
    private final SuperEnchantments plugin;

    public FrostBiteEnchantment(SuperEnchantments plugin) {
        super("frostbite", "Frostbite", 5);
        this.plugin = plugin;
    }

    @Override
    public void onTrigger(Player player, ItemStack item, int level, Event event) {
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;

            if (damageEvent.getDamager() == player && damageEvent.getEntity() instanceof LivingEntity) {
                double chanceBase = plugin.getConfig().getDouble("frostbite.chance-base", 0.10);
                double chanceIncrease = plugin.getConfig().getDouble("frostbite.chance-increase", 0.08);
                double chance = chanceBase + (level * chanceIncrease);

                if (random.nextDouble() < chance) {
                    LivingEntity target = (LivingEntity) damageEvent.getEntity();
                    freezeTarget(target, level);
                }
            }
        }
    }

    private void freezeTarget(LivingEntity target, int level) {
        double durationBase = plugin.getConfig().getDouble("frostbite.duration-base", 1.5);
        double durationIncrease = plugin.getConfig().getDouble("frostbite.duration-increase", 0.5);
        int duration = (int)((durationBase + level * durationIncrease) * 20);

        double intensityBase = plugin.getConfig().getDouble("frostbite.intensity-base", 1.0);
        double intensityIncrease = plugin.getConfig().getDouble("frostbite.intensity-increase", 1.0);
        int intensity = (int)(intensityBase + level * intensityIncrease);

        PotionEffectType slowness = getPotionEffectType("SLOWNESS");
        PotionEffectType miningFatigue = getPotionEffectType("MINING_FATIGUE");

        if (slowness != null) {
            target.addPotionEffect(new PotionEffect(slowness, duration, Math.min(intensity, 4)));
        }
        if (miningFatigue != null && level >= 3) {
            target.addPotionEffect(new PotionEffect(miningFatigue, duration, Math.min(level - 2, 2)));
        }

        int particleCount = 15 + (level * 5);
        target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), particleCount);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
        target.setFreezeTicks(duration + (level * 20));
    }

    @Override
    public boolean canApplyTo(ItemStack item) {
        Material type = item.getType();
        return type.name().contains("SWORD") || type.name().contains("AXE") ||
                type == Material.TRIDENT || type == Material.ICE ||
                type == Material.BLUE_ICE || type == Material.PACKED_ICE;
    }

    private PotionEffectType getPotionEffectType(String name) {
        try {
            return (PotionEffectType) PotionEffectType.class.getField(name).get(null);
        } catch (Exception e) {
            return null;
        }
    }
}