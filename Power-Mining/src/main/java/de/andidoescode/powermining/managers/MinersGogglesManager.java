package de.andidoescode.powermining.managers;

import de.andidoescode.powermining.PowerMining;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class MinersGogglesManager implements Listener {

    private final PowerMining plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final NamespacedKey gogglesKey;
    private final NamespacedKey radiusKey;
    private final NamespacedKey filterKey;

    private final Map<UUID, Set<BlockDisplay>> activeHighlights = new HashMap<>();

    private static final Map<Material, Color> ORE_COLORS = new LinkedHashMap<>();
    private static final Set<Material> ALL_ORES = new LinkedHashSet<>();

    static {
        ORE_COLORS.put(Material.DIAMOND_ORE, Color.fromRGB(0, 255, 255));
        ORE_COLORS.put(Material.DEEPSLATE_DIAMOND_ORE, Color.fromRGB(0, 255, 255));
        ORE_COLORS.put(Material.EMERALD_ORE, Color.fromRGB(0, 255, 0));
        ORE_COLORS.put(Material.DEEPSLATE_EMERALD_ORE, Color.fromRGB(0, 255, 0));
        ORE_COLORS.put(Material.ANCIENT_DEBRIS, Color.fromRGB(139, 69, 19));
        ORE_COLORS.put(Material.GOLD_ORE, Color.fromRGB(255, 215, 0));
        ORE_COLORS.put(Material.DEEPSLATE_GOLD_ORE, Color.fromRGB(255, 215, 0));
        ORE_COLORS.put(Material.NETHER_GOLD_ORE, Color.fromRGB(255, 215, 0));
        ORE_COLORS.put(Material.IRON_ORE, Color.fromRGB(210, 180, 140));
        ORE_COLORS.put(Material.DEEPSLATE_IRON_ORE, Color.fromRGB(210, 180, 140));
        ORE_COLORS.put(Material.COPPER_ORE, Color.fromRGB(184, 115, 51));
        ORE_COLORS.put(Material.DEEPSLATE_COPPER_ORE, Color.fromRGB(184, 115, 51));
        ORE_COLORS.put(Material.LAPIS_ORE, Color.fromRGB(0, 0, 255));
        ORE_COLORS.put(Material.DEEPSLATE_LAPIS_ORE, Color.fromRGB(0, 0, 255));
        ORE_COLORS.put(Material.REDSTONE_ORE, Color.fromRGB(255, 0, 0));
        ORE_COLORS.put(Material.DEEPSLATE_REDSTONE_ORE, Color.fromRGB(255, 0, 0));
        ORE_COLORS.put(Material.COAL_ORE, Color.fromRGB(64, 64, 64));
        ORE_COLORS.put(Material.DEEPSLATE_COAL_ORE, Color.fromRGB(64, 64, 64));
        ORE_COLORS.put(Material.NETHER_QUARTZ_ORE, Color.fromRGB(255, 255, 255));

        ALL_ORES.addAll(ORE_COLORS.keySet());
    }

    public MinersGogglesManager(PowerMining plugin) {
        this.plugin = plugin;
        this.gogglesKey = new NamespacedKey(plugin, "miners_goggles");
        this.radiusKey = new NamespacedKey(plugin, "miners_goggles_radius");
        this.filterKey = new NamespacedKey(plugin, "miners_goggles_filter");
        startGogglesScanner();
    }

    public ItemStack createMinersGoggles(int radius, String filter) {
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta meta = (LeatherArmorMeta) helmet.getItemMeta();

        meta.setColor(Color.fromRGB(139, 90, 43));

        String filterDisplay = filter == null ? "All Ores" : formatOreName(filter);

        meta.displayName(Component.text("Miner's Goggles")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        meta.lore(Arrays.asList(
                Component.empty(),
                Component.text("X-ray vision for miners!")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("✦ ")
                        .color(NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("Reveals nearby ores when worn")
                                .color(NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false)),
                Component.text("✦ ")
                        .color(NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("Shift + Right-click to cycle filter")
                                .color(NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false)),
                Component.empty(),
                Component.text("Radius: ")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(radius + " blocks")
                                .color(NamedTextColor.AQUA)
                                .decoration(TextDecoration.ITALIC, false)),
                Component.text("Filter: ")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(filterDisplay)
                                .color(NamedTextColor.GREEN)
                                .decoration(TextDecoration.ITALIC, false)),
                Component.empty(),
                Component.text("See through stone itself!")
                        .color(NamedTextColor.DARK_PURPLE)
                        .decoration(TextDecoration.ITALIC, true)
        ));

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(gogglesKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(radiusKey, PersistentDataType.INTEGER, radius);
        if (filter != null) {
            pdc.set(filterKey, PersistentDataType.STRING, filter);
        }

        helmet.setItemMeta(meta);
        return helmet;
    }

    public boolean isMinersGoggles(ItemStack item) {
        if (item == null || item.getType() != Material.LEATHER_HELMET) {
            return false;
        }
        var meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(gogglesKey, PersistentDataType.BYTE);
    }

    private void startGogglesScanner() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    ItemStack helmet = player.getInventory().getHelmet();

                    if (isMinersGoggles(helmet) && player.hasPermission("powermining.use.minersgoggles")) {
                        scanAndHighlightOres(player, helmet);
                    } else {
                        clearHighlights(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void scanAndHighlightOres(Player player, ItemStack goggles) {
        clearHighlights(player);

        var meta = goggles.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        int radius = pdc.getOrDefault(radiusKey, PersistentDataType.INTEGER, 10);
        String filter = pdc.get(filterKey, PersistentDataType.STRING);

        Set<Material> targetOres = new HashSet<>();
        if (filter == null || filter.isEmpty()) {
            targetOres.addAll(ALL_ORES);
        } else {
            for (Material ore : ALL_ORES) {
                if (ore.name().contains(filter.toUpperCase())) {
                    targetOres.add(ore);
                }
            }
        }

        Location center = player.getLocation();
        Set<BlockDisplay> highlights = new HashSet<>();

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > radius * radius) continue;

                    Block block = center.getWorld().getBlockAt(cx + x, cy + y, cz + z);
                    Material type = block.getType();

                    if (targetOres.contains(type)) {
                        Color color = ORE_COLORS.getOrDefault(type, Color.WHITE);
                        BlockDisplay display = createHighlight(block, color);
                        if (display != null) {
                            highlights.add(display);
                        }
                    }
                }
            }
        }

        activeHighlights.put(player.getUniqueId(), highlights);
    }

    private BlockDisplay createHighlight(Block block, Color color) {
        Location loc = block.getLocation().add(0.5, 0.5, 0.5);

        BlockDisplay display = loc.getWorld().spawn(loc, BlockDisplay.class, bd -> {
            bd.setBlock(block.getBlockData());
            bd.setGlowing(true);
            bd.setGlowColorOverride(org.bukkit.Color.fromRGB(color.getRed(), color.getGreen(), color.getBlue()));
            bd.setTransformation(new org.bukkit.util.Transformation(
                    new org.joml.Vector3f(-0.5f, -0.5f, -0.5f),
                    new org.joml.AxisAngle4f(0, 0, 0, 1),
                    new org.joml.Vector3f(1.0f, 1.0f, 1.0f),
                    new org.joml.AxisAngle4f(0, 0, 0, 1)
            ));
            bd.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
        });

        return display;
    }

    private void clearHighlights(Player player) {
        Set<BlockDisplay> highlights = activeHighlights.remove(player.getUniqueId());
        if (highlights != null) {
            for (BlockDisplay display : highlights) {
                if (display != null && display.isValid()) {
                    display.remove();
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isMinersGoggles(item)) return;
        if (!player.isSneaking()) return;

        event.setCancelled(true);

        var meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String currentFilter = pdc.get(filterKey, PersistentDataType.STRING);

        String[] filters = {null, "DIAMOND", "EMERALD", "GOLD", "IRON", "COPPER", "LAPIS", "REDSTONE", "COAL", "ANCIENT_DEBRIS", "QUARTZ"};
        int currentIndex = 0;
        for (int i = 0; i < filters.length; i++) {
            if (Objects.equals(filters[i], currentFilter)) {
                currentIndex = i;
                break;
            }
        }

        int newIndex = (currentIndex + 1) % filters.length;
        String newFilter = filters[newIndex];

        int radius = pdc.getOrDefault(radiusKey, PersistentDataType.INTEGER, 10);
        ItemStack newGoggles = createMinersGoggles(radius, newFilter);
        player.getInventory().setItemInMainHand(newGoggles);

        String filterName = newFilter == null ? "All Ores" : formatOreName(newFilter);
        player.sendMessage(miniMessage.deserialize("<gray>Goggles filter set to: <green>" + filterName + "</green>"));
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            ItemStack helmet = player.getInventory().getHelmet();
            if (!isMinersGoggles(helmet)) {
                clearHighlights(player);
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player player = event.getPlayer();
            ItemStack helmet = player.getInventory().getHelmet();
            if (!isMinersGoggles(helmet)) {
                clearHighlights(player);
            }
        }, 20L);
    }

    private String formatOreName(String name) {
        if (name == null) return "All Ores";
        String formatted = name.replace("_", " ");
        return formatted.charAt(0) + formatted.substring(1).toLowerCase();
    }

    public void shutdown() {
        for (UUID uuid : new HashSet<>(activeHighlights.keySet())) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                clearHighlights(player);
            } else {
                Set<BlockDisplay> highlights = activeHighlights.remove(uuid);
                if (highlights != null) {
                    for (BlockDisplay display : highlights) {
                        if (display != null && display.isValid()) {
                            display.remove();
                        }
                    }
                }
            }
        }
    }
}
