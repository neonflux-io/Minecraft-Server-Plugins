package org.example.enchantments.custom;

import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.example.SuperEnchantments;
import org.example.enchantments.CustomEnchantment;

public class ExplosiveTipEnchantment extends CustomEnchantment {

    private final SuperEnchantments plugin;

    public ExplosiveTipEnchantment(SuperEnchantments plugin) {
        super("explosive_tip", "Explosive Tip", 4);
        this.plugin = plugin;
    }

    @Override
    public void onTrigger(Player player, ItemStack item, int level, Event event) {
        if (event instanceof EntityShootBowEvent) {
            EntityShootBowEvent shootEvent = (EntityShootBowEvent) event;
            if (shootEvent.getProjectile() instanceof Arrow) {
                Arrow arrow = (Arrow) shootEvent.getProjectile();
                arrow.setMetadata("explosive_level", new FixedMetadataValue(plugin, level));

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!arrow.isValid() || arrow.isDead()) {
                            cancel();
                            return;
                        }

                        if (arrow.isOnGround() || arrow.isInBlock()) {
                            explodeArrow(arrow, level);
                            arrow.remove();
                            cancel();
                        }
                    }
                }.runTaskTimer(plugin, 1L, 1L);
            }
        }

        if (event instanceof ProjectileHitEvent) {
            ProjectileHitEvent hitEvent = (ProjectileHitEvent) event;
            if (hitEvent.getEntity() instanceof Arrow) {
                Arrow arrow = (Arrow) hitEvent.getEntity();
                if (arrow.hasMetadata("explosive_level")) {
                    int explosiveLevel = arrow.getMetadata("explosive_level").get(0).asInt();
                    explodeArrow(arrow, explosiveLevel);
                }
            }
        }
    }

    private void explodeArrow(Arrow arrow, int level) {
        float power = (float) (plugin.getConfig().getDouble("explosivetip.power-base", 0.5) + (level - 1) * plugin.getConfig().getDouble("explosivetip.power-increase", 0.8));
        arrow.getWorld().createExplosion(arrow.getLocation(), power, false, false);
    }

    @Override
    public boolean canApplyTo(ItemStack item) {
        Material type = item.getType();
        return type == Material.BOW || type == Material.CROSSBOW;
    }
}