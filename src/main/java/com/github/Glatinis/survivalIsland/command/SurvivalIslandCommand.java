package com.github.Glatinis.survivalIsland.command;

import com.github.Glatinis.survivalIsland.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Root executor for {@code /survivalisland} (alias {@code /si}). Dispatches to whichever
 * registered {@link SubCommand} matches the first argument, so every feature area gets its own
 * small class instead of one giant switch statement.
 */
public final class SurvivalIslandCommand implements CommandExecutor, TabCompleter {

    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    public void register(SubCommand subCommand) {
        subCommands.put(subCommand.name().toLowerCase(), subCommand);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            Messages.error(sender, "Usage: /survivalisland <" + String.join("|", subCommands.keySet()) + "> ...");
            return true;
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand == null) {
            Messages.error(sender, "Unknown category '" + args[0] + "'. Options: " + String.join(", ", subCommands.keySet()));
            return true;
        }

        if (!sender.hasPermission(subCommand.permission())) {
            Messages.error(sender, "You don't have permission to use this.");
            return true;
        }

        subCommand.execute(sender, Arrays.copyOfRange(args, 1, args.length));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) {
            return List.of();
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return subCommands.keySet().stream()
                .filter(name -> sender.hasPermission(subCommands.get(name).permission()))
                .filter(name -> name.startsWith(prefix))
                .collect(Collectors.toCollection(ArrayList::new));
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand == null || !sender.hasPermission(subCommand.permission())) {
            return List.of();
        }
        return subCommand.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
    }
}
