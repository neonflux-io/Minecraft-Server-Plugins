package arearewind.managers.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class GUIPaginationHelper {

    private static final Map<UUID, PaginationData> paginationData = new ConcurrentHashMap<>();
    public static final int FIRST_PAGE = 0;

    public static class PaginationData {
        private int currentPage;
        private int maxPage;
        private String guiType;
        private String areaName;

        public PaginationData(int currentPage, int maxPage, String guiType, String areaName) {
            this.currentPage = currentPage;
            this.maxPage = maxPage;
            this.guiType = guiType;
            this.areaName = areaName;
        }

        public int getCurrentPage() {
            return currentPage;
        }

        public int getMaxPage() {
            return maxPage;
        }

        public String getGuiType() {
            return guiType;
        }

        public String getAreaName() {
            return areaName;
        }

        public void setCurrentPage(int currentPage) {
            this.currentPage = currentPage;
        }

        public void setMaxPage(int maxPage) {
            this.maxPage = maxPage;
        }

        public void setAreaName(String areaName) {
            this.areaName = areaName;
        }
    }

    public static PaginationData getPaginationData(UUID playerId, String guiType, String areaName) {
        PaginationData existingData = paginationData.get(playerId);

        if (existingData != null && existingData.getGuiType().equals(guiType)) {
            if (areaName != null && !areaName.equals(existingData.getAreaName())) {
                existingData.setAreaName(areaName);
            }
            return existingData;
        }

        PaginationData newData = new PaginationData(0, 0, guiType, areaName);
        paginationData.put(playerId, newData);
        return newData;
    }

    public static void updatePaginationData(UUID playerId, int currentPage, int maxPage, String guiType,
            String areaName) {
        PaginationData data = getPaginationData(playerId, guiType, areaName);
        data.setCurrentPage(currentPage);
        data.setMaxPage(maxPage);
    }

    public static void clearPaginationData(UUID playerId) {
        paginationData.remove(playerId);
    }

    public static PaginationInfo calculatePagination(int totalItems, int itemsPerPage, int currentPage) {
        int maxPage = Math.max(0, (totalItems - 1) / itemsPerPage);
        currentPage = Math.max(0, Math.min(currentPage, maxPage));

        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalItems);

        return new PaginationInfo(currentPage, maxPage, startIndex, endIndex, totalItems);
    }

    public static class PaginationInfo {
        private final int currentPage;
        private final int maxPage;
        private final int startIndex;
        private final int endIndex;
        private final int totalItems;

        public PaginationInfo(int currentPage, int maxPage, int startIndex, int endIndex, int totalItems) {
            this.currentPage = currentPage;
            this.maxPage = maxPage;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.totalItems = totalItems;
        }

        public int getCurrentPage() {
            return currentPage;
        }

        public int getMaxPage() {
            return maxPage;
        }

        public int getStartIndex() {
            return startIndex;
        }

        public int getEndIndex() {
            return endIndex;
        }

        public int getTotalItems() {
            return totalItems;
        }

        public boolean hasNextPage() {
            return currentPage < maxPage;
        }

        public boolean hasPreviousPage() {
            return currentPage > 0;
        }

        public int getItemsOnCurrentPage() {
            return endIndex - startIndex;
        }
    }

    public static void addPaginationButtons(Inventory gui, PaginationInfo paginationInfo, int previousSlot,
            int nextSlot, int infoSlot) {
        if (paginationInfo.hasPreviousPage()) {
            ItemStack prevItem = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevItem.getItemMeta();
            prevMeta.setDisplayName(ChatColor.YELLOW + "Previous Page");

            List<String> prevLore = new ArrayList<>();
            prevLore.add(ChatColor.GRAY + "Page " + (paginationInfo.getCurrentPage()) + " of "
                    + (paginationInfo.getMaxPage() + 1));
            prevLore.add(ChatColor.GREEN + "Click to go to previous page");
            prevMeta.setLore(prevLore);

            prevItem.setItemMeta(prevMeta);
            gui.setItem(previousSlot, prevItem);
        } else {
            ItemStack placeholder = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta placeholderMeta = placeholder.getItemMeta();
            placeholderMeta.setDisplayName(ChatColor.GRAY + "No Previous Page");
            placeholder.setItemMeta(placeholderMeta);
            gui.setItem(previousSlot, placeholder);
        }

        if (paginationInfo.hasNextPage()) {
            ItemStack nextItem = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextItem.getItemMeta();
            nextMeta.setDisplayName(ChatColor.YELLOW + "Next Page");

            List<String> nextLore = new ArrayList<>();
            nextLore.add(ChatColor.GRAY + "Page " + (paginationInfo.getCurrentPage() + 2) + " of "
                    + (paginationInfo.getMaxPage() + 1));
            nextLore.add(ChatColor.GREEN + "Click to go to next page");
            nextMeta.setLore(nextLore);

            nextItem.setItemMeta(nextMeta);
            gui.setItem(nextSlot, nextItem);
        } else {
            ItemStack placeholder = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta placeholderMeta = placeholder.getItemMeta();
            placeholderMeta.setDisplayName(ChatColor.GRAY + "No Next Page");
            placeholder.setItemMeta(placeholderMeta);
            gui.setItem(nextSlot, placeholder);
        }

        if (infoSlot >= 0) {
            ItemStack infoItem = new ItemStack(Material.BOOK);
            ItemMeta infoMeta = infoItem.getItemMeta();
            infoMeta.setDisplayName(ChatColor.AQUA + "Page Information");

            List<String> infoLore = new ArrayList<>();
            infoLore.add(ChatColor.GRAY + "Current Page: " + (paginationInfo.getCurrentPage() + 1));
            infoLore.add(ChatColor.GRAY + "Total Pages: " + (paginationInfo.getMaxPage() + 1));
            infoLore.add(ChatColor.GRAY + "Items on Page: " + paginationInfo.getItemsOnCurrentPage());
            infoLore.add(ChatColor.GRAY + "Total Items: " + paginationInfo.getTotalItems());
            infoMeta.setLore(infoLore);

            infoItem.setItemMeta(infoMeta);
            gui.setItem(infoSlot, infoItem);
        }
    }

    public static PaginationAction checkPaginationClick(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return PaginationAction.NONE;
        }

        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        if (displayName.equals("Previous Page")) {
            return PaginationAction.PREVIOUS;
        } else if (displayName.equals("Next Page")) {
            return PaginationAction.NEXT;
        }

        return PaginationAction.NONE;
    }

    public enum PaginationAction {
        NONE, PREVIOUS, NEXT
    }
}
