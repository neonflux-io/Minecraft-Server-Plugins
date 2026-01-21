package org.example;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import java.util.Set;
import org.bukkit.event.EventPriority;

public class PistonCrusherListener implements Listener {
    private final Set<Material> whitelist;
    private double multiplier;
    private final PistonCrusherPlugin plugin;

    public PistonCrusherListener(PistonCrusherPlugin plugin, Set<Material> whitelist, double multiplier) {
        this.plugin = plugin;
        this.whitelist = whitelist;
        this.multiplier = multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public Set<Material> getWhitelist() {
        return whitelist;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        BlockFace direction = event.getDirection();

        for (Block block : event.getBlocks()) {
            if (block.getType() == plugin.getCrusherBlock()) {
                Block targetPosition = block.getRelative(direction);
                Block frontBlock = targetPosition.getRelative(direction);
                Material blockType = frontBlock.getType();
                if (blockType != Material.AIR && whitelist.contains(blockType)) {
                    breakAndDropBlock(frontBlock);
                }
                break;
            }
        }
    }

    private void breakAndDropBlock(Block block) {
        Material material = block.getType();
        if (material.isItem()) {
            Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);
            int amount = Math.max(1, (int)Math.round(multiplier));
            ItemStack drop = new ItemStack(material, amount);
            block.setType(Material.AIR, false);
            block.getWorld().dropItemNaturally(dropLoc, drop);
        } else {
            block.setType(Material.AIR, false);
        }
    }
}
