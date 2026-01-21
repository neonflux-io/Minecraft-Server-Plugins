package arearewind.managers.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public interface IGUIPage {

    void openGUI(Player player);

    default void openGUI(Player player, int page) {
        openGUI(player);
    }

    void handleClick(Player player, InventoryClickEvent event);

    String getPageType();

    default void handlePaginationAction(Player player, GUIPaginationHelper.PaginationAction action) { }
}