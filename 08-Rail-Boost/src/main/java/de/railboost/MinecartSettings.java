package de.railboost;

import org.bukkit.Material;
import java.util.Set;
import java.util.HashSet;

public class MinecartSettings {

    private String name;
    private int speed = 1;
    private boolean autoPickup = false;
    private int pickupRadius = 3;
    private boolean speedometer = false;
    private boolean chunkLoad = false;
    private boolean magnet = false;
    private boolean effects = false;
    private String effectType = "FLAME";
    private Set<Material> blacklist = new HashSet<>();

    public MinecartSettings(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getSpeed() { return speed; }
    public void setSpeed(int speed) {
        this.speed = Math.max(1, Math.min(6, speed));
    }

    public boolean isAutoPickup() { return autoPickup; }
    public void setAutoPickup(boolean autoPickup) { this.autoPickup = autoPickup; }

    public int getPickupRadius() { return pickupRadius; }
    public void setPickupRadius(int pickupRadius) {
        this.pickupRadius = Math.max(1, Math.min(5, pickupRadius));
    }

    public boolean isSpeedometer() { return speedometer; }
    public void setSpeedometer(boolean speedometer) { this.speedometer = speedometer; }

    public boolean isChunkLoad() { return chunkLoad; }
    public void setChunkLoad(boolean chunkLoad) { this.chunkLoad = chunkLoad; }

    public boolean isMagnet() { return magnet; }
    public void setMagnet(boolean magnet) { this.magnet = magnet; }

    public boolean isEffects() { return effects; }
    public void setEffects(boolean effects) { this.effects = effects; }

    public String getEffectType() { return effectType; }
    public void setEffectType(String effectType) { this.effectType = effectType; }

    public Set<Material> getBlacklist() { return blacklist; }
    public void setBlacklist(Set<Material> blacklist) { this.blacklist = blacklist; }

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(";");
        sb.append(speed).append(";");
        sb.append(autoPickup).append(";");
        sb.append(pickupRadius).append(";");
        sb.append(speedometer).append(";");
        sb.append(chunkLoad).append(";");
        sb.append(magnet).append(";");
        sb.append(effects).append(";");
        sb.append(effectType).append(";");

        for (Material mat : blacklist) {
            sb.append(mat.name()).append(",");
        }

        return sb.toString();
    }

    public static MinecartSettings deserialize(String data) {
        String[] parts = data.split(";");
        if (parts.length < 9) return null;

        MinecartSettings settings = new MinecartSettings(parts[0]);

        try {
            settings.setSpeed(Integer.parseInt(parts[1]));
            settings.setAutoPickup(Boolean.parseBoolean(parts[2]));
            settings.setPickupRadius(Integer.parseInt(parts[3]));
            settings.setSpeedometer(Boolean.parseBoolean(parts[4]));
            settings.setChunkLoad(Boolean.parseBoolean(parts[5]));
            settings.setMagnet(Boolean.parseBoolean(parts[6]));
            settings.setEffects(Boolean.parseBoolean(parts[7]));
            settings.setEffectType(parts[8]);

            if (parts.length > 9 && !parts[9].isEmpty()) {
                String[] materials = parts[9].split(",");
                for (String matName : materials) {
                    if (!matName.isEmpty()) {
                        try {
                            settings.blacklist.add(Material.valueOf(matName));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }

        return settings;
    }

    public MinecartSettings copy() {
        MinecartSettings copy = new MinecartSettings(this.name + "_copy");
        copy.speed = this.speed;
        copy.autoPickup = this.autoPickup;
        copy.pickupRadius = this.pickupRadius;
        copy.speedometer = this.speedometer;
        copy.chunkLoad = this.chunkLoad;
        copy.magnet = this.magnet;
        copy.effects = this.effects;
        copy.effectType = this.effectType;
        copy.blacklist = new HashSet<>(this.blacklist);
        return copy;
    }

    @Override
    public String toString() {
        return "§e" + name + " §7[Speed: " + speed + ", AutoPickup: " + autoPickup +
                ", Magnet: " + magnet + ", Effects: " + effects + "]";
    }
}