package com.github.Glatinis.survivalIsland.contestant;

import com.github.Glatinis.survivalIsland.command.SubCommand;
import com.github.Glatinis.survivalIsland.config.ConfigManager;
import com.github.Glatinis.survivalIsland.lives.LivesScoreboardService;
import com.github.Glatinis.survivalIsland.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code /survivalisland contestant add|remove|list <player> [island]} - binds a player to one
 * of the fixed islands so every other feature can resolve "whose island is this" without relying
 * on WorldGuard region ownership. Also keeps the lives board in sync, since only contestants get
 * a row on it.
 */
public final class ContestantSubCommand implements SubCommand {

    private final ContestantManager contestantManager;
    private final ConfigManager configManager;
    private final LivesScoreboardService livesScoreboardService;

    public ContestantSubCommand(ContestantManager contestantManager, ConfigManager configManager,
                                 LivesScoreboardService livesScoreboardService) {
        this.contestantManager = contestantManager;
        this.configManager = configManager;
        this.livesScoreboardService = livesScoreboardService;
    }

    @Override
    public String name() {
        return "contestant";
    }

    @Override
    public String permission() {
        return Messages.ADMIN_PERMISSION;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            Messages.error(sender, "Usage: /survivalisland contestant <add|remove|list> ...");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            default -> Messages.error(sender, "Usage: /survivalisland contestant <add|remove|list> ...");
        }
    }

    @SuppressWarnings("deprecation")
    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Messages.error(sender, "Usage: /survivalisland contestant add <player> <island>");
            return;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);
        String island = args[2];
        ContestantManager.AssignResult result = contestantManager.assign(player, island, configManager.islands());
        switch (result) {
            case OK -> {
                Messages.success(sender, "Assigned " + args[1] + " to " + island + ".");
                Player online = player.getPlayer();
                if (online != null) {
                    livesScoreboardService.refresh(online);
                }
            }
            case ISLAND_TAKEN -> Messages.error(sender, island + " is already assigned to another contestant - remove them first.");
            case UNKNOWN_ISLAND -> Messages.error(sender, "Unknown island '" + island + "'. Options: " + String.join(", ", configManager.islands()));
        }
    }

    @SuppressWarnings("deprecation")
    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Messages.error(sender, "Usage: /survivalisland contestant remove <player>");
            return;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);
        if (contestantManager.remove(player)) {
            Messages.success(sender, "Removed " + args[1] + "'s island assignment.");
            Player online = player.getPlayer();
            if (online != null) {
                livesScoreboardService.remove(online);
            }
            livesScoreboardService.forgetProgress(player.getUniqueId());
        } else {
            Messages.error(sender, args[1] + " isn't assigned to an island.");
        }
    }

    private void handleList(CommandSender sender) {
        if (contestantManager.all().isEmpty()) {
            Messages.info(sender, "No contestants assigned yet.");
            return;
        }
        contestantManager.all().forEach((island, uuid) ->
            Messages.info(sender, island + " -> " + contestantManager.contestantName(island).orElse(uuid.toString())));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filter(List.of("add", "remove", "list"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            return filter(Bukkit.getOnlinePlayers().stream().map(org.bukkit.entity.Player::getName).collect(Collectors.toList()), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            return filter(configManager.islands(), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(lower)).collect(Collectors.toCollection(ArrayList::new));
    }
}
