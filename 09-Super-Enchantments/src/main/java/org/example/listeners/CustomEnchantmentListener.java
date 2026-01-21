package org.example.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.example.SuperEnchantments;
import org.example.enchantments.CustomEnchantment;
import org.example.util.CustomEnchantmentManager;

import java.util.Map;
import java.util.Optional;

public class CustomEnchantmentListener implements Listener {

    private final SuperEnchantments plugin;
    private final CustomEnchantmentManager customEnchantmentManager;

    public CustomEnchantmentListener(SuperEnchantments plugin) {
        this.plugin = plugin;
        this.customEnchantmentManager = plugin.getCustomEnchantmentManager();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType().isAir()) {
            return;
        }

        Map<String, Integer> customEnchantments = customEnchantmentManager.getAllCustomEnchantments(item);

        for (Map.Entry<String, Integer> entry : customEnchantments.entrySet()) {
            String enchantmentName = entry.getKey();
            int level = entry.getValue();

            Optional<CustomEnchantment> customEnchantment = customEnchantmentManager.getCustomEnchantment(enchantmentName);
            if (customEnchantment.isPresent()) {
                customEnchantment.get().onTrigger(player, item, level, event);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            ItemStack item = player.getInventory().getItemInMainHand();

            if (item == null || item.getType().isAir()) {
                return;
            }

            Map<String, Integer> customEnchantments = customEnchantmentManager.getAllCustomEnchantments(item);

            for (Map.Entry<String, Integer> entry : customEnchantments.entrySet()) {
                String enchantmentName = entry.getKey();
                int level = entry.getValue();

                Optional<CustomEnchantment> customEnchantment = customEnchantmentManager.getCustomEnchantment(enchantmentName);
                if (customEnchantment.isPresent()) {
                    customEnchantment.get().onTrigger(player, item, level, event);
                }
            }
        }
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            ItemStack item = event.getBow();

            if (item == null || item.getType().isAir()) {
                return;
            }

            Map<String, Integer> customEnchantments = customEnchantmentManager.getAllCustomEnchantments(item);

            for (Map.Entry<String, Integer> entry : customEnchantments.entrySet()) {
                String enchantmentName = entry.getKey();
                int level = entry.getValue();

                Optional<CustomEnchantment> customEnchantment = customEnchantmentManager.getCustomEnchantment(enchantmentName);
                if (customEnchantment.isPresent()) {
                    customEnchantment.get().onTrigger(player, item, level, event);
                }
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity().getShooter() instanceof Player) {
            Player player = (Player) event.getEntity().getShooter();

            if (event.getEntity().hasMetadata("explosive_level")) {
                ItemStack item = player.getInventory().getItemInMainHand();

                if (item != null && !item.getType().isAir()) {
                    Map<String, Integer> customEnchantments = customEnchantmentManager.getAllCustomEnchantments(item);

                    for (Map.Entry<String, Integer> entry : customEnchantments.entrySet()) {
                        String enchantmentName = entry.getKey();
                        int level = entry.getValue();

                        Optional<CustomEnchantment> customEnchantment = customEnchantmentManager.getCustomEnchantment(enchantmentName);
                        if (customEnchantment.isPresent()) {
                            customEnchantment.get().onTrigger(player, item, level, event);
                        }
                    }
                }
            }
        }
    }
}