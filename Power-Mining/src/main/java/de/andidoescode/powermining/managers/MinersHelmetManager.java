package de.andidoescode.powermining.managers;

import de.andidoescode.powermining.PowerMining;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MinersHelmetManager implements Listener {

    private final PowerMining plugin;
    private final NamespacedKey helmetKey;
    private final Map<UUID, BukkitTask> activeEffects = new HashMap<>();

    public MinersHelmetManager(PowerMining plugin) {
        this.plugin = plugin;
        this.helmetKey = new NamespacedKey(plugin, "miners_helmet");
        startHelmetChecker();
    }

    public ItemStack createMinersHelmet() {
        ItemStack helmet = new ItemStack(Material.GOLDEN_HELMET);
        ItemMeta meta = helmet.getItemMeta();

        meta.displayName(Component.text("Miner's Helmet")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        meta.lore(Arrays.asList(
                Component.empty(),
                Component.text("A special helmet for miners!")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("✦ ")
                        .color(NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("Grants Night Vision when worn")
                                .color(NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false)),
                Component.empty(),
                Component.text("See clearly in the darkest caves!")
                        .color(NamedTextColor.DARK_PURPLE)
                        .decoration(TextDecoration.ITALIC, true)
        ));

        meta.getPersistentDataContainer().set(helmetKey, PersistentDataType.BYTE, (byte) 1);
        helmet.setItemMeta(meta);

        return helmet;
    }

    public boolean isMinersHelmet(ItemStack item) {
        if (item == null || item.getType() != Material.GOLDEN_HELMET) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(helmetKey, PersistentDataType.BYTE);
    }

    private void startHelmetChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    checkAndApplyEffect(player);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void checkAndApplyEffect(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();

        if (isMinersHelmet(helmet)) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.NIGHT_VISION,
                    400,
                    0,
                    true,
                    false,
                    true
            ));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> checkAndApplyEffect(player), 1L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> checkAndApplyEffect(event.getPlayer()), 20L);
    }

    public void shutdown() {
        activeEffects.values().forEach(BukkitTask::cancel);
        activeEffects.clear();
    }
}
