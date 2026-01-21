package arearewind.data;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@SerializableAs("AreaBackup")
public class AreaBackup implements ConfigurationSerializable, Serializable {
    private static long nextId = 0;
    private String id;
    private LocalDateTime timestamp;
    private Map<String, BlockInfo> blocks;

    public AreaBackup(LocalDateTime timestamp, Map<String, BlockInfo> blocks) {
        this.id = String.valueOf(nextId++);
        this.timestamp = timestamp;
        this.blocks = blocks;
    }

    public AreaBackup(String id, LocalDateTime timestamp, Map<String, BlockInfo> blocks) {
        this.id = id;
        this.timestamp = timestamp;
        this.blocks = blocks;
    }

    public String getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Map<String, BlockInfo> getBlocks() { return blocks; }

    public int getBlockCount() {
        return blocks.size();
    }

    public boolean containsPosition(String position) {
        return blocks.containsKey(position);
    }

    public BlockInfo getBlockAt(String position) {
        return blocks.get(position);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("timestamp", timestamp != null ? timestamp.toString() : null);
        map.put("blocks", blocks);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static AreaBackup deserialize(Map<String, Object> map) {
        String id = (String) map.get("id");
        LocalDateTime timestamp = map.get("timestamp") != null ? LocalDateTime.parse((String) map.get("timestamp")) : null;
        Map<String, BlockInfo> blocks = (Map<String, BlockInfo>) map.get("blocks");
        return new AreaBackup(id, timestamp, blocks);
    }
}
