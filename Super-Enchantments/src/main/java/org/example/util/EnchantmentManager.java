package org.example.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class EnchantmentManager {

    private final Map<String, Enchantment> enchantmentMap;
    private final Map<String, String> enchantmentDisplayNames;
    private final Map<Material, Set<Enchantment>> itemEnchantments;

    public EnchantmentManager() {
        enchantmentMap = new HashMap<>();
        enchantmentDisplayNames = new HashMap<>();
        itemEnchantments = new HashMap<>();
        initializeEnchantments();
        setupItemEnchantmentMap();
    }

    private void initializeEnchantments() {
        for (Enchantment enchantment : Enchantment.values()) {
            String name = enchantment.getKey().getKey();
            String normalizedName = normalizeEnchantmentName(name);
            enchantmentMap.put(normalizedName.toLowerCase(), enchantment);
            enchantmentDisplayNames.put(normalizedName.toLowerCase(), normalizedName);
        }
    }

    private void setupItemEnchantmentMap() {
        Set<Enchantment> swordEnchants = new HashSet<>();
        addEnchantmentByName("sharpness", swordEnchants);
        addEnchantmentByName("smite", swordEnchants);
        addEnchantmentByName("bane_of_arthropods", swordEnchants);
        addEnchantmentByName("knockback", swordEnchants);
        addEnchantmentByName("fire_aspect", swordEnchants);
        addEnchantmentByName("looting", swordEnchants);
        addEnchantmentByName("sweeping_edge", swordEnchants);
        addEnchantmentByName("unbreaking", swordEnchants);
        addEnchantmentByName("mending", swordEnchants);
        addEnchantmentByName("vanishing_curse", swordEnchants);

        Set<Enchantment> axeEnchants = new HashSet<>();
        addEnchantmentByName("sharpness", axeEnchants);
        addEnchantmentByName("smite", axeEnchants);
        addEnchantmentByName("bane_of_arthropods", axeEnchants);
        addEnchantmentByName("efficiency", axeEnchants);
        addEnchantmentByName("unbreaking", axeEnchants);
        addEnchantmentByName("fortune", axeEnchants);
        addEnchantmentByName("silk_touch", axeEnchants);
        addEnchantmentByName("mending", axeEnchants);
        addEnchantmentByName("vanishing_curse", axeEnchants);

        Set<Enchantment> pickaxeEnchants = new HashSet<>();
        addEnchantmentByName("efficiency", pickaxeEnchants);
        addEnchantmentByName("unbreaking", pickaxeEnchants);
        addEnchantmentByName("fortune", pickaxeEnchants);
        addEnchantmentByName("silk_touch", pickaxeEnchants);
        addEnchantmentByName("mending", pickaxeEnchants);
        addEnchantmentByName("vanishing_curse", pickaxeEnchants);

        Set<Enchantment> shovelEnchants = new HashSet<>();
        addEnchantmentByName("efficiency", shovelEnchants);
        addEnchantmentByName("unbreaking", shovelEnchants);
        addEnchantmentByName("fortune", shovelEnchants);
        addEnchantmentByName("silk_touch", shovelEnchants);
        addEnchantmentByName("mending", shovelEnchants);
        addEnchantmentByName("vanishing_curse", shovelEnchants);

        Set<Enchantment> hoeEnchants = new HashSet<>();
        addEnchantmentByName("efficiency", hoeEnchants);
        addEnchantmentByName("unbreaking", hoeEnchants);
        addEnchantmentByName("fortune", hoeEnchants);
        addEnchantmentByName("silk_touch", hoeEnchants);
        addEnchantmentByName("mending", hoeEnchants);
        addEnchantmentByName("vanishing_curse", hoeEnchants);

        Set<Enchantment> shearsEnchants = new HashSet<>();
        addEnchantmentByName("efficiency", shearsEnchants);
        addEnchantmentByName("unbreaking", shearsEnchants);
        addEnchantmentByName("mending", shearsEnchants);

        Set<Enchantment> flintSteelEnchants = new HashSet<>();
        addEnchantmentByName("unbreaking", flintSteelEnchants);
        addEnchantmentByName("mending", flintSteelEnchants);

        Set<Enchantment> carrotStickEnchants = new HashSet<>();
        addEnchantmentByName("unbreaking", carrotStickEnchants);
        addEnchantmentByName("mending", carrotStickEnchants);

        Set<Enchantment> fungusStickEnchants = new HashSet<>();
        addEnchantmentByName("unbreaking", fungusStickEnchants);
        addEnchantmentByName("mending", fungusStickEnchants);

        Set<Enchantment> compassEnchants = new HashSet<>();
        addEnchantmentByName("vanishing_curse", compassEnchants);

        Set<Enchantment> bowEnchants = new HashSet<>();
        addEnchantmentByName("power", bowEnchants);
        addEnchantmentByName("punch", bowEnchants);
        addEnchantmentByName("flame", bowEnchants);
        addEnchantmentByName("infinity", bowEnchants);
        addEnchantmentByName("unbreaking", bowEnchants);
        addEnchantmentByName("mending", bowEnchants);
        addEnchantmentByName("vanishing_curse", bowEnchants);

        Set<Enchantment> crossbowEnchants = new HashSet<>();
        addEnchantmentByName("quick_charge", crossbowEnchants);
        addEnchantmentByName("piercing", crossbowEnchants);
        addEnchantmentByName("multishot", crossbowEnchants);
        addEnchantmentByName("unbreaking", crossbowEnchants);
        addEnchantmentByName("mending", crossbowEnchants);
        addEnchantmentByName("vanishing_curse", crossbowEnchants);

        Set<Enchantment> tridentEnchants = new HashSet<>();
        addEnchantmentByName("loyalty", tridentEnchants);
        addEnchantmentByName("channeling", tridentEnchants);
        addEnchantmentByName("riptide", tridentEnchants);
        addEnchantmentByName("impaling", tridentEnchants);
        addEnchantmentByName("unbreaking", tridentEnchants);
        addEnchantmentByName("mending", tridentEnchants);
        addEnchantmentByName("vanishing_curse", tridentEnchants);

        Set<Enchantment> allArmorEnchants = new HashSet<>();
        addEnchantmentByName("protection", allArmorEnchants);
        addEnchantmentByName("fire_protection", allArmorEnchants);
        addEnchantmentByName("blast_protection", allArmorEnchants);
        addEnchantmentByName("projectile_protection", allArmorEnchants);
        addEnchantmentByName("thorns", allArmorEnchants);
        addEnchantmentByName("binding_curse", allArmorEnchants);
        addEnchantmentByName("unbreaking", allArmorEnchants);
        addEnchantmentByName("mending", allArmorEnchants);
        addEnchantmentByName("vanishing_curse", allArmorEnchants);

        Set<Enchantment> helmetEnchants = new HashSet<>(allArmorEnchants);
        addEnchantmentByName("respiration", helmetEnchants);
        addEnchantmentByName("aqua_affinity", helmetEnchants);

        Set<Enchantment> chestplateEnchants = new HashSet<>(allArmorEnchants);

        Set<Enchantment> leggingsEnchants = new HashSet<>(allArmorEnchants);
        addEnchantmentByName("swift_sneak", leggingsEnchants);

        Set<Enchantment> bootsEnchants = new HashSet<>(allArmorEnchants);
        addEnchantmentByName("feather_falling", bootsEnchants);
        addEnchantmentByName("depth_strider", bootsEnchants);
        addEnchantmentByName("frost_walker", bootsEnchants);
        addEnchantmentByName("soul_speed", bootsEnchants);

        Set<Enchantment> turtleHelmetEnchants = new HashSet<>(helmetEnchants);

        Set<Enchantment> fishingRodEnchants = new HashSet<>();
        addEnchantmentByName("luck_of_the_sea", fishingRodEnchants);
        addEnchantmentByName("lure", fishingRodEnchants);
        addEnchantmentByName("unbreaking", fishingRodEnchants);
        addEnchantmentByName("mending", fishingRodEnchants);
        addEnchantmentByName("vanishing_curse", fishingRodEnchants);

        Set<Enchantment> shieldEnchants = new HashSet<>();
        addEnchantmentByName("unbreaking", shieldEnchants);
        addEnchantmentByName("mending", shieldEnchants);
        addEnchantmentByName("vanishing_curse", shieldEnchants);

        Set<Enchantment> elytraEnchants = new HashSet<>();
        addEnchantmentByName("unbreaking", elytraEnchants);
        addEnchantmentByName("mending", elytraEnchants);
        addEnchantmentByName("vanishing_curse", elytraEnchants);

        Set<Enchantment> bookEnchants = new HashSet<>();
        for (Enchantment enchantment : Enchantment.values()) {
            bookEnchants.add(enchantment);
        }
        itemEnchantments.put(Material.ENCHANTED_BOOK, bookEnchants);

        itemEnchantments.put(Material.WOODEN_SWORD, swordEnchants);
        itemEnchantments.put(Material.STONE_SWORD, swordEnchants);
        itemEnchantments.put(Material.IRON_SWORD, swordEnchants);
        itemEnchantments.put(Material.GOLDEN_SWORD, swordEnchants);
        itemEnchantments.put(Material.DIAMOND_SWORD, swordEnchants);
        itemEnchantments.put(Material.NETHERITE_SWORD, swordEnchants);

        itemEnchantments.put(Material.WOODEN_AXE, axeEnchants);
        itemEnchantments.put(Material.STONE_AXE, axeEnchants);
        itemEnchantments.put(Material.IRON_AXE, axeEnchants);
        itemEnchantments.put(Material.GOLDEN_AXE, axeEnchants);
        itemEnchantments.put(Material.DIAMOND_AXE, axeEnchants);
        itemEnchantments.put(Material.NETHERITE_AXE, axeEnchants);

        itemEnchantments.put(Material.WOODEN_PICKAXE, pickaxeEnchants);
        itemEnchantments.put(Material.STONE_PICKAXE, pickaxeEnchants);
        itemEnchantments.put(Material.IRON_PICKAXE, pickaxeEnchants);
        itemEnchantments.put(Material.GOLDEN_PICKAXE, pickaxeEnchants);
        itemEnchantments.put(Material.DIAMOND_PICKAXE, pickaxeEnchants);
        itemEnchantments.put(Material.NETHERITE_PICKAXE, pickaxeEnchants);

        itemEnchantments.put(Material.WOODEN_SHOVEL, shovelEnchants);
        itemEnchantments.put(Material.STONE_SHOVEL, shovelEnchants);
        itemEnchantments.put(Material.IRON_SHOVEL, shovelEnchants);
        itemEnchantments.put(Material.GOLDEN_SHOVEL, shovelEnchants);
        itemEnchantments.put(Material.DIAMOND_SHOVEL, shovelEnchants);
        itemEnchantments.put(Material.NETHERITE_SHOVEL, shovelEnchants);

        itemEnchantments.put(Material.WOODEN_HOE, hoeEnchants);
        itemEnchantments.put(Material.STONE_HOE, hoeEnchants);
        itemEnchantments.put(Material.IRON_HOE, hoeEnchants);
        itemEnchantments.put(Material.GOLDEN_HOE, hoeEnchants);
        itemEnchantments.put(Material.DIAMOND_HOE, hoeEnchants);
        itemEnchantments.put(Material.NETHERITE_HOE, hoeEnchants);

        itemEnchantments.put(Material.SHEARS, shearsEnchants);
        itemEnchantments.put(Material.FLINT_AND_STEEL, flintSteelEnchants);
        itemEnchantments.put(Material.CARROT_ON_A_STICK, carrotStickEnchants);
        itemEnchantments.put(Material.WARPED_FUNGUS_ON_A_STICK, fungusStickEnchants);
        itemEnchantments.put(Material.COMPASS, compassEnchants);

        itemEnchantments.put(Material.BOW, bowEnchants);
        itemEnchantments.put(Material.CROSSBOW, crossbowEnchants);
        itemEnchantments.put(Material.TRIDENT, tridentEnchants);

        itemEnchantments.put(Material.LEATHER_HELMET, helmetEnchants);
        itemEnchantments.put(Material.CHAINMAIL_HELMET, helmetEnchants);
        itemEnchantments.put(Material.IRON_HELMET, helmetEnchants);
        itemEnchantments.put(Material.GOLDEN_HELMET, helmetEnchants);
        itemEnchantments.put(Material.DIAMOND_HELMET, helmetEnchants);
        itemEnchantments.put(Material.NETHERITE_HELMET, helmetEnchants);
        itemEnchantments.put(Material.TURTLE_HELMET, turtleHelmetEnchants);

        itemEnchantments.put(Material.LEATHER_CHESTPLATE, chestplateEnchants);
        itemEnchantments.put(Material.CHAINMAIL_CHESTPLATE, chestplateEnchants);
        itemEnchantments.put(Material.IRON_CHESTPLATE, chestplateEnchants);
        itemEnchantments.put(Material.GOLDEN_CHESTPLATE, chestplateEnchants);
        itemEnchantments.put(Material.DIAMOND_CHESTPLATE, chestplateEnchants);
        itemEnchantments.put(Material.NETHERITE_CHESTPLATE, chestplateEnchants);

        itemEnchantments.put(Material.LEATHER_LEGGINGS, leggingsEnchants);
        itemEnchantments.put(Material.CHAINMAIL_LEGGINGS, leggingsEnchants);
        itemEnchantments.put(Material.IRON_LEGGINGS, leggingsEnchants);
        itemEnchantments.put(Material.GOLDEN_LEGGINGS, leggingsEnchants);
        itemEnchantments.put(Material.DIAMOND_LEGGINGS, leggingsEnchants);
        itemEnchantments.put(Material.NETHERITE_LEGGINGS, leggingsEnchants);

        itemEnchantments.put(Material.LEATHER_BOOTS, bootsEnchants);
        itemEnchantments.put(Material.CHAINMAIL_BOOTS, bootsEnchants);
        itemEnchantments.put(Material.IRON_BOOTS, bootsEnchants);
        itemEnchantments.put(Material.GOLDEN_BOOTS, bootsEnchants);
        itemEnchantments.put(Material.DIAMOND_BOOTS, bootsEnchants);
        itemEnchantments.put(Material.NETHERITE_BOOTS, bootsEnchants);

        itemEnchantments.put(Material.FISHING_ROD, fishingRodEnchants);
        itemEnchantments.put(Material.SHIELD, shieldEnchants);
        itemEnchantments.put(Material.ELYTRA, elytraEnchants);
    }

    private void addEnchantmentByName(String name, Set<Enchantment> enchantments) {
        for (Enchantment enchantment : Enchantment.values()) {
            if (enchantment.getKey().getKey().equals(name)) {
                enchantments.add(enchantment);
                return;
            }
        }
    }

    private String normalizeEnchantmentName(String name) {
        return name.toLowerCase().replace("_", "");
    }

    public List<String> getAvailableEnchantments(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return Collections.emptyList();
        }

        Set<Enchantment> availableEnchants = itemEnchantments.get(item.getType());

        if (availableEnchants != null) {
            return availableEnchants.stream()
                    .map(enchantment -> getDisplayName(enchantment))
                    .filter(Objects::nonNull)
                    .sorted()
                    .collect(Collectors.toList());
        }

        return enchantmentMap.values().stream()
                .filter(enchantment -> enchantment.canEnchantItem(item))
                .map(this::getDisplayName)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());
    }

    public List<String> getAllEnchantmentNames() {
        return new ArrayList<>(enchantmentDisplayNames.values());
    }

    public Optional<Enchantment> getEnchantmentByName(String name) {
        return Optional.ofNullable(enchantmentMap.get(name.toLowerCase()));
    }

    public String getDisplayName(Enchantment enchantment) {
        return enchantmentDisplayNames.getOrDefault(
                enchantment.getKey().getKey().toLowerCase(),
                normalizeEnchantmentName(enchantment.getKey().getKey())
        );
    }

    public ItemStack applyEnchantment(ItemStack item, Enchantment enchantment, int level) {
        if (item == null || item.getType() == Material.AIR) {
            return item;
        }

        ItemStack result = item.clone();

        if (item.getType() == Material.ENCHANTED_BOOK) {
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) result.getItemMeta();
            if (meta != null) {
                meta.addStoredEnchant(enchantment, level, true);
                result.setItemMeta(meta);
            }
        } else {
            ItemMeta meta = result.getItemMeta();
            if (meta != null) {
                meta.addEnchant(enchantment, level, true);
                result.setItemMeta(meta);
            }
        }
        return result;
    }

    public boolean isEnchantmentCombinationAllowed(ItemStack item, Set<Enchantment> enchantments) {
        if (item == null || enchantments == null || enchantments.size() <= 1) return true;
        Material type = item.getType();
        if (containsMoreThanOne(enchantments, "sharpness", "smite", "bane_of_arthropods")) return false;
        if (containsMoreThanOne(enchantments, "fortune", "silk_touch")) return false;
        if (type == Material.BOW && containsMoreThanOne(enchantments, "infinity", "mending")) return false;
        if (type == Material.CROSSBOW && containsMoreThanOne(enchantments, "multishot", "piercing")) return false;
        if (type == Material.TRIDENT && containsMoreThanOne(enchantments, "riptide", "loyalty", "channeling")) return false;
        if (isBoots(type) && containsMoreThanOne(enchantments, "depth_strider", "frost_walker")) return false;
        return true;
    }

    private boolean containsMoreThanOne(Set<Enchantment> set, String... names) {
        int count = 0;
        for (String name : names) {
            Optional<Enchantment> ench = getEnchantmentByName(name);
            if (ench.isPresent() && set.contains(ench.get())) count++;
        }
        return count > 1;
    }

    private boolean isBoots(Material type) {
        return type == Material.LEATHER_BOOTS || type == Material.CHAINMAIL_BOOTS || type == Material.IRON_BOOTS || type == Material.GOLDEN_BOOTS || type == Material.DIAMOND_BOOTS || type == Material.NETHERITE_BOOTS;
    }
}
