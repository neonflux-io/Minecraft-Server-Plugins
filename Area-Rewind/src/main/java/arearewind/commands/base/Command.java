package arearewind.commands.base;

import org.bukkit.entity.Player;

import java.util.List;

public interface Command {

    boolean execute(Player player, String[] args);

    List<String> getTabCompletions(Player player, String[] args);

    String getName();

    List<String> getAliases();

    String getDescription();

    String getUsage();

    String getRequiredPermission();
}