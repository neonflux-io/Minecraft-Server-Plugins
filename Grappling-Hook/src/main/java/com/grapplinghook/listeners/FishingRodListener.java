package com.grapplinghook.listeners;

import com.grapplinghook.GrapplingHookPlugin;
import com.grapplinghook.managers.PlayerDataManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FishingRodListener implements Listener {

    private final GrapplingHookPlugin plugin;
    private final PlayerDataManager playerDataManager;
    private final Map<UUID, Long> lastPullTime;
    private static final long PULL_COOLDOWN_MS = 250;

    public FishingRodListener(GrapplingHookPlugin plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.lastPullTime = new HashMap<>();
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof FishHook)) {
            return;
        }

        FishHook hook = (FishHook) event.getEntity();
        
        if (!(hook.getShooter() instanceof Player)) {
            return;
        }

        Player player = (Player) hook.getShooter();
        
        if (!playerDataManager.isGrapplingHookEnabled(player)) {
            return;
        }

        if (!player.hasPermission("grapplinghook.use")) {
            return;
        }

        Vector direction = player.getLocation().getDirection();
        double velocityMultiplier = playerDataManager.getHookVelocity(player);
        hook.setVelocity(direction.multiply(velocityMultiplier));
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof FishHook)) {
            return;
        }

        FishHook hook = (FishHook) event.getEntity();
        
        if (!(hook.getShooter() instanceof Player)) {
            return;
        }

        Player player = (Player) hook.getShooter();
        
        if (!playerDataManager.isGrapplingHookEnabled(player)) {
            return;
        }

        if (!player.hasPermission("grapplinghook.use")) {
            return;
        }

        if (event.getHitBlock() == null) {
            return;
        }
        
        if (!checkCooldown(player)) {
            return;
        }

        Location hookLocation = hook.getLocation();
        Location playerLocation = player.getLocation();
        
        double distance = hookLocation.distance(playerLocation);
        double maxRange = playerDataManager.getRange(player);
        
        if (distance > maxRange) {
            player.sendMessage(ChatColor.RED + "Hook too far! Maximum range: " + maxRange + " blocks");
            return;
        }

        pullPlayerToHook(player, hookLocation);
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        
        if (!playerDataManager.isGrapplingHookEnabled(player)) {
            return;
        }

        if (!player.hasPermission("grapplinghook.use")) {
            return;
        }

        if (event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) {
            return;
        }
        
        if (!checkCooldown(player)) {
            return;
        }

        Entity caughtEntity = event.getCaught();
        
        if (caughtEntity != null && caughtEntity instanceof LivingEntity && !(caughtEntity instanceof Player)) {
            Location playerLocation = player.getLocation();
            double distance = caughtEntity.getLocation().distance(playerLocation);
            double maxRange = playerDataManager.getRange(player);
            
            if (distance > maxRange) {
                player.sendMessage(ChatColor.RED + "Entity too far! Maximum range: " + maxRange + " blocks");
                return;
            }
            
            pullEntityToPlayer(player, (LivingEntity) caughtEntity);
        }
    }
    
    private boolean checkCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        if (lastPullTime.containsKey(playerId)) {
            if (currentTime - lastPullTime.get(playerId) < PULL_COOLDOWN_MS) {
                return false;
            }
        }
        lastPullTime.put(playerId, currentTime);
        return true;
    }
    
    private void pullEntityToPlayer(Player player, LivingEntity entity) {
        Location playerLocation = player.getLocation();
        Location entityLocation = entity.getLocation();
        
        Vector direction = playerLocation.toVector().subtract(entityLocation.toVector());
        
        double pullSpeed = playerDataManager.getPullSpeed(player) * 1.5;
        direction.normalize().multiply(pullSpeed);
        
        double verticalBoost = playerDataManager.getVerticalBoost(player) * 1.2;
        direction.setY(direction.getY() + verticalBoost);
        
        entity.setVelocity(direction);
        
        if (playerDataManager.isSoundEnabled(player)) {
            entity.getWorld().playSound(entityLocation, org.bukkit.Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 0.8f);
        }
        
        if (playerDataManager.isParticlesEnabled(player)) {
            entity.getWorld().spawnParticle(
                org.bukkit.Particle.CRIT,
                entityLocation.add(0, entity.getHeight() / 2, 0),
                15,
                0.3, 0.3, 0.3,
                0.1
            );
        }
    }
    
    private void pullPlayerToHook(Player player, Location hookLocation) {
        Location playerLocation = player.getLocation();
        
        Vector direction = hookLocation.toVector().subtract(playerLocation.toVector());
        
        double pullSpeed = playerDataManager.getPullSpeed(player);
        direction.normalize().multiply(pullSpeed);
        
        double verticalBoost = playerDataManager.getVerticalBoost(player);
        direction.setY(direction.getY() + verticalBoost);
        
        player.setVelocity(direction);
        
        if (playerDataManager.isSoundEnabled(player)) {
            player.getWorld().playSound(playerLocation, org.bukkit.Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);
        }
        
        if (playerDataManager.isParticlesEnabled(player)) {
            player.getWorld().spawnParticle(
                org.bukkit.Particle.CLOUD,
                playerLocation,
                20,
                0.5, 0.5, 0.5,
                0.05
            );
        }
    }
}
