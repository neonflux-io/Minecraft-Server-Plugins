package arearewind.commands.registry;

import arearewind.commands.base.Command;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class CommandRegistry {

    private final Map<String, Command> commands = new HashMap<>();
    private final Map<String, String> aliases = new HashMap<>();

    public void registerCommand(Command command) {
        commands.put(command.getName().toLowerCase(), command);

        for (String alias : command.getAliases()) {
            aliases.put(alias.toLowerCase(), command.getName().toLowerCase());
        }
    }

    public Command getCommand(String name) {
        String commandName = name.toLowerCase();

        if (aliases.containsKey(commandName)) {
            commandName = aliases.get(commandName);
        }

        return commands.get(commandName);
    }

    public Collection<Command> getAllCommands() {
        return commands.values();
    }

    public List<String> getCommandCompletions(String partial) {
        String lowerPartial = partial.toLowerCase();

        List<String> completions = new ArrayList<>();

        completions.addAll(commands.keySet().stream()
                .filter(name -> name.startsWith(lowerPartial))
                .collect(Collectors.toList()));

        completions.addAll(aliases.keySet().stream()
                .filter(alias -> alias.startsWith(lowerPartial))
                .collect(Collectors.toList()));

        return completions;
    }

    public boolean executeCommand(Player player, String commandName, String[] args) {
        Command command = getCommand(commandName);
        if (command == null) {
            return false;
        }

        String requiredPermission = command.getRequiredPermission();
        if (requiredPermission != null && !player.hasPermission(requiredPermission)) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        return command.execute(player, args);
    }

    public List<String> getTabCompletions(Player player, String commandName, String[] args) {
        Command command = getCommand(commandName);
        if (command == null) {
            return new ArrayList<>();
        }

        return command.getTabCompletions(player, args);
    }
}
