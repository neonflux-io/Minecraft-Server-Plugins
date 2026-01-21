package com.example.advancedachievements.gui;

import com.example.advancedachievements.AdvancedAchievements;
import com.example.advancedachievements.models.Achievement;
import com.example.advancedachievements.models.PlayerProgress;
import com.example.advancedachievements.enums.AchievementState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class AchievementGUI implements Listener {
    
    private final AdvancedAchievements plugin;
    private final Map<UUID, GuiSession> sessions;
    
    public AchievementGUI(AdvancedAchievements plugin) {
        this.plugin = plugin;
        this.sessions = new HashMap<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    public void openGUI(Player player) {
        openGUI(player, 0);
    }

    public void openGUI(Player player, int page) {
        List<Achievement> achievements = getAchievementsForPlayer(player);
        int totalPages = (int) Math.ceil((double) achievements.size() / plugin.getConfigManager().getItemsPerPage());
        if (totalPages == 0) totalPages = 1;
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;
        String title = plugin.getMessageManager().getMessage("gui-title");
        title += " (" + (page + 1) + "/" + totalPages + ")";
        Inventory inventory = Bukkit.createInventory(null, plugin.getConfigManager().getGUIRows() * 9, title);
        int startIndex = page * plugin.getConfigManager().getItemsPerPage();
        int endIndex = Math.min(startIndex + plugin.getConfigManager().getItemsPerPage(), achievements.size());
        for (int i = startIndex; i < endIndex; i++) {
            Achievement achievement = achievements.get(i);
            ItemStack item = createAchievementItem(player, achievement);
            inventory.setItem(i - startIndex, item);
        }
        addNavigationItems(inventory, page, totalPages);
        player.openInventory(inventory);
        sessions.put(player.getUniqueId(), new GuiSession(page));
    }
    
    private List<Achievement> getAchievementsForPlayer(Player player) {
        List<Achievement> achievements = new ArrayList<>();
        
        for (Achievement achievement : plugin.getAchievementManager().getAllAchievements()) {
            if (achievement.isHidden()) {
                PlayerProgress progress = plugin.getProgressManager().getPlayerProgress(player, achievement.getId());
                if (!progress.isUnlocked()) {
                    continue;
                }
            }
            
            achievements.add(achievement);
        }
        
        achievements.sort((a, b) -> {
            PlayerProgress progressA = plugin.getProgressManager().getPlayerProgress(player, a.getId());
            PlayerProgress progressB = plugin.getProgressManager().getPlayerProgress(player, b.getId());
            
            if (progressA.isClaimed() != progressB.isClaimed()) {
                return progressA.isClaimed() ? 1 : -1;
            }
            if (progressA.isUnlocked() != progressB.isUnlocked()) {
                return progressA.isUnlocked() ? -1 : 1;
            }
            double percentA = (double) progressA.getProgress() / a.getRequiredAmount();
            double percentB = (double) progressB.getProgress() / b.getRequiredAmount();
            return Double.compare(percentB, percentA);
        });
        
        return achievements;
    }
    
    private ItemStack createAchievementItem(Player player, Achievement achievement) {
        PlayerProgress progress = plugin.getProgressManager().getPlayerProgress(player, achievement.getId());
        AchievementState state = getAchievementState(progress);
        
        Material material = achievement.getIcon();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String displayName = "§f" + achievement.getTitle();
            switch (state) {
                case LOCKED:
                    displayName = "§7" + achievement.getTitle();
                    break;
                case UNLOCKED:
                    displayName = "§a" + achievement.getTitle();
                    break;
                case CLAIMED:
                    displayName = "§b" + achievement.getTitle();
                    break;
            }
            meta.setDisplayName(displayName);
            
            List<String> lore = new ArrayList<>();
            lore.add("§7" + achievement.getDescription());
            lore.add("");
            
            if (state == AchievementState.LOCKED) {
                String progressText = plugin.getMessageManager().getMessage("gui-progress",
                    "current", String.valueOf(progress.getProgress()),
                    "required", String.valueOf(achievement.getRequiredAmount()));
                lore.add(progressText);
                lore.add(createProgressBar(progress.getProgress(), achievement.getRequiredAmount()));
            } else {
                lore.add(plugin.getMessageManager().getMessage(state == AchievementState.CLAIMED ? "gui-claimed" : "gui-unlocked"));
            }
            
            lore.add("");

            if (!achievement.getRewards().isEmpty()) {
                lore.add("");
                lore.add("§6Rewards:");
                for (String reward : achievement.getRewards()) {
                    lore.add("§7- " + formatReward(reward));
                }
            }
            
            if (state == AchievementState.UNLOCKED) {
                lore.add("");
                lore.add("§eClick to claim reward!");
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private AchievementState getAchievementState(PlayerProgress progress) {
        if (progress.isClaimed()) {
            return AchievementState.CLAIMED;
        } else if (progress.isUnlocked()) {
            return AchievementState.UNLOCKED;
        } else {
            return AchievementState.LOCKED;
        }
    }
    
    private String createProgressBar(int current, int required) {
        int barLength = 20;
        int filledBars = (int) ((double) current / required * barLength);
        
        StringBuilder bar = new StringBuilder("§7[");
        for (int i = 0; i < barLength; i++) {
            if (i < filledBars) {
                bar.append("§a■");
            } else {
                bar.append("§8■");
            }
        }
        bar.append("§7]");
        
        return bar.toString();
    }
      private String formatReward(String reward) {
        String[] parts = reward.split(":", 3);
        if (parts.length < 2) return reward;
        
        String type = parts[0].toUpperCase();
        switch (type) {
            case "ITEM": {
                String itemMaterialName = parts[1].replace("_", " ");
                String[] itemWords = itemMaterialName.split(" ");
                StringBuilder itemFormattedName = new StringBuilder();
                for (String word : itemWords) {
                    if (word.length() > 0) {
                        if (itemFormattedName.length() > 0) itemFormattedName.append(" ");
                        itemFormattedName.append(Character.toUpperCase(word.charAt(0)))
                            .append(word.substring(1).toLowerCase());
                    }
                }
                return parts.length > 2 ? parts[2] + "x " + itemFormattedName.toString() : "1x " + itemFormattedName.toString();
            }
            case "XP":
            case "EXPERIENCE":
                return parts[1] + " XP";
            case "MONEY":
                return "$" + parts[1];
            case "COMMAND":
                return "Special Reward";
            case "TITLE":
                return "Title: " + parts[1];
            default: {
                if (parts.length == 2 && type.matches("[A-Z_]+") && parts[1].matches("\\d+")) {
                    String matMaterialName = type.replace("_", " ");
                    String[] matWords = matMaterialName.split(" ");
                    StringBuilder matFormattedName = new StringBuilder();
                    for (String word : matWords) {
                        if (word.length() > 0) {
                            if (matFormattedName.length() > 0) matFormattedName.append(" ");
                            matFormattedName.append(Character.toUpperCase(word.charAt(0)))
                                .append(word.substring(1).toLowerCase());
                        }
                    }
                    return parts[1] + "x " + matFormattedName.toString();
                }
                return reward;
            }
        }
    }
    
    private void addNavigationItems(Inventory inventory, int currentPage, int totalPages) {
        int size = inventory.getSize();
        
        if (currentPage > 0) {
            ItemStack prevItem = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevItem.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName("§aPrevious Page");
                prevMeta.setLore(Arrays.asList("§7Click to go to page " + currentPage));
                prevItem.setItemMeta(prevMeta);
            }
            inventory.setItem(size - 9, prevItem);
        }
        
        if (currentPage < totalPages - 1) {
            ItemStack nextItem = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextItem.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName("§aNext Page");
                nextMeta.setLore(Arrays.asList("§7Click to go to page " + (currentPage + 2)));
                nextItem.setItemMeta(nextMeta);
            }
            inventory.setItem(size - 1, nextItem);
        }
        
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName("§cClose");
            closeMeta.setLore(Arrays.asList("§7Click to close this menu"));
            closeItem.setItemMeta(closeMeta);
        }
        inventory.setItem(size - 5, closeItem);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        
        if (!sessions.containsKey(playerId)) return;
        
        event.setCancelled(true);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        GuiSession session = sessions.get(playerId);
        String title = event.getView().getTitle();
        
        if (!title.startsWith(plugin.getMessageManager().getMessage("gui-title"))) return;
        
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) return;
        
        String displayName = meta.getDisplayName();
        
        if (displayName.contains("Previous Page")) {
            openGUI(player, session.getPage() - 1);
            return;
        }
        
        if (displayName.contains("Next Page")) {
            openGUI(player, session.getPage() + 1);
            return;
        }
        
        if (displayName.contains("Close")) {
            player.closeInventory();
            return;
        }
        
        String achievementTitle = displayName.replaceAll("§[0-9a-fk-or]", "");
        Achievement achievement = plugin.getAchievementManager().getAllAchievements().stream()
            .filter(a -> a.getTitle().equals(achievementTitle))
            .findFirst().orElse(null);
        
        if (achievement != null) {
            PlayerProgress progress = plugin.getProgressManager().getPlayerProgress(player, achievement.getId());
            if (progress.isUnlocked() && !progress.isClaimed()) {
                if (plugin.getRewardManager().claimReward(player, achievement.getId())) {
                    openGUI(player, session.getPage());
                }
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            sessions.remove(event.getPlayer().getUniqueId());
        }
    }
    
    private static class GuiSession {
        private final int page;
        
        public GuiSession(int page) {
            this.page = page;
        }
        
        public int getPage() {
            return page;
        }
    }
}