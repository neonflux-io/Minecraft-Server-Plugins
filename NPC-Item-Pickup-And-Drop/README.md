# NPCItemPickup Plugin

A Minecraft Spigot/Paper plugin that enhances Citizens2 NPCs by allowing them to pick up items from the ground and drop them when killed. This makes NPCs more interactive and adds interesting gameplay mechanics to your server.

## Features

- **Automatic Item Pickup**: NPCs automatically detect and pick up nearby items
- **Smart Movement**: NPCs can move towards items to pick them up
- **Item Storage**: NPCs store picked-up items in a virtual inventory
- **Death Drops**: When NPCs are killed, they drop their collected items
- **Configurable Filters**: Control which items NPCs can pick up using whitelists/blacklists
- **Admin Commands**: Manage NPC inventories and plugin settings
- **Extensive Configuration**: Customize pickup radius, movement speed, drop chances, and more

## Requirements

- Minecraft Server (Spigot/Paper) 1.16+
- Citizens2 plugin
- Java 8 or higher

## Installation

1. Download the latest release of NPCItemPickup
2. Place the JAR file in your server's `plugins` folder
3. Ensure Citizens2 is installed and working
4. Restart your server
5. Configure the plugin by editing `plugins/NPCItemPickup/config.yml`

## Configuration

The plugin creates a `config.yml` file with extensive customization options:

### Pickup Settings

- `pickup.radius`: How far NPCs can detect items (default: 5.0 blocks)
- `pickup.delay`: How often NPCs check for items (default: 20 ticks = 1 second)
- `pickup.max_items`: Maximum items an NPC can hold (default: 64)
- `pickup.move_to_items`: Whether NPCs move towards items (default: true)
- `pickup.movement_speed`: Speed multiplier when moving to items (default: 1.2)

### Drop Settings

- `drop.enabled`: Whether items are dropped on NPC death (default: true)
- `drop.drop_chance`: Probability of dropping items (0.0-1.0, default: 1.0)
- `drop.scatter_items`: Whether to scatter items around the death location (default: true)
- `drop.scatter_radius`: Radius for scattering items (default: 2.0 blocks)

### Item Filtering

- `item_filter.use_whitelist`: Use whitelist (true) or blacklist (false) mode
- `item_filter.whitelist`: List of allowed items (when whitelist mode is enabled)
- `item_filter.blacklist`: List of blocked items (when blacklist mode is enabled)

## Commands

All commands require the `npcpickup.admin` permission (default: OP).

- `/npcpickup reload` - Reload the plugin configuration
- `/npcpickup toggle` - Enable/disable the plugin functionality
- `/npcpickup status` - Show plugin status and statistics
- `/npcpickup info [npc_name]` - Show information about an NPC's inventory
- `/npcpickup clear <npc_name|all>` - Clear NPC inventories

### Command Examples

```
/npcpickup status
/npcpickup info MyNPC
/npcpickup clear all
/npcpickup clear "Town Guard"
```

## Permissions

- `npcpickup.admin` - Access to all plugin commands (default: OP)

## How It Works

1. **Item Detection**: Every few seconds (configurable), the plugin scans around each spawned NPC for nearby items
2. **Item Filtering**: Items are checked against the whitelist/blacklist configuration
3. **Movement**: If enabled, NPCs will move towards items within their detection radius
4. **Pickup**: When an NPC gets close enough to an item, they pick it up and store it in their virtual inventory
5. **Storage**: Items are stored per-NPC and persist until the NPC dies or the inventory is cleared
6. **Death**: When an NPC dies, their items are dropped at their location (with optional scattering)

## Integration with Citizens2

This plugin is designed to work seamlessly with Citizens2 and its addons:

- **Sentinel Compatibility**: Works with Sentinel NPCs - when they die in combat, they'll drop their collected items
- **NPC Respawning**: Items are tied to NPC instances, so respawned NPCs start with empty inventories
- **Multi-World Support**: Works across all worlds where Citizens2 NPCs are present

## Technical Details

- Items are stored in memory during server runtime
- No database storage - inventories reset on server restart
- Lightweight and efficient - minimal performance impact
- Thread-safe operations for multiplayer servers

## Troubleshooting

### NPCs aren't picking up items

1. Check that `enabled: true` in config.yml
2. Verify Citizens2 is properly installed
3. Ensure NPCs are spawned and not in a protected area
4. Check item filtering settings (whitelist/blacklist)

### Items aren't dropping on death

1. Verify `drop.enabled: true` in config.yml
2. Check `drop.drop_chance` value (should be > 0)
3. Ensure the NPC actually has items in their inventory

### Performance issues

1. Increase `pickup.delay` to reduce frequency of checks
2. Decrease `pickup.radius` to scan smaller areas
3. Enable debug mode to identify any error messages

## Configuration Example

```yaml
enabled: true

pickup:
  radius: 3.0
  delay: 40 # Check every 2 seconds instead of 1
  max_items: 32
  move_to_items: true
  movement_speed: 1.0

drop:
  enabled: true
  drop_chance: 0.8 # 80% chance to drop items
  scatter_items: true
  scatter_radius: 1.5

item_filter:
  use_whitelist: true
  whitelist:
    - "DIAMOND"
    - "GOLD_INGOT"
    - "IRON_INGOT"
    - "EMERALD"
    - "COAL"
```

## Support

If you encounter any issues or have suggestions:

1. Check the console for error messages
2. Enable debug mode in config.yml for detailed logging
3. Verify all dependencies are properly installed
4. Make sure you're using compatible versions

## Building from Source

1. Clone this repository
2. Run `mvn clean package`
3. Find the compiled JAR in the `target` folder

## License

This plugin is released under the MIT License. Feel free to modify and distribute according to the license terms.
