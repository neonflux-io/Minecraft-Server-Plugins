# Quick Installation Guide

## What you need:

1. A Minecraft server running Spigot or Paper (1.16+)
2. Citizens2 plugin installed
3. The Sentinel addon for Citizens2 (if you want combat NPCs)

## Installation Steps:

1. **Copy the plugin file:**

   - Take the file `target/NPCItemPickup.jar` from this project
   - Copy it to your server's `plugins` folder

2. **Start your server:**

   - The plugin will create a configuration file automatically
   - Default settings should work well for most servers

3. **Test the plugin:**
   - Create an NPC with Citizens2: `/npc create TestNPC`
   - Drop some items near the NPC
   - Watch as the NPC picks them up!
   - Kill the NPC to see the items drop

## Basic Commands:

- `/npcpickup status` - Check if the plugin is working
- `/npcpickup info` - Stand near an NPC to see what items they're carrying
- `/npcpickup toggle` - Enable/disable the plugin temporarily

## Configuration Tips:

1. **Edit `plugins/NPCItemPickup/config.yml`** to customize:

   - `pickup.radius: 5.0` - How far NPCs can see items (in blocks)
   - `pickup.max_items: 64` - Maximum items per NPC
   - `drop.enabled: true` - Whether NPCs drop items when killed

2. **Item Filtering** (to control what NPCs pick up):

   ```yaml
   item_filter:
     use_whitelist: true
     whitelist:
       - "DIAMOND"
       - "GOLD_INGOT"
       - "IRON_INGOT"
   ```

3. **Performance Tuning** (for servers with many NPCs):
   ```yaml
   pickup:
     delay: 40 # Check less frequently (every 2 seconds)
     radius: 3.0 # Smaller pickup radius
   ```

## Troubleshooting:

- **NPCs not picking up items?** Check that Citizens2 is installed and NPCs are spawned
- **No items dropping on death?** Make sure `drop.enabled: true` in config
- **Performance issues?** Increase the `pickup.delay` value in config

Enjoy your enhanced NPCs! 🎮
