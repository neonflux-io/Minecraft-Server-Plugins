package org.example.enchantments.custom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.example.SuperEnchantments;
import org.example.enchantments.CustomEnchantment;

import java.util.Random;

public class LightningEnchantment extends CustomEnchantment {

    private final Random random = new Random();
    private final SuperEnchantments plugin;

    public LightningEnchantment(SuperEnchantments plugin) {
        super("thunderbolt", "Thunderbolt", 5);
        this.plugin = plugin;
    }

    @Override
    public void onTrigger(Player player, ItemStack item, int level, Event event) {
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;

            if (damageEvent.getDamager() == player && damageEvent.getEntity() instanceof LivingEntity) {
                double chanceBase = plugin.getConfig().getDouble("thunderbolt.chance-base", 0.05);
                double chanceIncrease = plugin.getConfig().getDouble("thunderbolt.chance-increase", 0.03);
                double chance = chanceBase + (level * chanceIncrease);

                if (random.nextDouble() < chance) {
                    LivingEntity target = (LivingEntity) damageEvent.getEntity();
                    strikeLightning(target.getLocation(), level);
                }
            }
        }
    }

    private void strikeLightning(Location location, int level) {
        double strikeCountBase = plugin.getConfig().getDouble("thunderbolt.strike-count-base", 1.0);
        double strikeCountIncrease = plugin.getConfig().getDouble("thunderbolt.strike-count-increase", 0.5);
        int strikeCount = (int)(strikeCountBase + level * strikeCountIncrease);

        double spreadRadius = plugin.getConfig().getDouble("thunderbolt.spread-radius", 3.0);

        for (int i = 0; i < strikeCount; i++) {
            Location strikeLocation = location.clone();
            if (i > 0) {
                strikeLocation.add(
                        (random.nextDouble() - 0.5) * spreadRadius,
                        0,
                        (random.nextDouble() - 0.5) * spreadRadius
                );
            } else {
                strikeLocation.add(
                        (random.nextDouble() - 0.5) * (spreadRadius / 2),
                        0,
                        (random.nextDouble() - 0.5) * (spreadRadius / 2)
                );
            }

            strikeLocation.getWorld().strikeLightningEffect(strikeLocation);
        }
    }

    @Override
    public boolean canApplyTo(ItemStack item) {
        Material type = item.getType();
        return type.name().contains("SWORD") || type.name().contains("AXE") ||
                type == Material.TRIDENT || type == Material.STICK;
    }
}