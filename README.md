<p align="center">
  <img src="img/img_v1.0.1-mcplugin.png" alt="Minecraft Plugin" width="128" />
</p>
<h1 align="center">Minecraft Server Plugins</h1>
<p align="center">
  <b>A collection of open-source Minecraft server plugins.</b><br>
  <b>Various enhancements for gameplay, administration, and server features.</b>
</p>
<p align="center">
  <a href="https://github.com/BerndHagen/Minecraft-Server-Plugins/releases"><img src="https://img.shields.io/github/v/release/BerndHagen/Minecraft-Server-Plugins?include_prereleases&style=flat-square&color=4CAF50" alt="Latest Release"></a>&nbsp;&nbsp;<a href="https://github.com/BerndHagen/Minecraft-Server-Plugins/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="License"></a>&nbsp;&nbsp;<img src="https://img.shields.io/badge/Java-17+-orange?style=flat-square" alt="Java Version">&nbsp;&nbsp;<img src="https://img.shields.io/badge/Minecraft-1.21+-green?style=flat-square" alt="Minecraft Version">&nbsp;&nbsp;<img src="https://img.shields.io/badge/Platform-Spigot%2FPaper-yellow?style=flat-square" alt="Platform">&nbsp;&nbsp;<img src="https://img.shields.io/badge/Status-Active-brightgreen?style=flat-square" alt="Status">&nbsp;&nbsp;<a href="https://github.com/BerndHagen/Minecraft-Server-Plugins/issues"><img src="https://img.shields.io/github/issues/BerndHagen/Minecraft-Server-Plugins?style=flat-square&color=orange" alt="Open Issues"></a>
</p>

This collection provides **open-source Minecraft plugins** designed to add functionality to server gameplay. Originally developed for an own Minecraft server to enhance industrial automation, area protection, and backup capabilities, these plugins are now shared with the community to benefit other server administrators.

Each plugin is built with performance in mind and offers extensive configuration options for server administrators. Whether you're running a survival server, creative server, or custom game mode, these plugins provide additional features for your server. All plugins are released under the **MIT License**, allowing complete freedom to modify, distribute, and integrate them into your server environment.

### **Plugin Overview**

- **Advanced Achievements:** Comprehensive achievement system with custom rewards, progress tracking, and database integration
- **Area Rewind:** Advanced area protection and backup system with undo/redo functionality, automatic backups, and GUI management
- **Piston Crusher:** Automated block crushing system using pistons with configurable materials and multipliers  
- **Rail Boost:** Enhanced minecart system with speed control, auto-pickup, storage, and advanced transportation features
- **Super Enchantments:** Advanced enchantment system allowing enchantments beyond vanilla limits with level 1-255 support

### **Key Features**

- **Open Source:** Complete source code available under MIT License for modification and improvement
- **Performance Optimized:** Efficient code designed for minimal server impact and smooth gameplay
- **Highly Configurable:** Extensive configuration options to customize behavior for your server needs
- **Database Integration:** Advanced data storage and player progress tracking capabilities
- **Modern API Support:** Built for Minecraft 1.21+ with backwards compatibility considerations
- **Admin Commands:** Comprehensive command systems for easy server management and configuration
- **GUI Interfaces:** User-friendly graphical interfaces for easy management and configuration

### **Supported Platforms**

These plugins are compatible with major Minecraft server platforms:

- **Server Software:** `Spigot`, `Paper`, `Purpur`, `CraftBukkit`
- **Minecraft Versions:** `1.21.3`, `1.21.4`, `1.21.5`, `1.21.6`, `1.21.7`, `1.21.8` (also compatible with higher versions)
- **Java Requirements:** `Java 17+`

### **Plugin Compatibility**

All plugins are designed to work seamlessly together and with popular server plugins:

- **Economy Integration:** Vault support for economy rewards and transactions
- **Permission Systems:** Compatible with LuckPerms, PermissionsEx, and other permission managers
- **WorldGuard Integration:** Respect region protections and building restrictions
- **Database Support:** MySQL, SQLite, and file-based storage options

## **Table of Contents**

1. [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Installation Steps](#installation-steps)
    - [First Launch & Configuration](#first-launch--configuration)
    - [Verifying Installation](#verifying-installation)
2. [Advanced Achievements](#advanced-achievements)
    - [Core Features](#core-features)
    - [Administrative & Player Commands](#administrative--player-commands)
3. [Area Rewind](#area-rewind)
    - [Core Features](#core-features-1)
    - [Getting Started](#getting-started-1)
    - [Player Commands](#player-commands)
    - [Administrative Commands](#administrative-commands)
4. [Piston Crusher](#piston-crusher)
    - [Core Features](#core-features-2)
    - [Example Setup](#example-setup)
    - [Administrative Commands](#administrative-commands-1)
5. [Rail Boost](#rail-boost)
    - [Core Features](#core-features-3)
    - [Player Commands](#player-commands-1)
6. [Super Enchantments](#super-enchantments)
    - [Core Features](#core-features-4)
    - [Player Commands](#player-commands-2)
7. [License](#license)
8. [Screenshots](#screenshots)

## **Getting Started**

### **Prerequisites**
- Minecraft server 1.21 or higher (Spigot, Paper, or compatible fork)
- Java 17 or newer (Java 21 recommended)
- (Optional) Vault for economy features, LuckPerms or PermissionsEx for permissions, MySQL for advanced database support

### **Installation Steps**
1. Download the latest plugin `.jar` files from the [Releases](https://github.com/BerndHagen/Minecraft-Server-Plugins/releases) page.
2. Stop your Minecraft server if it is running.
3. Copy the downloaded `.jar` files into your server's `plugins` folder.
4. (Optional) Add Vault, LuckPerms, or other dependencies to the `plugins` folder if needed.
5. Start your server again.

### **First Launch & Configuration**
- On first start, each plugin will generate its own configuration files in the `plugins/PluginName/` directory.
- Edit these config files as needed, or use in-game commands for setup (see plugin sections below).

### **Verifying Installation**
- Use `/plugins` in-game to check if the plugins are loaded and active.
- Use `/version <PluginName>` to check the version and status of each plugin.
- If a plugin does not load, check the server console for error messages (e.g., missing dependencies or wrong Java version).

For plugin-specific commands and advanced configuration, see the sections below.

## **Advanced Achievements**

An advanced achievement system that tracks player progress for various task types (block breaking, crafting, mob kills, etc.) and provides highly configurable rewards. All progress and claim status are stored persistently per player. The plugin architecture allows for custom extensions via API.

### Core Features:
- **Progress Tracking:** Real-time tracking for all task types (block breaking, crafting, mob kills, etc.)
- **Reward System:** Economy (Vault), items, XP, titles, commands – all configurable per achievement
- **Database:** Supports MySQL and SQLite, including asynchronous load/save
- **GUI:** Inventory-based GUI with pages, navigation, and progress display (BossBar/ActionBar)
- **API:** Add custom achievements and triggers via API
- **Messages:** All messages and prefixes are fully customizable (YAML)
- **Prerequisites:** Achievements can depend on other achievements
- **Sound/Firework:** Unlocks can optionally play a sound and firework

### Administrative & Player Commands:

| Command | Description |
|---------|-------------|
| `/achievementadmin reload` | Reloads all configuration files and achievements |
| `/achievementadmin reset <player>` | Resets a player's progress |
| `/achievementadmin give <player> <id>` | Manually awards an achievement |
| `/achievementadmin create` | Starts an interactive creation dialog in chat |
| `/achievementadmin edit <id>` | Edits an existing achievement |
| `/achievementadmin delete <id>` | Permanently deletes an achievement |
| `/achievementadmin list [category] [page]` | Lists achievements with filtering and pagination |
| `/achievementadmin gui` | Opens the achievements GUI |
| `/achievementadmin info <id>` | Shows all details for an achievement |
| `/achievementadmin progress [id]` | Shows progress for all/specific achievements |
| `/achievementadmin help` | Shows all available commands |

**Aliases:** `/achadmin`, `/ach` (all subcommands as above)

## **Area Rewind**

A comprehensive area protection and backup system that allows players to create protected zones with automatic backups, undo/redo functionality, and advanced visualization features. Perfect for servers that need reliable area management with rollback capabilities.

### Core Features:
- **Enhanced GUI System:** Comprehensive graphical interface with main menu, area management with creation wizard, advanced backup management with visual timeline, permission and trust management interfaces, visualization controls, and administrative tools
- **Area Protection:** Create protected areas with position selection using wooden hoe tool
- **Automatic Backups:** Scheduled backups with configurable intervals and retention policies
- **Manual Backups:** Create backups on demand for important moments
- **Undo/Redo System:** Full undo/redo functionality for area changes with history tracking
- **Advanced Restoration:** Restore areas to any previous backup state with preview functionality and time-based rollback
- **Permission System:** Owner and trusted player system with granular permissions
- **Visualization:** Particle-based area boundary visualization with customizable effects
- **Import/Export:** Export and import area data for portability, including WorldEdit schematic export
- **WorldEdit Integration:** Enhanced functionality if WorldEdit is present
- **Block State Support:** Full support for containers with contents (chests, barrels, etc.), signs with text, banners with patterns, and all complex block states
- **Database Storage:** Persistent storage with file-based backup system
- **Custom Icons:** Set custom icons for areas and backups
- **Interval Management:** Automatic backup intervals and restoration scheduling
- **Change Detection:** Scan for changes since last backup with comparison tools

### Getting Started:
1. **Selection:** Use a wooden hoe to select two corners of your area (left-click pos1, right-click pos2)
2. **Creation:** Use `/rewind save <name>` to create a protected area
3. **Backup:** Use `/rewind backup <area>` to create manual backups
4. **Management:** Use `/rewind gui` for easy graphical management

### Player Commands:

| Command | Description |
|---------|-------------|
| `/rewind` | Open the AreaRewind management GUI |
| `/rewind tool` | Get the area selection tool (wooden hoe) |
| `/rewind pos1` | Set position 1 to block you're looking at |
| `/rewind pos2` | Set position 2 to block you're looking at |
| `/rewind save <name>` | Create protected area with current selection |
| `/rewind list [owned/trusted/all]` | List protected areas with filtering |
| `/rewind info <area>` | Show detailed information about an area |
| `/rewind teleport <area>` | Teleport to the center of an area |
| `/rewind backup <area>` | Create manual backup of an area |
| `/rewind restore <area> <backup_id/latest/oldest>` | Restore area from backup |
| `/rewind undo <area>` | Undo last change to area |
| `/rewind history <area> [page]` | View backup history with pagination |
| `/rewind rollback <area> <time>` | Rollback to specific time (e.g., 2h, 30m, 1d) |
| `/rewind preview <area> [backup_id] [particle_type]` | Preview backup with particles |
| `/rewind show <area>` | Visualize area boundaries |
| `/rewind hide [area]` | Hide area visualization |
| `/rewind trust <area> <player>` | Add trusted player to area |
| `/rewind untrust <area> <player>` | Remove trusted player from area |
| `/rewind export` | Export area's latest backup to WorldEdit .schem file |

### Administrative Commands:

| Command | Description |
|---------|-------------|
| `/rewind reload` | Reload plugin configuration |
| `/rewind delete <area>` | Delete area and all its backups |
| `/rewind rename <old_name> <new_name>` | Rename an area |
| `/rewind expand <area> <direction> <amount>` | Expand area in specified direction |
| `/rewind contract <area> <direction> <amount>` | Shrink area in specified direction |
| `/rewind permission <add/remove/list> <area> [player]` | Manage area permissions |
| `/rewind seticon <area> <material>` | Set custom icon for area |
| `/rewind seticon backup <area> <backup_id> <material>` | Set custom icon for specific backup |
| `/rewind cleanup <area> [days]` | Clean up old backups (default: 7 days) |
| `/rewind scan <area>` | Scan for changes since last backup |
| `/rewind diff <area> <id1> <id2>` | Compare two backups and show differences |
| `/rewind interval <set/remove/check> <area> [minutes] [backup_id]` | Manage automatic backup intervals |
| `/rewind restoreblock <area> <backup_id/latest/oldest> [world]` | Restore area from backup (for command blocks and console) |
| `/rewind status` | Show system status and statistics |
| `/rewind help` | Show help information |

**Aliases:** `/ar`, `/arearewind`  
**Tool:** Wooden Hoe for area selection  
**Command Aliases:** Most commands support shortened versions (e.g., `perm` for `permission`, `tp` for `teleport`, `compare` for `diff`)

## **Piston Crusher**

An automation plugin that allows pistons to crush only whitelisted blocks into multiple items, with a configurable multiplier. All settings can be changed live via command, no server restart required.

### Core Features:
- **Block Whitelist:** Only allowed blocks are crushed (configurable)
- **Output Multiplier:** Sets how many items are dropped per block
- **Crusher Block:** Must be movable by pistons (e.g., no obsidian, bedrock, etc.)
- **Tab Completion:** Available for all material names

### Example Setup

To build a working piston crusher, arrange the blocks in a straight line as follows:

```
[Piston] [Block to be crushed] [Crusher Block]
```

**How it works:**
- When the piston extends, it pushes the block to be crushed into the crusher block
- If the block to be crushed is on the whitelist, it will be destroyed and drop multiplied items according to the plugin settings
- The piston must be facing directly toward the block to be crushed, which is then directly adjacent to the crusher block

Make sure there is nothing blocking the piston from pushing the block into the crusher block. Only blocks on the whitelist will be crushed and multiplied.

### Administrative Commands:
| Command | Description |
|--------|--------------|
| `/pistoncrusher whitelist add <Material>` | Adds a block to the whitelist |
| `/pistoncrusher whitelist remove <Material>` | Removes a block from the whitelist |
| `/pistoncrusher whitelist list` | Shows all allowed blocks |
| `/pistoncrusher multiplier <Value>` | Sets the multiplier (e.g. 2.5) |
| `/pistoncrusher multiplier` | Shows the current multiplier |
| `/pistoncrusher crusherblock <Material>` | Sets the crusher block |
| `/pistoncrusher crusherblock` | Shows the current crusher block |

**Note:** Material names must be valid (`cobblestone`, `gold_block`, ...). Multiplier ≥ 1.0. Changes take effect immediately.

## **Rail Boost**

A comprehensive minecart enhancement plugin that transforms vanilla minecart transportation into a powerful and customizable system. Each minecart can be individually configured with speed levels, automated item collection, built-in storage, and advanced physics improvements for reliable rail-based transportation networks.

### Core Features:
- **Speed Control:** 6 configurable speed levels (0.25x to 4.0x) with intelligent physics handling for curves and uphill sections
- **Auto-Pickup System:** Automatic item collection within configurable radius (1-5 blocks) with customizable blacklist filtering
- **Storage Integration:** Each minecart has its own 27-slot inventory accessible via GUI, with automatic item sorting
- **Advanced Physics:** Enhanced curve navigation, uphill momentum preservation, and anti-stuck mechanisms for smooth travel
- **Magnetism System:** Optional minecart-to-minecart attraction for train formation with intelligent collision prevention
- **Visual Effects:** Customizable particle trails with intensity scaling based on speed and multiple particle types
- **Performance Tools:** Real-time speedometer with BossBar display showing current velocity in km/h
- **Automation Features:** Auto-sit functionality for seamless boarding and chunkloading for uninterrupted long-distance travel
- **Persistent Configuration:** All settings are saved per-minecart and persist through server restarts

### Player Commands:
| Command | Description |
|---------|-------------|
| `/railboost speed <1-6>` | Sets speed level (0.25x to 4.0x multiplier) |
| `/railboost autopickup <true/false>` | Toggles automatic item collection |
| `/railboost autopickup radius <1-5>` | Sets pickup radius in blocks |
| `/railboost storage` | Opens minecart's dedicated inventory |
| `/railboost speedometer <true/false>` | Shows/hides real-time speed display |
| `/railboost chunkload <true/false>` | Enables chunk loading while traveling |
| `/railboost magnet <true/false>` | Toggles minecart magnetism for train formation |
| `/railboost effect <true/false>` | Enables particle trail effects |
| `/railboost effect type <particle>` | Sets particle type (FLAME, HEART, CLOUD, etc.) |
| `/railboost autosit <true/false>` | Automatic boarding when approaching minecart |
| `/railboost blacklist add/remove <item>` | Manages auto-pickup item filter |
| `/railboost blacklist list` | Shows current blacklisted items |
| `/railboost info` | Displays all current minecart settings |

**Note:** Most commands require sitting in an activated minecart. Use `/railboost info` to verify activation status.

**Aliases:** `/rb` – **Activation:** Automatic when using any command while in a minecart

## **Super Enchantments**

A comprehensive enchantment system that extends vanilla Minecraft enchanting capabilities by allowing all vanilla enchantments with levels 1-255, plus a collection of powerful custom enchantments with unique abilities. Each enchantment is carefully balanced with configurable cooldowns, effects, and compatibility checks.

### Core Features:
- **Enhanced Vanilla Enchanting:** Apply any vanilla enchantment with levels 1-255, breaking traditional level limits
- **Custom Enchantments:** 9 unique custom enchantments with special abilities and visual effects
- **Smart Compatibility:** Automatic item compatibility checking - only valid enchantments are suggested
- **Intelligent Tab Completion:** Context-aware suggestions showing only applicable enchantments for held items
- **Configurable System:** Extensive configuration options for cooldowns, damage, effects, and behavior
- **Cooldown Management:** Built-in cooldown system with optional display messages for custom enchantments
- **Persistent Storage:** Custom enchantments are stored using Minecraft's persistent data containers
- **Visual Feedback:** Particle effects, sounds, and visual indicators for enchantment activation

### Custom Enchantments:

#### **Combat Enchantments**
- **Arrow Storm** (Level 1-5): Right-click to fire multiple arrows with increased damage and speed. Higher levels reduce cooldown and spread while increasing arrow count
- **Blazing Shot** (Level 1-5): Launch fireballs that explode on impact. Advanced levels fire multiple projectiles with increased explosion power
- **Venom Strike** (Level 1-5): Chance to poison targets on hit with increasing duration and intensity based on enchantment level
- **Thunderbolt** (Level 1-5): Chance to strike enemies with lightning, with higher levels creating multiple strikes in a wider area
- **Frostbite** (Level 1-5): Freeze enemies on hit, applying slowness and mining fatigue effects that scale with level
- **Soul Drain** (Level 1-5): Life-steal enchantment that heals the player based on damage dealt, with healing scaling by level

#### **Utility Enchantments**
- **Void Step** (Level 1-3): Teleport forward through obstacles and terrain. Higher levels increase teleport distance and reduce cooldown
- **Pack Leader** (Level 1-3): Summon tamed wolves to fight alongside you. Level determines wolf count, duration, and combat effectiveness

### Enchantment Compatibility:
- **Weapon Enchantments:** Swords, axes, tridents, and some utility items
- **Tool Enchantments:** All vanilla tools plus special items like sticks and rods
- **Ranged Enchantments:** Bows, crossbows for projectile-based abilities
- **Special Items:** Custom enchantments can be applied to themed items (e.g., bones for Pack Leader, ender pearls for Void Step)

### Player Commands:
| Command | Description | Permission |
|---------|-------------|------------|
| `/superenchant <enchantment> [level]` | Apply vanilla enchantment (1-255) | `superenchantments.use` |
| `/superenchant customs <enchantment> [level]` | Apply custom enchantment | `superenchantments.use` |
| `/enchantlist` | Show available enchantments for held item | `superenchantments.list` |
| `/superenchant togglecooldown` | Toggle cooldown display messages (admin) | `superenchantments.admin` |

### Usage Examples:
```
/superenchant sharpness 100          # Apply Sharpness 100 to held weapon
/superenchant customs arrow_storm 3   # Apply Arrow Storm level 3
/superenchant efficiency 255          # Maximum vanilla efficiency
/enchantlist                         # See what can be enchanted
```

**Aliases:** `/senchant`, `/se` for `/superenchant` | `/elist`, `/el` for `/enchantlist`  
**Requirements:** Item must be held in main hand and be enchantable  
**Permissions:** Default permissions allow listing (`superenchantments.list: true`) but restrict usage to operators (`superenchantments.use: op`)

**Note:** Custom enchantments use a sophisticated trigger system that responds to different events (combat, interaction, projectile hits) and include built-in cooldowns to maintain game balance. All effects are configurable through the plugin's config.yml file.

## **License**

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

## **Screenshots**

The following screenshots demonstrate the core functionality of each plugin, including the achievement system interface, area protection and backup management, automation, and enhanced minecart features.

<table>
  <tr>
    <th>Plugin - Advanced Achievement</th>
    <th>Plugin - Advanced Achievement</th>
    <th>Plugin - Area Rewind</th>
  </tr>
  <tr>
    <td><a href="https://github.com/BerndHagen/Minecraft-Server-Plugins/raw/main/img/img_v1.0.1-mcplugin-progress.png" target="_blank" rel="noopener noreferrer">
      <img src="https://github.com/BerndHagen/Minecraft-Server-Plugins/raw/main/img/img_v1.0.1-mcplugin-progress.png" alt="Achievement Progress" width="300">
    </a></td>
    <td><a href="https://github.com/BerndHagen/Minecraft-Server-Plugins/raw/main/img/img_v1.0.1-mcplugin-unlocked.png" target="_blank" rel="noopener noreferrer">
      <img src="https://github.com/BerndHagen/Minecraft-Server-Plugins/raw/main/img/img_v1.0.1-mcplugin-unlocked.png" alt="Achievement Unlocked" width="300">
    </a></td>
    <td><a href="https://github.com/BerndHagen/Minecraft-Server-Plugins/raw/main/img/img_v1.0.1-mcplugin-arearewind.png" target="_blank" rel="noopener noreferrer">
      <img src="https://github.com/BerndHagen/Minecraft-Server-Plugins/raw/main/img/img_v1.0.1-mcplugin-arearewind.png" alt="Area Rewind GUI" width="300">
    </a></td>
  </tr>
  <tr>
    <th>Plugin - Piston Crusher</th>
    <th>Plugin - Railboost</th>
    <th>Plugin - Super Enchantments</th>
  </tr>
  <tr>
    <td><a href="https://github.com/BerndHagen/Minecraft-Server-Plugins/raw/main/img/img_v1.0.1-mcplugin-crusher.png" target="_blank" rel="noopener noreferrer">
      <img src="https://github.com/BerndHagen/Minecraft-Server-Plugins/raw/main/img/img_v1.0.1-mcplugin-crusher.png" alt="Piston Crusher" width="300">
    </a></td>
    <td><a href="https://github.com/BerndHagen/Minecraft-Server-Plugins/raw/main/img/img_v1.0.1-mcplugin-effect.png" target="_blank" rel="noopener noreferrer">
      <img src="https://github.com/BerndHagen/Minecraft-Server-Plugins/raw/main/img/img_v1.0.1-mcplugin-effect.png" alt="Magnet Feature" width="300">
    </a></td>
    <td><a href="https://github.com/BerndHagen/Minecraft-Server-Plugins/raw/main/img/img_v1.0.1-mcplugin-enchantment.png" target="_blank" rel="noopener noreferrer">
      <img src="https://github.com/BerndHagen/Minecraft-Server-Plugins/raw/main/img/img_v1.0.1-mcplugin-enchantment.png" alt="Super Enchantment" width="300">
    </a></td>
  </tr>
</table>
