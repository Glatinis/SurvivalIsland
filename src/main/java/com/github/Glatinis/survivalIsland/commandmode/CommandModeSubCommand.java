package com.github.Glatinis.survivalIsland.commandmode;

import com.github.Glatinis.survivalIsland.command.SubCommand;
import com.github.Glatinis.survivalIsland.config.ConfigManager;
import com.github.Glatinis.survivalIsland.util.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@code /survivalisland commandmode <start|stop|reload>}.
 */
public final class CommandModeSubCommand implements SubCommand {

    private final CommandModeManager commandModeManager;
    private final ConfigManager configManager;

    public CommandModeSubCommand(CommandModeManager commandModeManager, ConfigManager configManager) {
        this.commandModeManager = commandModeManager;
        this.configManager = configManager;
    }

    @Override
    public String name() {
        return "commandmode";
    }

    @Override
    public String permission() {
        return Messages.COMMAND_MODE_PERMISSION;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            Messages.error(sender, "Usage: /survivalisland commandmode <start|stop|reload>");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> handleToggle(sender, true);
            case "stop" -> handleToggle(sender, false);
            case "reload" -> {
                configManager.reload();
                Messages.success(sender, "Reloaded command-mode triggers from config.yml.");
            }
            default -> Messages.error(sender, "Usage: /survivalisland commandmode <start|stop|reload>");
        }
    }

    private void handleToggle(CommandSender sender, boolean enable) {
        if (!(sender instanceof Player player)) {
            Messages.error(sender, "Only a player can toggle command mode.");
            return;
        }
        if (enable) {
            commandModeManager.enable(player.getUniqueId());
            Messages.success(sender, "Command mode ON - your chat messages will be treated as triggers.");
        } else {
            commandModeManager.disable(player.getUniqueId());
            Messages.success(sender, "Command mode OFF - chat is back to normal.");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String lower = args[0].toLowerCase();
            return Stream.of("start", "stop", "reload")
                .filter(s -> s.startsWith(lower))
                .collect(Collectors.toCollection(ArrayList::new));
        }
        return List.of();
    }
}
