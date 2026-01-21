package org.example;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import java.util.Set;

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

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        Block piston = event.getBlock();
        if (piston.getType() != Material.PISTON && piston.getType() != Material.STICKY_PISTON) return;

        BlockFace direction = event.getDirection();
        Block targetBlock = piston.getRelative(direction);
        Block crusherBlock = targetBlock.getRelative(direction);
        Material mat = targetBlock.getType();
        Material crusherMat = crusherBlock.getType();
        Material requiredCrusher = plugin.getCrusherBlock();

        if (whitelist.contains(mat) && mat != Material.AIR && crusherMat == requiredCrusher) {
            event.setCancelled(true);
            int amount = Math.max(1, (int) Math.round(multiplier));
            targetBlock.setType(Material.AIR);
            targetBlock.getWorld().dropItemNaturally(targetBlock.getLocation(), new ItemStack(mat, amount));
        }
    }

    @EventHandler
    public void onRedstone(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.PISTON && block.getType() != Material.STICKY_PISTON) return;
        if (event.getNewCurrent() <= 0) return;

        BlockFace[] faces = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};
        for (BlockFace face : faces) {
            Block target = block.getRelative(face);
            if (whitelist.contains(target.getType()) && target.getType() != Material.AIR) {
                target.setType(Material.AIR);
                target.getWorld().dropItemNaturally(target.getLocation(), new ItemStack(target.getType()));
                break;
            }
        }
    }

    private void breakAndDropBlock(Block block) {
        Material material = block.getType();
        if (material.isItem()) {
            Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);
            int amount = Math.max(1, (int) Math.round(multiplier));
            ItemStack drop = new ItemStack(material, amount);
            block.setType(Material.AIR, false);
            block.getWorld().dropItemNaturally(dropLoc, drop);
        } else {
            block.setType(Material.AIR, false);
        }
    }
}
