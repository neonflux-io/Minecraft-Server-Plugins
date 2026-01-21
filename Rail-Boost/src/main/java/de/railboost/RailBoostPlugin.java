package de.railboost;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RailBoostPlugin extends JavaPlugin implements Listener, TabExecutor {
    private final Map<UUID, Double> minecartSpeeds = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> autoPickupEnabled = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> autoPickupRadius = new ConcurrentHashMap<>();
    private final Map<UUID, Inventory> minecartStorage = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> speedometerEnabled = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> chunkloadEnabled = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> magnetEnabled = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> effectEnabled = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> autoSitEnabled = new ConcurrentHashMap<>();
    private final Map<UUID, Set<Material>> blacklist = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> speedometerBars = new ConcurrentHashMap<>();
    private final Set<UUID> activatedMinecarts = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final double[] SPEED_LEVELS = {0.25, 0.5, 1.0, 2.0, 3.0, 4.0};
    private static final String[] SPEED_DESCRIPTIONS = {
        "1 - x0.25", "2 - x0.5", "3 - x1.0", "4 - x2.0", "5 - x3.0", "6 - x4.0"
    };
    private final Map<UUID, org.bukkit.Particle> playerParticleChoice = new ConcurrentHashMap<>();
    private static final long EFFECT_UPDATE_COOLDOWN = 200;
    private final Map<UUID, Long> lastEffectUpdateTime = new ConcurrentHashMap<>();
    private static final org.bukkit.Particle[] AVAILABLE_PARTICLES = {
        org.bukkit.Particle.END_ROD,
        org.bukkit.Particle.FLAME,
        org.bukkit.Particle.PORTAL,
        org.bukkit.Particle.CLOUD,
        org.bukkit.Particle.VILLAGER_HAPPY,
        org.bukkit.Particle.CRIT,
        org.bukkit.Particle.SPELL_WITCH,
        org.bukkit.Particle.DRAGON_BREATH,
        org.bukkit.Particle.HEART
    };
    private final Map<String, List<Material>> materialCategories = new HashMap<>();
    private final Map<UUID, Long> exitCooldowns = new ConcurrentHashMap<>();
    private static final long EXIT_COOLDOWN_TIME = 3000;
    private final Map<UUID, String> effectType = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("railboost").setExecutor(this);
        getCommand("railboost").setTabCompleter(this);
        initializeMaterialCategories();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!playerParticleChoice.containsKey(player.getUniqueId())) {
                playerParticleChoice.put(player.getUniqueId(), org.bukkit.Particle.END_ROD);
            }
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.isInsideVehicle() && player.getVehicle() instanceof Minecart) {
                        Minecart cart = (Minecart) player.getVehicle();
                        UUID cartId = cart.getUniqueId();

                        if (speedometerEnabled.getOrDefault(cartId, false)) {
                            double speed = cart.getVelocity().length() * 20 * 3.6;
                            double percent = Math.min(1.0, speed / 300.0);
                            BossBar bar = speedometerBars.computeIfAbsent(player.getUniqueId(), uuid -> {
                                BossBar b = Bukkit.createBossBar("Speedometer", BarColor.BLUE, BarStyle.SEGMENTED_20);
                                b.addPlayer(player);
                                return b;
                            });
                            bar.setProgress(percent);
                            bar.setTitle("§bSpeedometer: " + String.format("%.1f", speed) + " km/h");
                            bar.setVisible(true);
                        } else {
                            BossBar bar = speedometerBars.get(player.getUniqueId());
                            if (bar != null) bar.setVisible(false);
                        }
                    } else {
                        BossBar bar = speedometerBars.get(player.getUniqueId());
                        if (bar != null) bar.setVisible(false);
                    }
                }
            }
        }.runTaskTimer(this, 0, 10);
        new BukkitRunnable() {
            @Override
            public void run() {
                List<Minecart> minecarts = new ArrayList<>();
                for (World world : Bukkit.getWorlds()) {
                    for (Entity entity : world.getEntitiesByClass(Minecart.class)) {
                        minecarts.add((Minecart) entity);
                    }
                }
                for (int i = 0; i < minecarts.size(); i++) {
                    Minecart cartA = minecarts.get(i);
                    UUID uuidA = cartA.getUniqueId();
                    if (!magnetEnabled.getOrDefault(uuidA, false)) continue;
                    for (int j = i + 1; j < minecarts.size(); j++) {
                        Minecart cartB = minecarts.get(j);
                        UUID uuidB = cartB.getUniqueId();
                        if (!magnetEnabled.getOrDefault(uuidB, false)) continue;
                        double distance = cartA.getLocation().distance(cartB.getLocation());
                        if (distance < 3.0 && distance > 0.1) {
                            Vector dir = cartB.getLocation().toVector().subtract(cartA.getLocation().toVector()).normalize();
                            double strength = 0.15 / distance;
                            cartA.setVelocity(cartA.getVelocity().add(dir.multiply(strength)));
                            cartB.setVelocity(cartB.getVelocity().add(dir.multiply(-strength)));
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0, 2);
        loadMinecartSettings();
    }

    private void initializeMaterialCategories() {
        List<Material> stones = new ArrayList<>();
        List<Material> woods = new ArrayList<>();
        List<Material> ores = new ArrayList<>();
        List<Material> plants = new ArrayList<>();
        for (Material mat : Material.values()) {
            String name = mat.name();
            if (name.contains("STONE") || name.contains("GRANITE") || name.contains("ANDESITE") ||
                name.contains("DIORITE") || name.contains("BLACKSTONE") || name.contains("DEEPSLATE")) {
                stones.add(mat);
            } else if (name.contains("WOOD") || name.contains("LOG") || name.contains("PLANK") ||
                      name.contains("STRIPPED")) {
                woods.add(mat);
            } else if (name.contains("ORE") || name.contains("INGOT") || name.contains("NUGGET")) {
                ores.add(mat);
            } else if (name.contains("LEAVES") || name.contains("SAPLING") || name.contains("FLOWER") ||
                      name.contains("GRASS") || name.contains("SEED")) {
                plants.add(mat);
            }
        }
        materialCategories.put("stone", stones);
        materialCategories.put("wood", woods);
        materialCategories.put("ore", ores);
        materialCategories.put("plant", plants);
    }

    @Override
    public void onDisable() {
        saveMinecartSettings();
    }

    private void saveMinecartSettings() {
        try {
            org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();

            for (UUID cartId : activatedMinecarts) {
                String key = cartId.toString();

                config.set("minecarts." + key + ".active", true);
                config.set("minecarts." + key + ".speed", minecartSpeeds.getOrDefault(cartId, 1.0));
                config.set("minecarts." + key + ".autopickup", autoPickupEnabled.getOrDefault(cartId, false));
                config.set("minecarts." + key + ".autopickupRadius", autoPickupRadius.getOrDefault(cartId, 3));
                config.set("minecarts." + key + ".speedometer", speedometerEnabled.getOrDefault(cartId, false));
                config.set("minecarts." + key + ".chunkload", chunkloadEnabled.getOrDefault(cartId, false));
                config.set("minecarts." + key + ".magnet", magnetEnabled.getOrDefault(cartId, false));
                config.set("minecarts." + key + ".effect", effectEnabled.getOrDefault(cartId, false));
                config.set("minecarts." + key + ".effectType", effectType.getOrDefault(cartId, "FLAME"));
                config.set("minecarts." + key + ".autosit", autoSitEnabled.getOrDefault(cartId, false));

                Set<Material> bl = blacklist.getOrDefault(cartId, new HashSet<>());
                List<String> blList = new ArrayList<>();
                for (Material m : bl) blList.add(m.name());
                config.set("minecarts." + key + ".blacklist", blList);

                Minecart cart = null;
                for (World world : Bukkit.getWorlds()) {
                    for (Entity entity : world.getEntitiesByClass(Minecart.class)) {
                        if (entity.getUniqueId().equals(cartId)) {
                            cart = (Minecart) entity;
                            break;
                        }
                    }
                    if (cart != null) break;
                }

                if (cart != null) {
                    config.set("minecarts." + key + ".lastWorld", cart.getWorld().getName());
                    config.set("minecarts." + key + ".lastX", cart.getLocation().getBlockX());
                    config.set("minecarts." + key + ".lastY", cart.getLocation().getBlockY());
                    config.set("minecarts." + key + ".lastZ", cart.getLocation().getBlockZ());
                }
            }

            java.io.File dataDir = getDataFolder();
            if (!dataDir.exists()) {
                dataDir.mkdir();
            }

            config.save(new java.io.File(getDataFolder(), "minecarts.yml"));
            getLogger().info("RailBoost: " + activatedMinecarts.size() + " minecarts saved");
        } catch (Exception e) {
            getLogger().warning("Error saving minecart settings: " + e.getMessage());
        }
    }

    private void loadMinecartSettings() {
        try {
            java.io.File file = new java.io.File(getDataFolder(), "minecarts.yml");
            if (!file.exists()) {
                java.io.File oldFile = new java.io.File(getDataFolder(), "loren.yml");
                if (oldFile.exists()) {
                    file = oldFile;
                    getLogger().info("Loading settings from old file 'loren.yml'");
                } else {
                    return;
                }
            }

            org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
            if (!config.contains("minecarts")) return;

            int loadedCount = 0;

            for (String key : config.getConfigurationSection("minecarts").getKeys(false)) {
                try {
                    UUID cartId = java.util.UUID.fromString(key);

                    activatedMinecarts.add(cartId);
                    loadedCount++;

                    minecartSpeeds.put(cartId, config.getDouble("minecarts." + key + ".speed", 1.0));
                    autoPickupEnabled.put(cartId, config.getBoolean("minecarts." + key + ".autopickup", false));
                    autoPickupRadius.put(cartId, config.getInt("minecarts." + key + ".autopickupRadius", 3));
                    speedometerEnabled.put(cartId, config.getBoolean("minecarts." + key + ".speedometer", false));
                    chunkloadEnabled.put(cartId, config.getBoolean("minecarts." + key + ".chunkload", false));
                    magnetEnabled.put(cartId, config.getBoolean("minecarts." + key + ".magnet", false));
                    effectEnabled.put(cartId, config.getBoolean("minecarts." + key + ".effect", false));
                    effectType.put(cartId, config.getString("minecarts." + key + ".effectType", "FLAME"));
                    autoSitEnabled.put(cartId, config.getBoolean("minecarts." + key + ".autosit", false));

                    List<String> blList = config.getStringList("minecarts." + key + ".blacklist");
                    Set<Material> blSet = new HashSet<>();
                    for (String mat : blList) {
                        try { blSet.add(Material.valueOf(mat)); } catch (Exception ignore) {}
                    }
                    blacklist.put(cartId, blSet);

                    if (!minecartStorage.containsKey(cartId)) {
                        minecartStorage.put(cartId, Bukkit.createInventory(null, 27, "Minecart Storage"));
                    }
                } catch (Exception e) {
                    getLogger().warning("Error loading minecart with ID " + key + ": " + e.getMessage());
                }
            }

            getLogger().info("RailBoost: " + loadedCount + " minecarts loaded");

            if (file.getName().equals("loren.yml")) {
                saveMinecartSettings();
                getLogger().info("Settings transferred to new file 'minecarts.yml'");
            }
        } catch (Exception e) {
            getLogger().warning("Error loading minecart settings: " + e.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            player.sendMessage("§eRailBoost commands: /railboost help");
            return true;
        }
        String cmd = args[0].toLowerCase();
        if (Arrays.asList("speed", "autopickup", "storage", "speedometer", "chunkload", "magnet", "effect", "autosit", "blacklist", "configuration").contains(cmd)) {
            if (!(cmd.equals("blacklist") && args.length > 1 && args[1].equalsIgnoreCase("list"))) {
                if (!player.isInsideVehicle() || !(player.getVehicle() instanceof Minecart)) {
                    player.sendMessage("§cYou must be sitting in a minecart!");
                    return true;
                }

                UUID cartId = player.getVehicle().getUniqueId();
                if (!cmd.equals("configuration") && !activatedMinecarts.contains(cartId)) {
                    player.sendMessage("§cThis minecart is not activated for RailBoost!");
                    player.sendMessage("§cUse /railboost configuration true to activate the minecart.");
                    return true;
                }
            }
        }
        switch (cmd) {
            case "speed":
                if (args.length < 2) {
                    player.sendMessage("§c/railboost speed <1-6>");
                    for (String desc : SPEED_DESCRIPTIONS) player.sendMessage("§7" + desc);
                    return true;
                }
                try {
                    int speedIdx = Integer.parseInt(args[1]) - 1;
                    if (speedIdx < 0 || speedIdx >= SPEED_LEVELS.length) throw new NumberFormatException();
                    UUID cartId = player.getVehicle().getUniqueId();
                    minecartSpeeds.put(cartId, SPEED_LEVELS[speedIdx]);
                    player.sendMessage("§aSpeed set: " + SPEED_DESCRIPTIONS[speedIdx] + " (" + SPEED_LEVELS[speedIdx] + "x)");
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid number (1-6).");
                }
                return true;
            case "autopickup":
                if (!(player.isInsideVehicle() && player.getVehicle() instanceof Minecart)) {
                    player.sendMessage("§cYou must be sitting in a minecart!");
                    return true;
                }
                UUID cartId = player.getVehicle().getUniqueId();
                if (args.length < 2) {
                    player.sendMessage("§c/railboost autopickup <true|false|radius <1-5>>");
                    player.sendMessage("§7Available options: true, false, radius <1-5>");
                    return true;
                }
                if (args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("on") || args[1].equals("1")) {
                    autoPickupEnabled.put(cartId, true);
                    player.sendMessage("§aAutoPickUp enabled for this minecart.");
                } else if (args[1].equalsIgnoreCase("false") || args[1].equalsIgnoreCase("off") || args[1].equals("0")) {
                    autoPickupEnabled.put(cartId, false);
                    player.sendMessage("§cAutoPickUp disabled for this minecart.");
                } else if (args[1].equalsIgnoreCase("radius") && args.length > 2) {
                    try {
                        int radius = Integer.parseInt(args[2]);
                        if (radius < 1 || radius > 5) throw new NumberFormatException();
                        autoPickupRadius.put(cartId, radius);
                        player.sendMessage("§aAutoPickUp radius for this minecart: " + radius);
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cInvalid radius (1-5).");
                    }
                } else {
                    player.sendMessage("§c/railboost autopickup <true|false|radius <1-5>>");
                    player.sendMessage("§7Available options: true, false, radius <1-5>");
                }
                return true;
            case "storage":
                UUID id = player.getVehicle().getUniqueId();
                Inventory inv = minecartStorage.computeIfAbsent(id, k -> Bukkit.createInventory(null, 27, "Minecart Storage"));
                player.openInventory(inv);
                return true;
            case "speedometer":
                cartId = player.getVehicle().getUniqueId();
                if (args.length < 2) {
                    boolean current = speedometerEnabled.getOrDefault(cartId, false);
                    speedometerEnabled.put(cartId, !current);
                    player.sendMessage("§eSpeedometer for this minecart " + (!current ? "enabled" : "disabled"));
                    return true;
                }
                if (!(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false") ||
                     args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off") ||
                     args[1].equals("1") || args[1].equals("0"))) {
                    player.sendMessage("§c/railboost speedometer <true|false>");
                    player.sendMessage("§7Available options: true, false");
                    return true;
                }
                boolean enable = args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("on") || args[1].equals("1");
                speedometerEnabled.put(cartId, enable);
                player.sendMessage(enable ? "§aSpeedometer enabled for this minecart." : "§cSpeedometer disabled for this minecart.");
                return true;
            case "chunkload":
                cartId = player.getVehicle().getUniqueId();
                if (args.length < 2) {
                    boolean current = chunkloadEnabled.getOrDefault(cartId, false);
                    player.sendMessage("§eChunkload for this minecart is currently " + (current ? "enabled" : "disabled"));
                    player.sendMessage("§7Usage: /railboost chunkload <true|false>");
                    return true;
                }
                if (!(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false") ||
                     args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off") ||
                     args[1].equals("1") || args[1].equals("0"))) {
                    player.sendMessage("§c/railboost chunkload <true|false>");
                    player.sendMessage("§7Available options: true, false");
                    return true;
                }
                enable = args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("on") || args[1].equals("1");
                chunkloadEnabled.put(cartId, enable);
                player.sendMessage(enable ? "§aChunkload enabled for this minecart." : "§cChunkload disabled for this minecart.");
                return true;
            case "magnet":
                cartId = player.getVehicle().getUniqueId();
                if (args.length < 2) {
                    boolean current = magnetEnabled.getOrDefault(cartId, false);
                    magnetEnabled.put(cartId, !current);
                    player.sendMessage("§eMagnetism for this minecart " + (!current ? "enabled" : "disabled"));
                    return true;
                }
                if (!(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false") ||
                     args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off") ||
                     args[1].equals("1") || args[1].equals("0"))) {
                    player.sendMessage("§c/railboost magnet <true|false>");
                    player.sendMessage("§7Available options: true, false");
                    return true;
                }
                enable = args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("on") || args[1].equals("1");
                magnetEnabled.put(cartId, enable);
                player.sendMessage(enable ? "§aMagnetism enabled for this minecart." : "§cMagnetism disabled for this minecart.");
                return true;
            case "effect":
                if (!(player.isInsideVehicle() && player.getVehicle() instanceof Minecart)) {
                    player.sendMessage("§cYou must be sitting in a minecart!");
                    return true;
                }
                UUID effectCartId = player.getVehicle().getUniqueId();
                if (args.length < 2) {
                    boolean current = effectEnabled.getOrDefault(effectCartId, false);
                    effectEnabled.put(effectCartId, !current);
                    player.sendMessage("§eEffects for this minecart " + (!current ? "enabled" : "disabled"));
                    return true;
                }
                if (args[1].equalsIgnoreCase("type")) {
                    if (args.length < 3) {
                        player.sendMessage("§c/railboost effect type <particle type>");
                        player.sendMessage("§7Available types: FLAME, SMOKE, HEART, CLOUD");
                        return true;
                    }
                    String type = args[2].toUpperCase();
                    effectType.put(effectCartId, type);
                    player.sendMessage("§aEffect type set for this minecart: " + type);
                    return true;
                }
                if (!(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false") ||
                     args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off") ||
                     args[1].equals("1") || args[1].equals("0"))) {
                    player.sendMessage("§c/railboost effect <true|false|type>");
                    player.sendMessage("§7Available options: true, false, type");
                    return true;
                }
                enable = args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("on") || args[1].equals("1");
                effectEnabled.put(effectCartId, enable);
                player.sendMessage(enable ? "§aEffects enabled for this minecart." : "§cEffects disabled for this minecart.");
                return true;
            case "autosit":
                if (!(player.isInsideVehicle() && player.getVehicle() instanceof Minecart)) {
                    player.sendMessage("§cYou must be sitting in a minecart!");
                    return true;
                }
                cartId = player.getVehicle().getUniqueId();
                if (args.length < 2) {
                    boolean current = autoSitEnabled.getOrDefault(cartId, false);
                    autoSitEnabled.put(cartId, !current);
                    player.sendMessage("§eAuto-Sit for this minecart " + (!current ? "enabled" : "disabled"));
                    return true;
                }
                if (!(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false") ||
                     args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off") ||
                     args[1].equals("1") || args[1].equals("0"))) {
                    player.sendMessage("§c/railboost autosit <true|false>");
                    player.sendMessage("§7Available options: true, false");
                    return true;
                }
                enable = args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("on") || args[1].equals("1");
                autoSitEnabled.put(cartId, enable);
                player.sendMessage(enable ? "§aAuto-Sit enabled for this minecart." : "§cAuto-Sit disabled for this minecart.");
                return true;
            case "blacklist":
                UUID blCartId = null;
                if (player.isInsideVehicle() && player.getVehicle() instanceof Minecart) {
                    blCartId = player.getVehicle().getUniqueId();
                }
                if (args.length < 2) {
                    player.sendMessage("§c/railboost blacklist <add|remove|list> <item>");
                    return true;
                }
                if (args[1].equalsIgnoreCase("list")) {
                    Set<Material> filter = blCartId != null ? blacklist.computeIfAbsent(blCartId, k -> new HashSet<>()) : new HashSet<>();
                    player.sendMessage("§eBlacklist: " + filter);
                    return true;
                }
                if (blCartId == null) {
                    player.sendMessage("§cYou must be sitting in a minecart!");
                    return true;
                }
                Set<Material> filter = blacklist.computeIfAbsent(blCartId, k -> new HashSet<>());
                if (args[1].equalsIgnoreCase("add") && args.length > 2) {
                    Material mat = Material.matchMaterial(args[2]);
                    if (mat != null) {
                        filter.add(mat);
                        player.sendMessage("§a" + mat + " added to the blacklist of this minecart.");
                    } else {
                        player.sendMessage("§cUnknown item.");
                    }
                } else if (args[1].equalsIgnoreCase("remove") && args.length > 2) {
                    Material mat = Material.matchMaterial(args[2]);
                    if (mat != null && filter.remove(mat)) {
                        player.sendMessage("§a" + mat + " removed from the blacklist of this minecart.");
                    } else {
                        player.sendMessage("§cItem not in the blacklist.");
                    }
                } else {
                    player.sendMessage("§c/railboost blacklist <add|remove|list> <item>");
                }
                return true;
            case "info":
                if (!(player.isInsideVehicle() && player.getVehicle() instanceof Minecart)) {
                    player.sendMessage("§cYou must be sitting in a minecart to see info!");
                    return true;
                }
                UUID infoCartId = player.getVehicle().getUniqueId();
                player.sendMessage("§e--- RailBoost Info ---");
                player.sendMessage("Speed: " + getCurrentSpeedDescription(player));
                player.sendMessage("AutoPickup: " + (autoPickupEnabled.getOrDefault(infoCartId, false) ? "enabled" : "disabled"));
                player.sendMessage("AutoPickup Radius: " + autoPickupRadius.getOrDefault(infoCartId, 3));
                player.sendMessage("Speedometer: " + (speedometerEnabled.getOrDefault(infoCartId, false) ? "enabled" : "disabled"));
                player.sendMessage("Magnet: " + (magnetEnabled.getOrDefault(infoCartId, false) ? "enabled" : "disabled"));
                player.sendMessage("Effects: " + (effectEnabled.getOrDefault(infoCartId, false) ? "enabled" : "disabled"));
                String effType = effectType != null ? String.valueOf(effectType.getOrDefault(infoCartId, "FLAME")) : "FLAME";
                player.sendMessage("Effect-Type: " + effType);
                player.sendMessage("Auto-Sit: " + (autoSitEnabled.getOrDefault(infoCartId, false) ? "enabled" : "disabled"));
                Set<Material> bl = blacklist.getOrDefault(infoCartId, new HashSet<>());
                player.sendMessage("Blacklist: " + (bl.isEmpty() ? "none" : bl));
                player.sendMessage("Chunkload (this minecart): " + (chunkloadEnabled.getOrDefault(infoCartId, false) ? "enabled" : "disabled"));
                player.sendMessage("§e----------------------");
                return true;
            case "help":
                player.sendMessage("§eRailBoost commands:\n" +
                        "/railboost speed <1-6>\n" +
                        "/railboost autopickup <true|false|radius <1-5>>\n" +
                        "/railboost storage\n" +
                        "/railboost speedometer <true|false>\n" +
                        "/railboost chunkload <true|false>\n" +
                        "/railboost magnet <true|false>\n" +
                        "/railboost effect <true|false|type>\n" +
                        "/railboost autosit <true|false>\n" +
                        "/railboost blacklist <add|remove|list> <item>\n" +
                        "/railboost configuration <true|false>\n" +
                        "/railboost info");
                return true;
            case "configuration":
                if (!(player.isInsideVehicle() && player.getVehicle() instanceof Minecart)) {
                    player.sendMessage("§cYou must be sitting in a minecart!");
                    return true;
                }
                UUID configCartId = player.getVehicle().getUniqueId();

                if (args.length < 2) {
                    player.sendMessage("§c/railboost configuration <true|false>");
                    player.sendMessage("§7true - Activates the minecart for RailBoost");
                    player.sendMessage("§7false - Deactivates the minecart for RailBoost");
                    return true;
                }

                boolean active = Boolean.parseBoolean(args[1]);

                if (args[1].equalsIgnoreCase("true") || args[1].equals("1") || args[1].equalsIgnoreCase("on")) {
                    active = true;
                } else if (args[1].equalsIgnoreCase("false") || args[1].equals("0") || args[1].equalsIgnoreCase("off")) {
                    active = false;
                }

                setLoreActive(configCartId, active);

                if (active) {
                    player.sendMessage("§aThis minecart is now activated for RailBoost and will be saved.");
                } else {
                    player.sendMessage("§cThis minecart is now deactivated for RailBoost and will not be saved anymore.");
                }
                return true;

            default:
                player.sendMessage("§cUnknown command. Use /railboost help for an overview.");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.addAll(Arrays.asList("speed", "autopickup", "storage", "speedometer", "chunkload", "magnet", "effect", "autosit", "blacklist", "info", "help", "configuration"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "autopickup":
                    list.addAll(Arrays.asList("true", "false", "radius"));
                    break;
                case "blacklist":
                    list.addAll(Arrays.asList("add", "remove", "list"));
                    break;
                case "speed":
                    list.addAll(Arrays.asList("1", "2", "3", "4", "5", "6"));
                    break;
                case "effect":
                    list.addAll(Arrays.asList("true", "false", "type"));
                    break;
                case "magnet":
                    list.addAll(Arrays.asList("true", "false"));
                    break;
                case "autosit":
                    list.addAll(Arrays.asList("true", "false"));
                    break;
                case "speedometer":
                    list.addAll(Arrays.asList("true", "false"));
                    break;
                case "chunkload":
                    list.addAll(Arrays.asList("true", "false"));
                    break;
                case "storage":
                    break;
                case "help":
                    break;
                case "configuration":
                    list.addAll(Arrays.asList("true", "false"));
                    break;
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("blacklist") && (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove"))) {
            String prefix = args[2].toLowerCase();
            Set<String> suggestions = new HashSet<>();
            for (String category : materialCategories.keySet()) {
                if (category.toLowerCase().startsWith(prefix)) {
                    suggestions.add(category);
                }
            }
            for (Material mat : Material.values()) {
                String matName = mat.name().toLowerCase();
                if (matName.startsWith(prefix)) {
                    suggestions.add(matName);
                }
            }
            list.addAll(suggestions);
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("autopickup") && args[1].equalsIgnoreCase("radius")) {
                list.addAll(Arrays.asList("1", "2", "3", "4", "5"));
            } else if (args[0].equalsIgnoreCase("effect") && args[1].equalsIgnoreCase("type")) {
                list.addAll(Arrays.asList("FLAME", "END_ROD", "PORTAL", "CLOUD", "VILLAGER_HAPPY", "CRIT", "SPELL_WITCH", "DRAGON_BREATH", "HEART"));
            }
        }
        return list;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
    }

    @EventHandler
    public void onPlayerExitVehicle(org.spigotmc.event.entity.EntityDismountEvent event) {
        if (event.getEntity() instanceof Player && event.getDismounted() instanceof Minecart) {
            Player player = (Player) event.getEntity();
            Minecart cart = (Minecart) event.getDismounted();

            if (autoSitEnabled.getOrDefault(cart.getUniqueId(), false)) {
                exitCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            }
        }
    }

    @EventHandler
    public void onPlayerMoveNearMinecart(org.bukkit.event.player.PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.isInsideVehicle()) return;

        long now = System.currentTimeMillis();
        Long lastExit = exitCooldowns.get(player.getUniqueId());
        if (lastExit != null && now - lastExit < EXIT_COOLDOWN_TIME) {
            return;
        }

        Collection<Entity> nearby = player.getWorld().getNearbyEntities(player.getLocation(), 0.7, 0.7, 0.7,
            entity -> entity instanceof Minecart && autoSitEnabled.getOrDefault(entity.getUniqueId(), false));

        if (!nearby.isEmpty()) {
            Minecart cart = (Minecart) nearby.iterator().next();
            cart.addPassenger(player);
        }
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player) {
            Player player = (Player) event.getEntered();
            if (event.getVehicle() instanceof Minecart) {
                Minecart cart = (Minecart) event.getVehicle();
                UUID cartId = cart.getUniqueId();

                if (autoSitEnabled.getOrDefault(cartId, false)) {
                    getLogger().fine("Player " + player.getName() + " was placed in the minecart via Auto-Sit.");
                }
            }
        }
    }

    @EventHandler
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        if (event.getVehicle() instanceof Minecart) {
            Minecart cart = (Minecart) event.getVehicle();
            UUID cartId = cart.getUniqueId();

            Inventory inv = minecartStorage.get(cartId);
            if (inv != null) {
                for (ItemStack item : inv.getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        cart.getWorld().dropItemNaturally(cart.getLocation(), item);
                    }
                }
            }

            minecartStorage.remove(cartId);
            activatedMinecarts.remove(cartId);

            minecartSpeeds.remove(cartId);
            autoPickupEnabled.remove(cartId);
            autoPickupRadius.remove(cartId);
            speedometerEnabled.remove(cartId);
            chunkloadEnabled.remove(cartId);
            magnetEnabled.remove(cartId);
            effectEnabled.remove(cartId);
            effectType.remove(cartId);
            autoSitEnabled.remove(cartId);
            blacklist.remove(cartId);

            saveMinecartSettings();
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Minecart) {
            Minecart cart = (Minecart) entity;
            UUID cartId = cart.getUniqueId();

            Inventory inv = minecartStorage.get(cartId);
            if (inv != null) {
                for (ItemStack item : inv.getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        cart.getWorld().dropItemNaturally(cart.getLocation(), item);
                    }
                }
            }

            minecartStorage.remove(cartId);
            activatedMinecarts.remove(cartId);

            minecartSpeeds.remove(cartId);
            autoPickupEnabled.remove(cartId);
            autoPickupRadius.remove(cartId);
            speedometerEnabled.remove(cartId);
            chunkloadEnabled.remove(cartId);
            magnetEnabled.remove(cartId);
            effectEnabled.remove(cartId);
            effectType.remove(cartId);
            autoSitEnabled.remove(cartId);
            blacklist.remove(cartId);

            saveMinecartSettings();
        }
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!(event.getVehicle() instanceof Minecart)) return;

        Minecart cart = (Minecart) event.getVehicle();
        UUID cartId = cart.getUniqueId();

        if (activatedMinecarts.contains(cartId)) {
            double speedFactor = minecartSpeeds.getOrDefault(cartId, 1.0);

            double baseSpeed = 0.4;
            double targetSpeed = baseSpeed * speedFactor;
            cart.setMaxSpeed(targetSpeed);

            if (cart.getVelocity().lengthSquared() > 0.001) {
                Vector velocity = cart.getVelocity();
                Vector direction = velocity.clone().normalize();
                double currentSpeed = velocity.length();

                double adjustmentRate = 0.4;
                if (speedFactor >= 3.0) {
                    adjustmentRate = 0.6;
                }
                if (speedFactor >= 6.0) {
                    adjustmentRate = 0.8;
                }

                double newSpeed;
                if (currentSpeed < targetSpeed) {
                    newSpeed = Math.min(targetSpeed, currentSpeed + (targetSpeed - currentSpeed) * adjustmentRate);

                    if (speedFactor > 3.0) {
                        newSpeed = Math.min(targetSpeed, newSpeed + 0.01 * speedFactor);
                    }
                } else if (currentSpeed > targetSpeed) {
                    newSpeed = Math.max(targetSpeed, currentSpeed - (currentSpeed - targetSpeed) * 0.2);
                } else {
                    newSpeed = currentSpeed;
                }

                Vector newVelocity = direction.multiply(newSpeed);
                cart.setVelocity(newVelocity);

                if (speedFactor > 6.0 && cart.isOnGround() && newSpeed < 0.1) {
                    if (cart.getPassengers().size() > 0 && cart.getPassengers().get(0) instanceof Player) {
                        Player player = (Player) cart.getPassengers().get(0);
                        cart.setVelocity(player.getLocation().getDirection().normalize().multiply(0.3));
                    }
                }

                if (getConfig().getBoolean("debug", false)) {
                    getLogger().info("Minecart speed adjusted: " + currentSpeed + " -> " + newSpeed +
                                     " (factor: " + speedFactor + ", target: " + targetSpeed + ")");
                }
            }

            if (effectEnabled.getOrDefault(cartId, false)) {
                long now = System.currentTimeMillis();

                if (!lastEffectUpdateTime.containsKey(cartId) ||
                    now - lastEffectUpdateTime.getOrDefault(cartId, 0L) > EFFECT_UPDATE_COOLDOWN) {

                    lastEffectUpdateTime.put(cartId, now);

                    org.bukkit.Particle particle;
                    try {
                        String type = effectType.getOrDefault(cartId, "FLAME");
                        particle = org.bukkit.Particle.valueOf(type);
                    } catch (Exception e) {
                        particle = org.bukkit.Particle.FLAME;
                    }

                    int particleCount = 10;
                    if (speedFactor > 3.0) {
                        particleCount = 15;
                    }
                    if (speedFactor > 6.0) {
                        particleCount = 25;
                    }

                    cart.getWorld().spawnParticle(particle, cart.getLocation(), particleCount, 0.2, 0.2, 0.2, 0.02 * speedFactor);
                }
            }

            if (autoPickupEnabled.getOrDefault(cartId, false)) {
                int radius = autoPickupRadius.getOrDefault(cartId, 3);
                Collection<Entity> nearbyItems = cart.getWorld().getNearbyEntities(
                    cart.getLocation(), radius, radius, radius,
                    entity -> entity instanceof Item
                );

                Set<Material> blocklist = blacklist.getOrDefault(cartId, new HashSet<>());
                Inventory storage = minecartStorage.computeIfAbsent(cartId, k ->
                    Bukkit.createInventory(null, 27, "Minecart Storage")
                );

                for (Entity entity : nearbyItems) {
                    Item item = (Item) entity;
                    ItemStack stack = item.getItemStack();

                    if (blocklist.contains(stack.getType())) {
                        continue;
                    }

                    HashMap<Integer, ItemStack> leftover = new HashMap<>();
                    try {
                        leftover = storage.addItem(stack);
                    } catch (Exception e) {
                        getLogger().warning("Fehler beim Hinzufügen von Items zum Storage: " + e.getMessage());
                    }

                    if (leftover.isEmpty()) {
                        entity.remove();
                    } else {
                        stack.setAmount(leftover.get(0).getAmount());
                    }
                }
            }

            if (chunkloadEnabled.getOrDefault(cartId, false)) {
                org.bukkit.Chunk chunk = cart.getLocation().getChunk();
                if (!chunk.isLoaded()) {
                    chunk.load();
                }

                int chunkX = chunk.getX();
                int chunkZ = chunk.getZ();
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && z == 0) continue;

                        org.bukkit.Chunk nearby = cart.getWorld().getChunkAt(chunkX + x, chunkZ + z);
                        if (!nearby.isLoaded()) {
                            nearby.load();
                        }
                    }
                }
            }
        }
    }

    private void setLoreActive(UUID cartId, boolean active) {
        if (active) {
            activatedMinecarts.add(cartId);
            getLogger().info("Minecart " + cartId + " activated and saved.");
        } else {
            activatedMinecarts.remove(cartId);
            getLogger().info("Minecart " + cartId + " deactivated and not saved.");
        }
    }

    private String getCurrentSpeedDescription(Player player) {
        if (!player.isInsideVehicle() || !(player.getVehicle() instanceof Minecart)) {
            return "N/A";
        }
        UUID cartId = player.getVehicle().getUniqueId();
        double speed = minecartSpeeds.getOrDefault(cartId, 1.0);
        int level = 0;
        for (int i = 0; i < SPEED_LEVELS.length; i++) {
            if (SPEED_LEVELS[i] == speed) {
                level = i + 1;
                break;
            }
        }
        return "§7Level " + level + " (" + String.format("%.2f", speed) + "x)";
    }
}
