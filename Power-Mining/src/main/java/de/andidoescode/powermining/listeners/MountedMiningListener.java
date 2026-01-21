package de.andidoescode.powermining.listeners;

import de.andidoescode.powermining.PowerMining;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MountedMiningListener implements Listener {

    private final PowerMining plugin;
    private final Map<UUID, Long> lastMineTime = new HashMap<>();
    
    private static final Set<Material> PICKAXES = Set.of(
        Material.WOODEN_PICKAXE,
        Material.STONE_PICKAXE,
        Material.IRON_PICKAXE,
        Material.GOLDEN_PICKAXE,
        Material.DIAMOND_PICKAXE,
        Material.NETHERITE_PICKAXE
    );
    
    private static final Set<Material> UNBREAKABLE_BLOCKS = Set.of(
        Material.BEDROCK,
        Material.BARRIER,
        Material.COMMAND_BLOCK,
        Material.CHAIN_COMMAND_BLOCK,
        Material.REPEATING_COMMAND_BLOCK,
        Material.STRUCTURE_BLOCK,
        Material.STRUCTURE_VOID,
        Material.JIGSAW,
        Material.END_PORTAL_FRAME,
        Material.END_PORTAL,
        Material.NETHER_PORTAL,
        Material.END_GATEWAY,
        Material.SPAWNER
    );

    public MountedMiningListener(PowerMining plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("mounted-mining.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        
        if (!player.hasPermission("powermining.use.mountedmining")) {
            return;
        }

        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof AbstractHorse)) {
            return;
        }

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (!PICKAXES.contains(mainHand.getType())) {
            return;
        }

        long miningInterval = plugin.getConfig().getLong("mounted-mining.mining-interval", 5) * 50;
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastMineTime.get(player.getUniqueId());
        
        if (lastTime != null && currentTime - lastTime < miningInterval) {
            return;
        }

        lastMineTime.put(player.getUniqueId(), currentTime);

        mineBlocksAhead(player, (AbstractHorse) vehicle);
    }

    private void mineBlocksAhead(Player player, AbstractHorse mount) {
        int radiusX = plugin.getConfig().getInt("mounted-mining.radius-x", 1);
        int radiusY = plugin.getConfig().getInt("mounted-mining.radius-y", 1);
        int radiusZ = plugin.getConfig().getInt("mounted-mining.radius-z", 1);
        int yOffset = plugin.getConfig().getInt("mounted-mining.y-offset", 1);

        Location mountLoc = mount.getLocation();
        Vector direction = mountLoc.getDirection().normalize();
        
        Location centerLoc = mountLoc.clone().add(direction.multiply(2));
        centerLoc.setY(mountLoc.getY() + yOffset);

        int minY = mountLoc.getBlockY();

        for (int x = -radiusX; x <= radiusX; x++) {
            for (int y = -radiusY; y <= radiusY; y++) {
                for (int z = -radiusZ; z <= radiusZ; z++) {
                    Location blockLoc = centerLoc.clone().add(x, y, z);
                    
                    if (blockLoc.getBlockY() < minY) {
                        continue;
                    }

                    Block block = blockLoc.getBlock();
                    
                    if (canBreakBlock(block)) {
                        breakBlockWithPickaxe(player, block);
                    }
                }
            }
        }
    }

    private boolean canBreakBlock(Block block) {
        Material type = block.getType();
        
        if (type.isAir() || !type.isSolid()) {
            return false;
        }
        
        if (UNBREAKABLE_BLOCKS.contains(type)) {
            return false;
        }
        
        return type.getHardness() >= 0;
    }

    private void breakBlockWithPickaxe(Player player, Block block) {
        ItemStack pickaxe = player.getInventory().getItemInMainHand();
        
        if (!PICKAXES.contains(pickaxe.getType())) {
            return;
        }

        block.breakNaturally(pickaxe);
        
        if (pickaxe.getType().getMaxDurability() > 0) {
            var meta = pickaxe.getItemMeta();
            if (meta instanceof org.bukkit.inventory.meta.Damageable damageable) {
                int newDamage = damageable.getDamage() + 1;
                if (newDamage >= pickaxe.getType().getMaxDurability()) {
                    player.getInventory().setItemInMainHand(null);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                } else {
                    damageable.setDamage(newDamage);
                    pickaxe.setItemMeta(meta);
                }
            }
        }
    }
}
