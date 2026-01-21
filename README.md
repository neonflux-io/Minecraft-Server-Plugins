<p align="center">
  <img src="https://raw.githubusercontent.com/BerndHagen/Minecraft-Server-Plugins/main/img/minecraft-plugins-logo.png" alt="Minecraft Plugins Logo" width="128" />
</p>
<h1 align="center">Minecraft Server Plugins</h1>
<p align="center">
  <b>A collection of open-source Minecraft server plugins.</b><br>
  <b>Plugins for achievements, automation, and transportation systems.</b>
</p>
<p align="center">
  <a href="https://github.com/BerndHagen/Minecraft-Server-Plugins/releases"><img src="https://img.shields.io/github/v/release/BerndHagen/Minecraft-Server-Plugins?include_prereleases&style=flat-square&color=4CAF50" alt="Latest Release"></a>&nbsp;&nbsp;<a href="https://github.com/BerndHagen/Minecraft-Server-Plugins/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="License"></a>&nbsp;&nbsp;<img src="https://img.shields.io/badge/Java-17+-orange?style=flat-square" alt="Java Version">&nbsp;&nbsp;<img src="https://img.shields.io/badge/Minecraft-1.19+-green?style=flat-square" alt="Minecraft Version">&nbsp;&nbsp;<img src="https://img.shields.io/badge/Platform-Spigot%2FPaper-yellow?style=flat-square" alt="Platform">&nbsp;&nbsp;<img src="https://img.shields.io/badge/Status-Active-brightgreen?style=flat-square" alt="Status">&nbsp;&nbsp;<a href="https://github.com/BerndHagen/Minecraft-Server-Plugins/issues"><img src="https://img.shields.io/github/issues/BerndHagen/Minecraft-Server-Plugins?style=flat-square&color=orange" alt="Open Issues"></a>
</p>

This collection provides **open-source Minecraft plugins** designed to add functionality to server gameplay. Each plugin is built with performance in mind and offers configuration options for server administrators.

Whether you're running a survival server, creative server, or custom game mode, these plugins provide additional features for your server. All plugins are released under the **MIT License**, allowing complete freedom to modify, distribute, and integrate them into your server environment.

### **Plugin Overview**

- **Advanced Achievements:** Comprehensive achievement system with custom rewards, progress tracking, and database integration
- **Piston Crusher:** Automated block crushing system using pistons with configurable materials and multipliers  
- **Rail Boost:** Enhanced minecart system with speed control, auto-pickup, storage, and advanced transportation features

### **Key Features**

- **Open Source:** Complete source code available under MIT License for modification and improvement
- **Performance Optimized:** Efficient code designed for minimal server impact and smooth gameplay
- **Highly Configurable:** Extensive configuration options to customize behavior for your server needs
- **Database Integration:** Advanced data storage and player progress tracking capabilities
- **Modern API Support:** Built for Minecraft 1.19+ with backwards compatibility considerations
- **Admin Commands:** Comprehensive command systems for easy server management and configuration

### **Supported Platforms**

These plugins are compatible with major Minecraft server platforms:

- **Server Software:** `Spigot`, `Paper`, `Purpur`, `CraftBukkit`
- **Minecraft Versions:** `1.19+`, `1.20+`, `1.21+`
- **Java Requirements:** `Java 17+` (recommended Java 21)

### **Plugin Compatibility**

All plugins are designed to work seamlessly together and with popular server plugins:

- **Economy Integration:** Vault support for economy rewards and transactions
- **Permission Systems:** Compatible with LuckPerms, PermissionsEx, and other permission managers
- **WorldGuard Integration:** Respect region protections and building restrictions
- **Database Support:** MySQL, SQLite, and file-based storage options

## **Table of Contents**

1. [Installation Guide](#installation-guide)
   - [Prerequisites](#prerequisites)
   - [Download and Setup](#download-and-setup)
   - [Configuration](#configuration)
2. [Getting Started](#getting-started)
   - [Server Setup](#server-setup)
   - [Player Commands](#player-commands)
   - [Admin Configuration](#admin-configuration)
3. [Advanced Achievements](#advanced-achievements)
4. [Piston Crusher](#piston-crusher)
5. [Rail Boost](#rail-boost)
6. [License](#license)
7. [Screenshots](#screenshots)

## **Installation Guide**

### **Prerequisites**

Before installing these plugins, ensure your server meets the following requirements:

**Server Requirements:**
- **Minecraft Version:** 1.19 or higher (1.20+ recommended)
- **Server Software:** Spigot, Paper, or compatible fork
- **Java Version:** Java 17 or higher (Java 21 recommended)
- **RAM:** Minimum 2GB allocated to server (4GB+ recommended)
- **Storage:** At least 100MB free space for plugin files and databases

**Optional Dependencies:**
- **Vault:** Required for economy integration in Advanced Achievements
- **MySQL Server:** For advanced database features (SQLite is used by default)
- **Permission Plugin:** LuckPerms, PermissionsEx, or similar for permission management

### **Download and Setup**

1. **Download Plugin Files**
   - Visit the [Releases](https://github.com/BerndHagen/Minecraft-Server-Plugins/releases) page
   - Download the latest `.jar` files for desired plugins
   - Alternative: Build from source using the instructions below

2. **Install Plugins**
   ```bash
   # Stop your server
   # Copy plugin JAR files to the plugins directory
   cp *.jar /path/to/your/server/plugins/
   
   # Start your server
   ```

3. **Verify Installation**
   - Check server console for successful plugin loading
   - Use `/plugins` command to confirm plugins are active
   - Look for configuration files generated in `/plugins/PluginName/`

### **Configuration**

Each plugin generates configuration files on first startup:

**Advanced Achievements:**
- `config.yml` - Main plugin settings and database configuration
- `achievements.yml` - Achievement definitions and requirements
- `messages.yml` - Customizable player messages and notifications

**Piston Crusher:**
- Plugin stores configuration in memory, use commands for setup
- Settings persist across server restarts

**Rail Boost:**
- Configuration managed through in-game commands
- Player settings are saved per-player and persist across sessions

## **Getting Started**

### **Server Setup**

1. **Initial Configuration**
   ```yaml
   # Example server startup sequence
   # 1. Install plugins
   # 2. Start server to generate configs
   # 3. Stop server and configure settings
   # 4. Restart server with custom configuration
   ```

2. **Permission Setup**
   ```yaml
   # Basic permissions for LuckPerms
   /lp group default permission set advancedachievements.use true
   /lp group admin permission set advancedachievements.admin true
   /lp group admin permission set pistoncrusher.admin true
   ```

### **Player Commands**

**Essential Commands for Players:**
```yaml
/ach                                        # View achievements interface
/railboost info                             # Check current minecart settings
/railboost speed 3                          # Set moderate speed for balanced travel
/railboost autopickup true                  # Enable automatic item collection
/railboost configuration true               # Activate minecart for RailBoost features
```

### **Admin Configuration**

**Quick Setup for Administrators:**
```yaml
# Configure Achievement System
/achievementadmin reload                    # Load achievement configurations

# Setup Piston Crusher automation
/pistoncrusher whitelist add COBBLESTONE    # Add cobblestone to crushable blocks
/pistoncrusher multiplier 2.0               # Set 2x output multiplier for efficiency

# Test Rail Boost functionality
/railboost help                             # Display all available commands and usage
```

# **Advanced Achievements**

A comprehensive achievement system that tracks player progress across multiple categories and provides customizable rewards. Features include progress tracking, database integration, and an intuitive GUI for players to view their achievements.

**Core Features:**
- **Multi-Category Achievements:** Organize achievements by type (combat, building, exploration, etc.)
- **Progress Tracking:** Real-time tracking of player progress with percentage completion
- **Custom Rewards:** Configure economy rewards, items, commands, and experience points
- **Database Integration:** Persistent storage with MySQL and SQLite support
- **Interactive GUI:** User-friendly interface for browsing and tracking achievements
- **API Integration:** Developer API for creating custom achievement triggers
- **Message Customization:** Fully customizable achievement notifications and messages

**Administrative Commands:**

| Command | Description | Usage Example |
|---------|-------------|---------------|
| `/achievementadmin reload` | Reloads all plugin configuration files and achievement definitions | `/achievementadmin reload` |
| `/achievementadmin reset <player>` | Resets all achievement progress for the specified player | `/achievementadmin reset PlayerName` |
| `/achievementadmin give <player> <id>` | Manually awards a specific achievement to a player | `/achievementadmin give PlayerName first_kill` |
| `/achievementadmin stats` | Displays plugin statistics including total achievements and player counts | `/achievementadmin stats` |
| `/achievementadmin create` | Starts an interactive achievement creation process with step-by-step guidance | `/achievementadmin create` |
| `/achievementadmin delete <id>` | Permanently deletes an existing achievement by its unique identifier | `/achievementadmin delete custom_achievement` |
| `/achievementadmin edit <id>` | Opens the achievement editor for modifying existing achievement properties | `/achievementadmin edit mining_expert` |

**Player Commands:**

| Command | Description | Usage Example |
|---------|-------------|---------------|
| `/ach` | Opens the main achievements GUI interface for browsing and tracking progress | `/ach` |
| `/ach list [category] [page]` | Lists all achievements with optional category filtering and pagination | `/ach list combat 2` |
| `/ach progress [achievement_id]` | Displays current progress for all achievements or a specific achievement | `/ach progress mining_master` |
| `/ach info <achievement_id>` | Shows detailed information about a specific achievement including requirements | `/ach info first_diamond` |
| `/ach stats` | Displays personal achievement statistics and completion percentages | `/ach stats` |
| `/ach gui` | Alternative command to open the achievements GUI interface | `/ach gui` |
| `/ach help` | Shows help information and available commands for the achievement system | `/ach help` |

**Command Aliases:**
- `/achievementadmin` can be used as `/achadmin` or `/ach` (for admin commands with parameters)
- Most player commands support partial matching for achievement IDs

# **Piston Crusher**

An automation plugin that allows pistons to crush specific blocks into multiple items, creating efficient resource processing systems. Perfect for industrial-style servers and automated farms.

**Core Features:**
- **Block Whitelist:** Configure which blocks can be crushed by pistons
- **Output Multiplier:** Adjust how many items are produced when blocks are crushed
- **Crusher Block Selection:** Choose which block type acts as the crusher mechanism
- **Admin Controls:** Easy configuration through in-game commands
- **Performance Optimized:** Efficient event handling for minimal server impact

**How It Works:**
1. Configure which blocks can be crushed using the whitelist system
2. Place the configured crusher block (default: Polished Andesite) in your setup
3. When the crusher block touches any whitelisted block, it automatically breaks that block
4. The broken block drops as a collectible item with the configured output multiplier
5. Perfect for creating automated mining systems and resource processing farms

**Administrative Commands:**

| Command | Description | Usage Example |
|---------|-------------|---------------|
| `/pistoncrusher whitelist add <material>` | Adds a block type to the list of materials that can be crushed by pistons | `/pistoncrusher whitelist add COBBLESTONE` |
| `/pistoncrusher whitelist remove <material>` | Removes a block type from the crush whitelist | `/pistoncrusher whitelist remove STONE` |
| `/pistoncrusher whitelist list` | Displays all currently whitelisted block types that can be crushed | `/pistoncrusher whitelist list` |
| `/pistoncrusher multiplier <value>` | Sets the output multiplier for crushed blocks (minimum value: 1.0) | `/pistoncrusher multiplier 2.5` |
| `/pistoncrusher multiplier` | Shows the current output multiplier setting | `/pistoncrusher multiplier` |
| `/pistoncrusher crusherblock <material>` | Sets which block type acts as the crusher mechanism | `/pistoncrusher crusherblock POLISHED_ANDESITE` |
| `/pistoncrusher crusherblock` | Displays the current crusher block type and usage instructions | `/pistoncrusher crusherblock` |

**Important Notes:**
- Material names must be valid Minecraft material identifiers (e.g., COBBLESTONE, STONE, IRON_ORE)
- The multiplier determines how many items are produced when a block is crushed
- The crusher block is the block that blocks must be pushed into for crushing to occur
- Tab completion is available for all material names

# **Rail Boost**

A comprehensive minecart enhancement plugin that transforms basic minecarts into powerful transportation and utility vehicles with speed control, storage, and automation features.

**Core Features:**
- **Variable Speed Control:** 6 different speed levels from 0.25x to 4.0x normal speed
- **Auto-Pickup System:** Configurable radius for automatic item collection while traveling
- **Built-in Storage:** Each minecart can carry items with inventory management
- **Speedometer Display:** Real-time speed indicator using boss bar display
- **Chunk Loading:** Keep chunks loaded while traveling for uninterrupted journeys
- **Magnetic Collection:** Advanced item attraction system with customizable range
- **Particle Effects:** Visual effects and customizable particle trails
- **Auto-Sit Feature:** Automatic boarding system for convenient transportation
- **Item Blacklist:** Configure which items should not be automatically collected

**Player Commands:**

| Command | Description | Usage Example |
|---------|-------------|---------------|
| `/railboost speed <1-6>` | Sets minecart speed level from 1 (0.25x) to 6 (4.0x normal speed) | `/railboost speed 4` |
| `/railboost autopickup <true/false>` | Toggles automatic item pickup while traveling in the minecart | `/railboost autopickup true` |
| `/railboost autopickup radius <1-5>` | Sets the radius in blocks for automatic item pickup (1-5 blocks) | `/railboost autopickup radius 3` |
| `/railboost storage` | Opens the built-in storage inventory for the current minecart | `/railboost storage` |
| `/railboost speedometer <true/false>` | Toggles the real-time speed display using a boss bar | `/railboost speedometer true` |
| `/railboost chunkload <true/false>` | Toggles chunk loading to prevent chunks from unloading during travel | `/railboost chunkload true` |
| `/railboost magnet <true/false>` | Toggles magnetic attraction between nearby RailBoost-enabled minecarts | `/railboost magnet true` |
| `/railboost effect <true/false>` | Toggles particle effects and visual trails behind the minecart | `/railboost effect true` |
| `/railboost effect type <particle>` | Sets the particle effect type (FLAME, SMOKE, HEART, CLOUD, etc.) | `/railboost effect type FLAME` |
| `/railboost autosit <true/false>` | Toggles automatic boarding when right-clicking the minecart | `/railboost autosit true` |
| `/railboost blacklist add <item>` | Adds an item type to the pickup blacklist for this minecart | `/railboost blacklist add DIRT` |
| `/railboost blacklist remove <item>` | Removes an item type from the pickup blacklist | `/railboost blacklist remove COBBLESTONE` |
| `/railboost blacklist list` | Displays all items currently blacklisted from automatic pickup | `/railboost blacklist list` |
| `/railboost configuration <true/false>` | Activates or deactivates the minecart for RailBoost features | `/railboost configuration true` |
| `/railboost info` | Displays complete current configuration and settings for the minecart | `/railboost info` |
| `/railboost help` | Shows all available commands with brief descriptions | `/railboost help` |

**Command Aliases:**
- `/railboost` can be shortened to `/rb`
- Boolean values accept: `true/false`, `on/off`, `1/0`
- The `configuration` command is required to activate minecarts before other features work

**Prerequisites for Commands:**
- Most commands require the player to be sitting in a minecart
- The minecart must be activated using `/railboost configuration true`
- Some commands like `blacklist list` can be used without being in a minecart

## **License**

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

## **Screenshots**

The following screenshots demonstrate the core functionality of each plugin, including the achievement system interface, automated resource processing setups, and enhanced minecart features in typical server environments.

| Screenshot 1 | Screenshot 2 | Screenshot 3 |
|:------------:|:------------:|:------------:|
| <img src="https://raw.githubusercontent.com/BerndHagen/Minecraft-Server-Plugins/main/img/screenshot1.png" alt="Advanced Achievements GUI Interface" width="100%"> | <img src="https://raw.githubusercontent.com/BerndHagen/Minecraft-Server-Plugins/main/img/screenshot2.png" alt="Achievement Progress Tracking" width="100%"> | <img src="https://raw.githubusercontent.com/BerndHagen/Minecraft-Server-Plugins/main/img/screenshot3.png" alt="Piston Crusher Automation Setup" width="100%"> |
| **Screenshot 4** | **Screenshot 5** | **Screenshot 6** |
| <img src="https://raw.githubusercontent.com/BerndHagen/Minecraft-Server-Plugins/main/img/screenshot4.png" alt="Resource Processing System" width="100%"> | <img src="https://raw.githubusercontent.com/BerndHagen/Minecraft-Server-Plugins/main/img/screenshot5.png" alt="Rail Boost Enhanced Minecart" width="100%"> | <img src="https://raw.githubusercontent.com/BerndHagen/Minecraft-Server-Plugins/main/img/screenshot6.png" alt="Speed Control and Effects" width="100%"> |
