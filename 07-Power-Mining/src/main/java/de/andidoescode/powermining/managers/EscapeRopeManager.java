package de.andidoescode.powermining.managers;

import de.andidoescode.powermining.PowerMining;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

public class EscapeRopeManager implements Listener {

    private final PowerMining plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private final NamespacedKey escapeRopeKey;
    private final NamespacedKey worldKey;
    private final NamespacedKey xKey;
    private final NamespacedKey yKey;
    private final NamespacedKey zKey;
    private final NamespacedKey yawKey;
    private final NamespacedKey pitchKey;

    public EscapeRopeManager(PowerMining plugin) {
        this.plugin = plugin;
        this.escapeRopeKey = new NamespacedKey(plugin, "escape_rope");
        this.worldKey = new NamespacedKey(plugin, "escape_rope_world");
        this.xKey = new NamespacedKey(plugin, "escape_rope_x");
        this.yKey = new NamespacedKey(plugin, "escape_rope_y");
        this.zKey = new NamespacedKey(plugin, "escape_rope_z");
        this.yawKey = new NamespacedKey(plugin, "escape_rope_yaw");
        this.pitchKey = new NamespacedKey(plugin, "escape_rope_pitch");
    }

    public ItemStack createEscapeRope() {
        ItemStack lead = new ItemStack(Material.LEAD);
        ItemMeta meta = lead.getItemMeta();

        meta.displayName(Component.text("Escape Rope")
                .color(NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        meta.lore(Arrays.asList(
                Component.empty(),
                Component.text("A magical rope for quick escapes!")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("✦ ")
                        .color(NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("Right-click a block to set return point")
                                .color(NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false)),
                Component.text("✦ ")
                        .color(NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("Right-click air to teleport back")
                                .color(NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false)),
                Component.empty(),
                Component.text("⚠ ")
                        .color(NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("Consumed on teleport!")
                                .color(NamedTextColor.RED)
                                .decoration(TextDecoration.ITALIC, false)),
                Component.empty(),
                Component.text("Status: ")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("No return point set")
                                .color(NamedTextColor.RED)
                                .decoration(TextDecoration.ITALIC, false))
        ));

        meta.getPersistentDataContainer().set(escapeRopeKey, PersistentDataType.BYTE, (byte) 1);
        lead.setItemMeta(meta);

        return lead;
    }

    public boolean isEscapeRope(ItemStack item) {
        if (item == null || item.getType() != Material.LEAD) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(escapeRopeKey, PersistentDataType.BYTE);
    }

    private boolean hasReturnPoint(ItemStack item) {
        if (!isEscapeRope(item)) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(worldKey, PersistentDataType.STRING);
    }

    private Location getReturnPoint(ItemStack item, Player player) {
        if (!hasReturnPoint(item)) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        String worldName = pdc.get(worldKey, PersistentDataType.STRING);
        Double x = pdc.get(xKey, PersistentDataType.DOUBLE);
        Double y = pdc.get(yKey, PersistentDataType.DOUBLE);
        Double z = pdc.get(zKey, PersistentDataType.DOUBLE);
        Float yaw = pdc.get(yawKey, PersistentDataType.FLOAT);
        Float pitch = pdc.get(pitchKey, PersistentDataType.FLOAT);

        if (worldName == null || x == null || y == null || z == null) {
            return null;
        }

        var world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return null;
        }

        return new Location(world, x, y, z, yaw != null ? yaw : 0, pitch != null ? pitch : 0);
    }

    private void setReturnPoint(ItemStack item, Location location) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        pdc.set(worldKey, PersistentDataType.STRING, location.getWorld().getName());
        pdc.set(xKey, PersistentDataType.DOUBLE, location.getX());
        pdc.set(yKey, PersistentDataType.DOUBLE, location.getY());
        pdc.set(zKey, PersistentDataType.DOUBLE, location.getZ());
        pdc.set(yawKey, PersistentDataType.FLOAT, location.getYaw());
        pdc.set(pitchKey, PersistentDataType.FLOAT, location.getPitch());

        meta.displayName(Component.text("Escape Rope")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        String coordsText = String.format("%.0f, %.0f, %.0f", location.getX(), location.getY(), location.getZ());

        meta.lore(Arrays.asList(
                Component.empty(),
                Component.text("A magical rope for quick escapes!")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("✦ ")
                        .color(NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("Right-click air to teleport back")
                                .color(NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false)),
                Component.empty(),
                Component.text("⚠ ")
                        .color(NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("Consumed on teleport!")
                                .color(NamedTextColor.RED)
                                .decoration(TextDecoration.ITALIC, false)),
                Component.empty(),
                Component.text("Status: ")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("Return point set!")
                                .color(NamedTextColor.GREEN)
                                .decoration(TextDecoration.ITALIC, false)),
                Component.text("📍 ")
                        .color(NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(coordsText + " (" + location.getWorld().getName() + ")")
                                .color(NamedTextColor.AQUA)
                                .decoration(TextDecoration.ITALIC, false))
        ));

        item.setItemMeta(meta);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isEscapeRope(item)) {
            return;
        }

        if (!player.hasPermission("powermining.use.escaperope")) {
            String noPermMsg = plugin.getConfig().getString("messages.no-permission", "<red>You don't have permission to do that!</red>");
            player.sendMessage(miniMessage.deserialize(noPermMsg));
            return;
        }

        event.setCancelled(true);

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Location clickedLocation = event.getClickedBlock().getLocation().add(0.5, 1, 0.5);
            clickedLocation.setYaw(player.getLocation().getYaw());
            clickedLocation.setPitch(player.getLocation().getPitch());

            setReturnPoint(item, clickedLocation);

            String setMsg = plugin.getConfig().getString("messages.escape-rope-set",
                    "<green>Return point set! Right-click in the air to teleport back.</green>");
            player.sendMessage(miniMessage.deserialize(setMsg));

            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
            player.getWorld().spawnParticle(Particle.ENCHANT, clickedLocation, 30, 0.5, 0.5, 0.5, 0.1);

        } else if (event.getAction() == Action.RIGHT_CLICK_AIR || 
                   (event.getAction() == Action.RIGHT_CLICK_BLOCK && !hasReturnPoint(item))) {

            if (!hasReturnPoint(item)) {
                String noPointMsg = plugin.getConfig().getString("messages.escape-rope-no-point",
                        "<red>No return point set! Right-click a block first.</red>");
                player.sendMessage(miniMessage.deserialize(noPointMsg));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            Location returnPoint = getReturnPoint(item, player);
            if (returnPoint == null) {
                String invalidMsg = plugin.getConfig().getString("messages.escape-rope-invalid",
                        "<red>Return point is no longer valid!</red>");
                player.sendMessage(miniMessage.deserialize(invalidMsg));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.1);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

            player.teleport(returnPoint);

            player.getWorld().spawnParticle(Particle.PORTAL, returnPoint.clone().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.1);
            player.playSound(returnPoint, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);

            item.setAmount(item.getAmount() - 1);

            String teleportMsg = plugin.getConfig().getString("messages.escape-rope-teleport",
                    "<green>Whoosh! You've been teleported back to your return point!</green>");
            player.sendMessage(miniMessage.deserialize(teleportMsg));
        }
    }

    public void shutdown() {
    }
}
