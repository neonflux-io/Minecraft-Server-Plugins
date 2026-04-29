package com.example.advancedachievements.listeners;

import com.example.advancedachievements.AdvancedAchievements;
import com.example.advancedachievements.enums.TaskType;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class AchievementListener implements Listener {
    
    private final AdvancedAchievements plugin;
    
    public AchievementListener(AdvancedAchievements plugin) {
        this.plugin = plugin;
        startPlayTimeTask();
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getProgressManager().loadPlayerProgress(player);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getProgressManager().unloadPlayerProgress(player);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();
        
        plugin.getProgressManager().addProgress(player, TaskType.BLOCK_BREAK, blockType.name(), 1);
        plugin.getProgressManager().addProgress(player, TaskType.BLOCK_BREAK, "ANY", 1);
        
        if (isOre(blockType)) {
            plugin.getProgressManager().addProgress(player, TaskType.MINING, blockType.name(), 1);
            plugin.getProgressManager().addProgress(player, TaskType.MINING, "ANY", 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlockPlaced().getType();

        plugin.getProgressManager().addProgress(player, TaskType.BLOCK_PLACE, blockType.name(), 1);
        plugin.getProgressManager().addProgress(player, TaskType.BLOCK_PLACE, "ANY", 1);
    }
      @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(org.bukkit.event.entity.EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            ItemStack item = event.getItem().getItemStack();
            
            plugin.getProgressManager().addProgress(player, TaskType.ITEM_PICKUP, 
                item.getType().name(), item.getAmount());
            plugin.getProgressManager().addProgress(player, TaskType.ITEM_PICKUP, "ANY", item.getAmount());
            
            if (isLog(item.getType())) {
                plugin.getProgressManager().addProgress(player, TaskType.ITEM_PICKUP, "LOG", item.getAmount());
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() instanceof Player) {
            Player player = event.getEntity().getKiller();
            EntityType entityType = event.getEntity().getType();
            
            plugin.getProgressManager().addProgress(player, TaskType.MOB_KILL, entityType.name(), 1);
            plugin.getProgressManager().addProgress(player, TaskType.MOB_KILL, "ANY", 1);
            
            if (event.getEntity() instanceof Monster) {
                plugin.getProgressManager().addProgress(player, TaskType.MOB_KILL, "HOSTILE", 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            int damage = (int) Math.ceil(Math.max(0.0, event.getFinalDamage()));
            if (damage > 0) {
                plugin.getProgressManager().addProgress(player, TaskType.DAMAGE_DEALT, event.getEntity().getType().name(), damage);
                plugin.getProgressManager().addProgress(player, TaskType.DAMAGE_DEALT, "ANY", damage);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            int damage = (int) Math.ceil(Math.max(0.0, event.getFinalDamage()));
            if (damage > 0) {
                plugin.getProgressManager().addProgress(player, TaskType.DAMAGE_TAKEN, event.getCause().name(), damage);
                plugin.getProgressManager().addProgress(player, TaskType.DAMAGE_TAKEN, "ANY", damage);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            ItemStack result = event.getRecipe().getResult();
            
            int amount = result.getAmount();
            if (event.isShiftClick()) {
                amount = calculateShiftClickAmount(event);
            }
            
            plugin.getProgressManager().addProgress(player, TaskType.ITEM_CRAFT, 
                result.getType().name(), amount);
            plugin.getProgressManager().addProgress(player, TaskType.ITEM_CRAFT, "ANY", amount);
        }    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            Player player = event.getPlayer();
            if (event.getCaught() instanceof org.bukkit.entity.Item) {
                org.bukkit.entity.Item caught = (org.bukkit.entity.Item) event.getCaught();
                String itemName = caught.getItemStack().getType().name();
                plugin.getProgressManager().addProgress(player, TaskType.FISHING, itemName, 1);
            }
            plugin.getProgressManager().addProgress(player, TaskType.FISHING, "ANY", 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        Material smeltedType = event.getItemType();
        int amount = Math.max(1, event.getItemAmount());

        plugin.getProgressManager().addProgress(player, TaskType.ITEM_SMELT, smeltedType.name(), amount);
        plugin.getProgressManager().addProgress(player, TaskType.ITEM_SMELT, "ANY", amount);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        plugin.getProgressManager().addProgress(player, TaskType.EATING, item.getType().name(), 1);
        plugin.getProgressManager().addProgress(player, TaskType.EATING, "ANY", 1);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        plugin.getProgressManager().addProgress(player, TaskType.ENCHANTING, "ANY", 1);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getType() == InventoryType.MERCHANT && 
            event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            plugin.getProgressManager().addProgress(player, TaskType.TRADING, "ANY", 1);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityTame(EntityTameEvent event) {
        if (event.getOwner() instanceof Player) {
            Player player = (Player) event.getOwner();
            plugin.getProgressManager().addProgress(player, TaskType.TAMING, 
                event.getEntity().getType().name(), 1);
            plugin.getProgressManager().addProgress(player, TaskType.TAMING, "ANY", 1);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBreed(EntityBreedEvent event) {
        if (event.getBreeder() instanceof Player) {
            Player player = (Player) event.getBreeder();
            plugin.getProgressManager().addProgress(player, TaskType.BREEDING, 
                event.getEntity().getType().name(), 1);
            plugin.getProgressManager().addProgress(player, TaskType.BREEDING, "ANY", 1);
        }    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        plugin.getProgressManager().addProgress(player, TaskType.DEATH, "ANY", 1);
        plugin.getProgressManager().addProgress(player, TaskType.DEATH, player.getName(), 1);

        if (player.getKiller() != null) {
            Player killer = player.getKiller();
            plugin.getProgressManager().addProgress(killer, TaskType.PLAYER_KILL, "ANY", 1);
            plugin.getProgressManager().addProgress(killer, TaskType.PLAYER_KILL, player.getName(), 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (event.getFrom().getWorld() != event.getTo().getWorld()) return;

        double distance = event.getFrom().distance(event.getTo());
        if (distance <= 0) return;

        int centiBlocks = (int) Math.round(distance * 100.0);
        if (centiBlocks <= 0) return;

        Player player = event.getPlayer();
        plugin.getProgressManager().addProgress(player, TaskType.WALK_DISTANCE, "ANY", centiBlocks);
        plugin.getProgressManager().addProgress(player, TaskType.WALK_DISTANCE, player.getWorld().getName(), centiBlocks);
    }

    private void startPlayTimeTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    plugin.getProgressManager().addProgress(player, TaskType.PLAY_TIME, "ANY", 60);
                    plugin.getProgressManager().addProgress(player, TaskType.PLAY_TIME, player.getWorld().getName(), 60);
                }
            }
        }.runTaskTimer(plugin, 1200L, 1200L);
    }
    
    private boolean isOre(Material material) {
        return material.name().contains("_ORE") || 
               material == Material.COAL || 
               material == Material.DIAMOND || 
               material == Material.EMERALD ||
               material == Material.REDSTONE ||
               material == Material.LAPIS_LAZULI;
    }
    
    private boolean isLog(Material material) {
        return material.name().contains("_LOG") || material.name().contains("_WOOD");
    }
    
    private int calculateShiftClickAmount(CraftItemEvent event) {
        return event.getRecipe().getResult().getAmount();
    }
}
