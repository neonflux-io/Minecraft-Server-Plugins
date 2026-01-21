package de.andidoescode.powermining.managers;

import de.andidoescode.powermining.PowerMining;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CaveCompassManager implements Listener {

    private final PowerMining plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final NamespacedKey compassKey;
    private final NamespacedKey modeKey;

    private static final List<StructureTarget> STRUCTURE_TARGETS = new ArrayList<>();
    private static final Pattern LOCATE_PATTERN = Pattern.compile("\\[(-?\\d+|~), (-?\\d+|~), (-?\\d+|~)\\]");
    private final Map<UUID, Long> lastUpdateTime = new HashMap<>();
    private final Map<UUID, Location> cachedLocations = new HashMap<>();

    static {
        STRUCTURE_TARGETS.add(new StructureTarget("mineshaft", "Mineshaft", NamedTextColor.GOLD));
        STRUCTURE_TARGETS.add(new StructureTarget("stronghold", "Stronghold", NamedTextColor.DARK_PURPLE));
        STRUCTURE_TARGETS.add(new StructureTarget("fortress", "Nether Fortress", NamedTextColor.DARK_RED));
        STRUCTURE_TARGETS.add(new StructureTarget("bastion_remnant", "Bastion Remnant", NamedTextColor.DARK_GRAY));
        STRUCTURE_TARGETS.add(new StructureTarget("ancient_city", "Ancient City", NamedTextColor.DARK_AQUA));
        STRUCTURE_TARGETS.add(new StructureTarget("trail_ruins", "Trail Ruins", NamedTextColor.YELLOW));
        STRUCTURE_TARGETS.add(new StructureTarget("trial_chambers", "Trial Chambers", NamedTextColor.LIGHT_PURPLE));
    }

    public CaveCompassManager(PowerMining plugin) {
        this.plugin = plugin;
        this.compassKey = new NamespacedKey(plugin, "cave_compass");
        this.modeKey = new NamespacedKey(plugin, "cave_compass_mode");
        startCompassUpdater();
    }

    public ItemStack createCaveCompass() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        CompassMeta meta = (CompassMeta) compass.getItemMeta();

        meta.displayName(Component.text("Cave Compass")
                .color(NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        meta.getPersistentDataContainer().set(compassKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(modeKey, PersistentDataType.INTEGER, 0);

        updateCompassLore(meta, 0);
        compass.setItemMeta(meta);

        return compass;
    }

    private void updateCompassLore(CompassMeta meta, int modeIndex) {
        StructureTarget current = STRUCTURE_TARGETS.get(modeIndex);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("A mystical compass for explorers!")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("✦ ")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text("Points to nearby structures")
                        .color(NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false)));
        lore.add(Component.text("✦ ")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text("Shift + Right-click: Next target")
                        .color(NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false)));
        lore.add(Component.text("✦ ")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text("Shift + Left-click: Previous target")
                        .color(NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false)));
        lore.add(Component.empty());
        lore.add(Component.text("Current Target: ")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text(current.displayName)
                        .color(current.color)
                        .decoration(TextDecoration.ITALIC, false)
                        .decoration(TextDecoration.BOLD, true)));
        lore.add(Component.empty());

        for (int i = 0; i < STRUCTURE_TARGETS.size(); i++) {
            StructureTarget target = STRUCTURE_TARGETS.get(i);
            String prefix = (i == modeIndex) ? "▶ " : "  ";
            NamedTextColor textColor = (i == modeIndex) ? target.color : NamedTextColor.DARK_GRAY;

            lore.add(Component.text(prefix + target.displayName)
                    .color(textColor)
                    .decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
    }

    public boolean isCaveCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) {
            return false;
        }
        var meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(compassKey, PersistentDataType.BYTE);
    }

    private void startCompassUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    ItemStack mainHand = player.getInventory().getItemInMainHand();
                    ItemStack offHand = player.getInventory().getItemInOffHand();

                    if (isCaveCompass(mainHand)) {
                        updateCompassTarget(player, mainHand);
                    } else if (isCaveCompass(offHand)) {
                        updateCompassTarget(player, offHand);
                    }
                }
            }
        }.runTaskTimer(plugin, 60L, 60L);
    }

    private void updateCompassTarget(Player player, ItemStack compass) {
        if (!player.hasPermission("powermining.use.cavecompass")) {
            return;
        }

        CompassMeta meta = (CompassMeta) compass.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int modeIndex = pdc.getOrDefault(modeKey, PersistentDataType.INTEGER, 0);

        StructureTarget target = STRUCTURE_TARGETS.get(modeIndex);
        UUID playerId = player.getUniqueId();
        
        Long lastUpdate = lastUpdateTime.get(playerId);
        Location cachedLoc = cachedLocations.get(playerId);
        if (lastUpdate != null && cachedLoc != null && System.currentTimeMillis() - lastUpdate < 3000) {
            updateCompassDisplay(player, compass, target, cachedLoc);
            return;
        }

        String command = "locate structure minecraft:" + target.id;
        
        LocateCommandSender locateSender = new LocateCommandSender(player, output -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Location foundLocation = parseLocateOutput(output, player);
                
                lastUpdateTime.put(playerId, System.currentTimeMillis());
                cachedLocations.put(playerId, foundLocation);
                
                updateCompassDisplay(player, compass, target, foundLocation);
            });
        });
        
        Bukkit.dispatchCommand(locateSender, command);
    }
    
    private Location parseLocateOutput(String output, Player player) {
        Matcher matcher = LOCATE_PATTERN.matcher(output);
        if (matcher.find()) {
            try {
                String xStr = matcher.group(1);
                String yStr = matcher.group(2);
                String zStr = matcher.group(3);
                
                int x = xStr.equals("~") ? player.getLocation().getBlockX() : Integer.parseInt(xStr);
                int y = yStr.equals("~") ? 0 : Integer.parseInt(yStr);
                int z = zStr.equals("~") ? player.getLocation().getBlockZ() : Integer.parseInt(zStr);
                
                return new Location(player.getWorld(), x, y, z);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private void updateCompassDisplay(Player player, ItemStack compass, StructureTarget target, Location foundLocation) {
        CompassMeta updatedMeta = (CompassMeta) compass.getItemMeta();

        if (foundLocation != null) {
            updatedMeta.setLodestone(foundLocation);
            updatedMeta.setLodestoneTracked(false);
            
            int distance = (int) player.getLocation().distance(foundLocation);

            player.sendActionBar(Component.text("🧭 ")
                    .color(NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text(target.displayName)
                            .color(target.color))
                    .append(Component.text(" - ")
                            .color(NamedTextColor.DARK_GRAY))
                    .append(Component.text(distance + " blocks away")
                            .color(NamedTextColor.YELLOW)));
        } else {
            updatedMeta.setLodestone(null);
            player.sendActionBar(Component.text("🧭 ")
                    .color(NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text("No " + target.displayName + " found nearby")
                            .color(NamedTextColor.RED)));
        }

        compass.setItemMeta(updatedMeta);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isCaveCompass(item)) {
            return;
        }

        if (!player.hasPermission("powermining.use.cavecompass")) {
            String noPermMsg = plugin.getConfig().getString("messages.no-permission", "<red>You don't have permission to do that!</red>");
            player.sendMessage(miniMessage.deserialize(noPermMsg));
            return;
        }

        boolean isRightClick = event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK;
        boolean isLeftClick = event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK;
        
        if ((isRightClick || isLeftClick) && player.isSneaking()) {
            event.setCancelled(true);

            CompassMeta meta = (CompassMeta) item.getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            int currentMode = pdc.getOrDefault(modeKey, PersistentDataType.INTEGER, 0);

            int newMode;
            if (isRightClick) {
                newMode = (currentMode + 1) % STRUCTURE_TARGETS.size();
            } else {
                newMode = (currentMode - 1 + STRUCTURE_TARGETS.size()) % STRUCTURE_TARGETS.size();
            }
            pdc.set(modeKey, PersistentDataType.INTEGER, newMode);
            
            cachedLocations.remove(player.getUniqueId());
            lastUpdateTime.remove(player.getUniqueId());

            updateCompassLore(meta, newMode);
            item.setItemMeta(meta);

            StructureTarget newTarget = STRUCTURE_TARGETS.get(newMode);
            player.sendMessage(miniMessage.deserialize(
                    "<gray>Cave Compass now tracking: <" + newTarget.color.asHexString() + ">" + newTarget.displayName + "</" + newTarget.color.asHexString() + ">"));

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);

            updateCompassTarget(player, item);
        }
    }

    public void shutdown() {
        cachedLocations.clear();
        lastUpdateTime.clear();
    }

    private record StructureTarget(String id, String displayName, NamedTextColor color) {}
    
    private static class LocateCommandSender implements org.bukkit.command.CommandSender {
        private final Player player;
        private final java.util.function.Consumer<String> outputConsumer;
        private final StringBuilder output = new StringBuilder();
        
        public LocateCommandSender(Player player, java.util.function.Consumer<String> outputConsumer) {
            this.player = player;
            this.outputConsumer = outputConsumer;
        }
        
        @Override
        public void sendMessage(String message) {
            output.append(message);
            outputConsumer.accept(output.toString());
        }
        
        @Override
        public void sendMessage(String... messages) {
            for (String msg : messages) {
                output.append(msg);
            }
            outputConsumer.accept(output.toString());
        }
        
        @Override
        public void sendMessage(UUID sender, String message) {
            sendMessage(message);
        }
        
        @Override
        public void sendMessage(UUID sender, String... messages) {
            sendMessage(messages);
        }
        
        @Override
        public org.bukkit.Server getServer() {
            return player.getServer();
        }
        
        @Override
        public String getName() {
            return player.getName();
        }
        
        @Override
        public boolean isPermissionSet(String name) {
            return player.isPermissionSet(name);
        }
        
        @Override
        public boolean isPermissionSet(org.bukkit.permissions.Permission perm) {
            return player.isPermissionSet(perm);
        }
        
        @Override
        public boolean hasPermission(String name) {
            return player.hasPermission(name);
        }
        
        @Override
        public boolean hasPermission(org.bukkit.permissions.Permission perm) {
            return player.hasPermission(perm);
        }
        
        @Override
        public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin, String name, boolean value) {
            return player.addAttachment(plugin, name, value);
        }
        
        @Override
        public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin) {
            return player.addAttachment(plugin);
        }
        
        @Override
        public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin, String name, boolean value, int ticks) {
            return player.addAttachment(plugin, name, value, ticks);
        }
        
        @Override
        public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin, int ticks) {
            return player.addAttachment(plugin, ticks);
        }
        
        @Override
        public void removeAttachment(org.bukkit.permissions.PermissionAttachment attachment) {
            player.removeAttachment(attachment);
        }
        
        @Override
        public void recalculatePermissions() {
            player.recalculatePermissions();
        }
        
        @Override
        public Set<org.bukkit.permissions.PermissionAttachmentInfo> getEffectivePermissions() {
            return player.getEffectivePermissions();
        }
        
        @Override
        public boolean isOp() {
            return true;
        }
        
        @Override
        public void setOp(boolean value) {
        }
        
        @Override
        public Spigot spigot() {
            return player.spigot();
        }
        
        @Override
        public Component name() {
            return player.name();
        }
    }
}
