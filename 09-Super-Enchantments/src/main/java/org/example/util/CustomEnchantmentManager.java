package org.example.util;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.example.SuperEnchantments;
import org.example.enchantments.CustomEnchantment;
import org.example.enchantments.custom.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CustomEnchantmentManager {

    private final SuperEnchantments plugin;
    private final Map<String, CustomEnchantment> customEnchantments;

    public CustomEnchantmentManager(SuperEnchantments plugin) {
        this.plugin = plugin;
        this.customEnchantments = new HashMap<>();
        initializeCustomEnchantments();
    }

    private void initializeCustomEnchantments() {
        registerEnchantment(new ArrowStormEnchantment(plugin));
        registerEnchantment(new BlazingShotEnchantment(plugin));
        registerEnchantment(new PoisonEnchantment(plugin));
        registerEnchantment(new LightningEnchantment(plugin));
        registerEnchantment(new FrostBiteEnchantment(plugin));
        registerEnchantment(new VoidStepEnchantment(plugin));
        registerEnchantment(new ExplosiveTipEnchantment(plugin));
        registerEnchantment(new SoulDrainEnchantment(plugin));
        registerEnchantment(new PackLeaderEnchantment(plugin));
    }

    private void registerEnchantment(CustomEnchantment enchantment) {
        customEnchantments.put(enchantment.getName().toLowerCase(), enchantment);
    }

    public Optional<CustomEnchantment> getCustomEnchantment(String name) {
        return Optional.ofNullable(customEnchantments.get(name.toLowerCase()));
    }

    public List<String> getAllCustomEnchantmentNames() {
        return new ArrayList<>(customEnchantments.keySet());
    }

    public List<String> getAvailableCustomEnchantments(ItemStack item) {
        List<String> available = new ArrayList<>();

        for (CustomEnchantment enchantment : customEnchantments.values()) {
            if (enchantment.canApplyTo(item)) {
                available.add(enchantment.getName().toLowerCase());
            }
        }

        return available;
    }

    public ItemStack applyCustomEnchantment(ItemStack item, CustomEnchantment enchantment, int level) {
        if (item == null || item.getType().isAir()) {
            return item;
        }

        ItemStack result = item.clone();
        ItemMeta meta = result.getItemMeta();

        if (meta == null) {
            return result;
        }

        removeAllCustomEnchantments(result);
        meta = result.getItemMeta();

        if (meta == null) {
            return result;
        }

        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        dataContainer.set(
                plugin.getCustomEnchantmentKey(enchantment.getName()),
                PersistentDataType.INTEGER,
                level
        );

        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }

        String enchantmentLine = ChatColor.GRAY + enchantment.getDisplayName() + " " + getRomanNumeral(level);
        lore.add(enchantmentLine);

        meta.setLore(lore);
        result.setItemMeta(meta);

        return result;
    }

    private void removeAllCustomEnchantments(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();

        for (String enchantmentName : customEnchantments.keySet()) {
            dataContainer.remove(plugin.getCustomEnchantmentKey(enchantmentName));
        }

        List<String> lore = meta.getLore();
        if (lore != null) {
            List<String> newLore = new ArrayList<>();

            for (String line : lore) {
                String strippedLine = ChatColor.stripColor(line);
                boolean isCustomEnchantment = false;

                for (CustomEnchantment enchantment : customEnchantments.values()) {
                    if (strippedLine.startsWith(enchantment.getDisplayName())) {
                        isCustomEnchantment = true;
                        break;
                    }
                }

                if (!isCustomEnchantment) {
                    newLore.add(line);
                }
            }

            meta.setLore(newLore.isEmpty() ? null : newLore);
        }

        item.setItemMeta(meta);
    }

    public int getCustomEnchantmentLevel(ItemStack item, String enchantmentName) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }

        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        Integer level = dataContainer.get(
                plugin.getCustomEnchantmentKey(enchantmentName),
                PersistentDataType.INTEGER
        );

        return level != null ? level : 0;
    }

    public boolean hasCustomEnchantment(ItemStack item, String enchantmentName) {
        return getCustomEnchantmentLevel(item, enchantmentName) > 0;
    }

    public Map<String, Integer> getAllCustomEnchantments(ItemStack item) {
        Map<String, Integer> enchantments = new HashMap<>();

        if (item == null || item.getType().isAir()) {
            return enchantments;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return enchantments;
        }

        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();

        for (String enchantmentName : customEnchantments.keySet()) {
            Integer level = dataContainer.get(
                    plugin.getCustomEnchantmentKey(enchantmentName),
                    PersistentDataType.INTEGER
            );

            if (level != null && level > 0) {
                enchantments.put(enchantmentName, level);
            }
        }

        return enchantments;
    }

    private String getRomanNumeral(int number) {
        switch (number) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            default: return String.valueOf(number);
        }
    }
}
