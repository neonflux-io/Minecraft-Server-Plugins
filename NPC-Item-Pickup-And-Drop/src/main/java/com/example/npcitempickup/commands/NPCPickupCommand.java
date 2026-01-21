package com.example.npcitempickup.commands;

import com.example.npcitempickup.NPCItemPickupPlugin;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Command handler for NPCItemPickup plugin
 */
public class NPCPickupCommand implements CommandExecutor, TabCompleter {

    private final NPCItemPickupPlugin plugin;

    public NPCPickupCommand(NPCItemPickupPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("npcpickup.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;
            case "toggle":
                handleToggle(sender);
                break;
            case "status":
                handleStatus(sender);
                break;
            case "info":
                handleInfo(sender, args);
                break;
            case "clear":
                handleClear(sender, args);
                break;
            case "disable":
                handleDisablePickup(sender, args);
                break;
            case "enable":
                handleEnablePickup(sender, args);
                break;
            case "thief":
                handleThiefMode(sender, args);
                break;
            case "unthief":
                handleUnthiefMode(sender, args);
                break;
            default:
                sendHelpMessage(sender);
                break;
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        plugin.reload();
        sender.sendMessage(ChatColor.GREEN + "NPCItemPickup configuration reloaded!");
    }

    private void handleToggle(CommandSender sender) {
        boolean newState = !plugin.isPluginEnabled();
        plugin.setPluginEnabled(newState);

        String status = newState ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled";
        sender.sendMessage(ChatColor.YELLOW + "NPCItemPickup is now " + status + ChatColor.YELLOW + ".");
    }

    private void handleStatus(CommandSender sender) {
        String status = plugin.isPluginEnabled() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled";

        sender.sendMessage(ChatColor.YELLOW + "NPCItemPickup Status:");
        sender.sendMessage(ChatColor.GRAY + "- Plugin: " + status);
        sender.sendMessage(ChatColor.GRAY + "- Pickup Radius: " +
                plugin.getConfig().getDouble("pickup.radius", 5.0) + " blocks");
        sender.sendMessage(ChatColor.GRAY + "- Max Items per NPC: " +
                plugin.getConfig().getInt("pickup.max_items", 64));
        sender.sendMessage(ChatColor.GRAY + "- Drop on Death: " +
                (plugin.getConfig().getBoolean("drop.enabled", true) ? ChatColor.GREEN + "enabled"
                        : ChatColor.RED + "disabled"));

        // Count NPCs with items
        Map<UUID, List<ItemStack>> inventories = plugin.getInventoryManager().getAllInventories();
        int npcsWithItems = 0;
        int totalItems = 0;

        for (List<ItemStack> inventory : inventories.values()) {
            if (!inventory.isEmpty()) {
                npcsWithItems++;
                totalItems += inventory.stream().mapToInt(ItemStack::getAmount).sum();
            }
        }

        sender.sendMessage(ChatColor.GRAY + "- NPCs with Items: " + npcsWithItems);
        sender.sendMessage(ChatColor.GRAY + "- Total Items Held: " + totalItems);
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return;
        }

        Player player = (Player) sender;
        NPC targetNPC = null;

        if (args.length > 1) {
            // Try to find NPC by name
            String npcName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            for (NPC npc : CitizensAPI.getNPCRegistry()) {
                if (npc.getName().equalsIgnoreCase(npcName)) {
                    targetNPC = npc;
                    break;
                }
            }
        } else {
            // Find nearby NPC
            for (NPC npc : CitizensAPI.getNPCRegistry()) {
                if (npc.isSpawned() && npc.getEntity() != null) {
                    if (npc.getEntity().getLocation().distance(player.getLocation()) <= 5.0) {
                        targetNPC = npc;
                        break;
                    }
                }
            }
        }

        if (targetNPC == null) {
            sender.sendMessage(ChatColor.RED + "No NPC found. Stand close to an NPC or specify a name.");
            return;
        }

        // Display NPC info
        List<ItemStack> items = plugin.getInventoryManager().getItems(targetNPC);
        int itemCount = plugin.getInventoryManager().getItemCount(targetNPC);

        sender.sendMessage(ChatColor.YELLOW + "NPC Info: " + ChatColor.WHITE + targetNPC.getName());
        sender.sendMessage(ChatColor.GRAY + "- Items Held: " + itemCount + "/" +
                plugin.getConfig().getInt("pickup.max_items", 64));

        if (!items.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "- Inventory:");
            for (ItemStack item : items) {
                sender.sendMessage(ChatColor.GRAY + "  • " + item.getAmount() + "x " +
                        item.getType().name().toLowerCase().replace("_", " "));
            }
        } else {
            sender.sendMessage(ChatColor.GRAY + "- Inventory: Empty");
        }
    }

    private void handleClear(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /npcpickup clear <npc_name|all>");
            return;
        }

        String target = args[1].toLowerCase();

        if (target.equals("all")) {
            // Clear all NPC inventories
            Map<UUID, List<ItemStack>> inventories = plugin.getInventoryManager().getAllInventories();
            int clearedCount = 0;

            for (UUID npcId : new ArrayList<>(inventories.keySet())) {
                NPC npc = CitizensAPI.getNPCRegistry().getByUniqueId(npcId);
                if (npc != null) {
                    plugin.getInventoryManager().clearInventory(npc);
                    clearedCount++;
                }
            }

            sender.sendMessage(ChatColor.GREEN + "Cleared inventories of " + clearedCount + " NPCs.");
        } else {
            // Clear specific NPC
            NPC targetNPC = null;
            String npcName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

            for (NPC npc : CitizensAPI.getNPCRegistry()) {
                if (npc.getName().equalsIgnoreCase(npcName)) {
                    targetNPC = npc;
                    break;
                }
            }

            if (targetNPC == null) {
                sender.sendMessage(ChatColor.RED + "NPC not found: " + npcName);
                return;
            }

            List<ItemStack> clearedItems = plugin.getInventoryManager().clearInventory(targetNPC);
            int totalCleared = clearedItems.stream().mapToInt(ItemStack::getAmount).sum();

            sender.sendMessage(ChatColor.GREEN + "Cleared " + totalCleared +
                    " items from " + targetNPC.getName() + "'s inventory.");
        }
    }

    private void handleDisablePickup(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /npcpickup disable <npc_id|npc_name>");
            return;
        }

        NPC targetNPC = findNPC(joinArgs(args, 1));
        if (targetNPC == null) {
            sender.sendMessage(ChatColor.RED + "NPC '" + joinArgs(args, 1) + "' not found.");
            return;
        }

        plugin.getInventoryManager().disablePickup(targetNPC);
        sender.sendMessage(ChatColor.GREEN + "Pickup disabled for NPC " + targetNPC.getName() + " (ID: "
                + targetNPC.getId() + ")");
    }

    private void handleEnablePickup(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /npcpickup enable <npc_id|npc_name>");
            return;
        }

        NPC targetNPC = findNPC(joinArgs(args, 1));
        if (targetNPC == null) {
            sender.sendMessage(ChatColor.RED + "NPC '" + joinArgs(args, 1) + "' not found.");
            return;
        }

        plugin.getInventoryManager().enablePickup(targetNPC);
        sender.sendMessage(
                ChatColor.GREEN + "Pickup enabled for NPC " + targetNPC.getName() + " (ID: " + targetNPC.getId() + ")");
    }

    private void handleThiefMode(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /npcpickup thief <npc_id|npc_name>");
            return;
        }

        NPC targetNPC = findNPC(joinArgs(args, 1));
        if (targetNPC == null) {
            sender.sendMessage(ChatColor.RED + "NPC '" + joinArgs(args, 1) + "' not found.");
            return;
        }

        plugin.getInventoryManager().enableThiefMode(targetNPC);
        sender.sendMessage(ChatColor.GREEN + "Thief mode enabled for NPC " + targetNPC.getName() + " (ID: "
                + targetNPC.getId() + ")");
        sender.sendMessage(ChatColor.YELLOW + "This NPC will now steal items from nearby chests!");
    }

    private void handleUnthiefMode(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /npcpickup unthief <npc_id|npc_name>");
            return;
        }

        NPC targetNPC = findNPC(joinArgs(args, 1));
        if (targetNPC == null) {
            sender.sendMessage(ChatColor.RED + "NPC '" + joinArgs(args, 1) + "' not found.");
            return;
        }

        plugin.getInventoryManager().disableThiefMode(targetNPC);
        sender.sendMessage(ChatColor.GREEN + "Thief mode disabled for NPC " + targetNPC.getName() + " (ID: "
                + targetNPC.getId() + ")");
    }

    private NPC findNPC(String identifier) {
        try {
            // Try to find by ID first
            int id = Integer.parseInt(identifier);
            return CitizensAPI.getNPCRegistry().getById(id);
        } catch (NumberFormatException e) {
            // Not a number, try to find by name
            for (NPC npc : CitizensAPI.getNPCRegistry()) {
                if (npc.getName().equalsIgnoreCase(identifier)) {
                    return npc;
                }
            }
        }
        return null;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "NPCItemPickup Commands:");
        sender.sendMessage(ChatColor.GRAY + "/npcpickup reload - Reload the plugin configuration");
        sender.sendMessage(ChatColor.GRAY + "/npcpickup toggle - Enable/disable the plugin");
        sender.sendMessage(ChatColor.GRAY + "/npcpickup status - Show plugin status and statistics");
        sender.sendMessage(ChatColor.GRAY + "/npcpickup info [npc_name] - Show NPC inventory info");
        sender.sendMessage(ChatColor.GRAY + "/npcpickup clear <npc_name|all> - Clear NPC inventories");
        sender.sendMessage(ChatColor.GRAY + "/npcpickup disable <npc_id|name> - Disable pickup for specific NPC");
        sender.sendMessage(ChatColor.GRAY + "/npcpickup enable <npc_id|name> - Enable pickup for specific NPC");
        sender.sendMessage(ChatColor.GRAY + "/npcpickup thief <npc_id|name> - Enable thief mode for NPC");
        sender.sendMessage(ChatColor.GRAY + "/npcpickup unthief <npc_id|name> - Disable thief mode for NPC");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("reload", "toggle", "status", "info", "clear", "disable", "enable",
                    "thief", "unthief");
            String partial = args[0].toLowerCase();

            for (String subCommand : subCommands) {
                if (subCommand.startsWith(partial)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("clear") ||
                args[0].equalsIgnoreCase("disable") || args[0].equalsIgnoreCase("enable") ||
                args[0].equalsIgnoreCase("thief") || args[0].equalsIgnoreCase("unthief"))) {
            String partial = args[1].toLowerCase();

            if (args[0].equalsIgnoreCase("clear")) {
                if ("all".startsWith(partial)) {
                    completions.add("all");
                }
            }

            // Add NPC names
            for (NPC npc : CitizensAPI.getNPCRegistry()) {
                String name = npc.getName().toLowerCase();
                if (name.startsWith(partial)) {
                    completions.add(npc.getName());
                }
            }
        }

        return completions;
    }

    private String joinArgs(String[] args, int startIndex) {
        if (startIndex >= args.length) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) {
                sb.append(" ");
            }
            sb.append(args[i]);
        }
        return sb.toString();
    }
}
