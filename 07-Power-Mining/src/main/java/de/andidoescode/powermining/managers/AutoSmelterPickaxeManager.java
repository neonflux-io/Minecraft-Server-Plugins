package de.andidoescode.powermining.managers;

import de.andidoescode.powermining.PowerMining;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class AutoSmelterPickaxeManager implements Listener {

    private final PowerMining plugin;
    private final NamespacedKey pickaxeKey;

    private static final Map<Material, Material> SMELT_MAP = new HashMap<>();

    static {
        SMELT_MAP.put(Material.IRON_ORE, Material.IRON_INGOT);
        SMELT_MAP.put(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT);
        SMELT_MAP.put(Material.GOLD_ORE, Material.GOLD_INGOT);
        SMELT_MAP.put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT);
        SMELT_MAP.put(Material.NETHER_GOLD_ORE, Material.GOLD_INGOT);
        SMELT_MAP.put(Material.COPPER_ORE, Material.COPPER_INGOT);
        SMELT_MAP.put(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT);
        SMELT_MAP.put(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP);
        SMELT_MAP.put(Material.COBBLESTONE, Material.STONE);
        SMELT_MAP.put(Material.STONE, Material.SMOOTH_STONE);
        SMELT_MAP.put(Material.SAND, Material.GLASS);
        SMELT_MAP.put(Material.RED_SAND, Material.GLASS);
        SMELT_MAP.put(Material.CLAY, Material.TERRACOTTA);
        SMELT_MAP.put(Material.NETHERRACK, Material.NETHER_BRICK);
        SMELT_MAP.put(Material.WET_SPONGE, Material.SPONGE);
        SMELT_MAP.put(Material.COBBLED_DEEPSLATE, Material.DEEPSLATE);
    }

    public AutoSmelterPickaxeManager(PowerMining plugin) {
        this.plugin = plugin;
        this.pickaxeKey = new NamespacedKey(plugin, "auto_smelter_pickaxe");
    }

    public ItemStack createAutoSmelterPickaxe() {
        ItemStack pickaxe = new ItemStack(Material.GOLDEN_PICKAXE);
        ItemMeta meta = pickaxe.getItemMeta();

        meta.displayName(Component.text("Smelter's Pickaxe")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        meta.lore(Arrays.asList(
                Component.empty(),
                Component.text("A pickaxe infused with fire!")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("✦ ")
                        .color(NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("Auto-smelts ores on mining")
                                .color(NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false)),
                Component.text("✦ ")
                        .color(NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("Iron, Gold, Copper → Ingots")
                                .color(NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false)),
                Component.text("✦ ")
                        .color(NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("Works with Fortune!")
                                .color(NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false)),
                Component.empty(),
                Component.text("The flames of the forge in your hands!")
                        .color(NamedTextColor.DARK_PURPLE)
                        .decoration(TextDecoration.ITALIC, true)
        ));

        meta.addEnchant(Enchantment.EFFICIENCY, 3, true);
        meta.addEnchant(Enchantment.UNBREAKING, 2, true);

        meta.getPersistentDataContainer().set(pickaxeKey, PersistentDataType.BYTE, (byte) 1);
        pickaxe.setItemMeta(meta);

        return pickaxe;
    }

    public boolean isAutoSmelterPickaxe(ItemStack item) {
        if (item == null || item.getType() != Material.GOLDEN_PICKAXE) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(pickaxeKey, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (!isAutoSmelterPickaxe(tool)) {
            return;
        }

        if (!player.hasPermission("powermining.use.autosmelterpickaxe")) {
            return;
        }

        Block block = event.getBlock();
        Material blockType = block.getType();

        if (!SMELT_MAP.containsKey(blockType)) {
            return;
        }

        Material smeltedMaterial = SMELT_MAP.get(blockType);

        event.setDropItems(false);

        int fortuneLevel = tool.getEnchantmentLevel(Enchantment.FORTUNE);
        int dropAmount = calculateDropAmount(blockType, fortuneLevel);

        block.getWorld().dropItemNaturally(
                block.getLocation().add(0.5, 0.5, 0.5),
                new ItemStack(smeltedMaterial, dropAmount)
        );

        block.getWorld().spawnParticle(
                Particle.FLAME,
                block.getLocation().add(0.5, 0.5, 0.5),
                8, 0.3, 0.3, 0.3, 0.02
        );

        player.playSound(block.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.5f, 1.5f);

        ItemMeta meta = tool.getItemMeta();
        if (meta instanceof Damageable damageable) {
            int unbreakingLevel = tool.getEnchantmentLevel(Enchantment.UNBREAKING);
            if (Math.random() < (1.0 / (unbreakingLevel + 1))) {
                damageable.setDamage(damageable.getDamage() + 1);
                tool.setItemMeta(meta);

                if (damageable.getDamage() >= tool.getType().getMaxDurability()) {
                    player.getInventory().setItemInMainHand(null);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                }
            }
        }
    }

    private int calculateDropAmount(Material blockType, int fortuneLevel) {
        int baseAmount = 1;

        if (blockType == Material.COPPER_ORE || blockType == Material.DEEPSLATE_COPPER_ORE) {
            baseAmount = 2 + (int) (Math.random() * 3);
        }

        if (fortuneLevel > 0) {
            int bonus = (int) (Math.random() * (fortuneLevel + 1));
            baseAmount += bonus;
        }

        return baseAmount;
    }

    public void shutdown() {
    }
}
