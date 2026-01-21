package org.example.enchantments;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

public abstract class CustomEnchantment {

    private final String name;
    private final String displayName;
    private final int maxLevel;

    public CustomEnchantment(String name, String displayName, int maxLevel) {
        this.name = name;
        this.displayName = displayName;
        this.maxLevel = maxLevel;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public abstract void onTrigger(Player player, ItemStack item, int level, Event event);

    public abstract boolean canApplyTo(ItemStack item);
}