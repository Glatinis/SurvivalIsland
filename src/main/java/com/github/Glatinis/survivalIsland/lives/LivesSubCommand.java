package com.github.Glatinis.survivalIsland.lives;

import com.github.Glatinis.survivalIsland.command.SubCommand;
import com.github.Glatinis.survivalIsland.contestant.ContestantManager;
import com.github.Glatinis.survivalIsland.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code /survivalisland lives <set|add|remove> <player> <amount>} - manual lives adjustment for
 * a contestant. The lives board is a private scoreboard, not the server's main one, so this is
 * the only way to change a contestant's lives (the vanilla {@code /scoreboard players} command
 * can't reach it).
 */
public final class LivesSubCommand implements SubCommand {

    private final LivesScoreboardService livesScoreboardService;
    private final ContestantManager contestantManager;

    public LivesSubCommand(LivesScoreboardService livesScoreboardService, ContestantManager contestantManager) {
        this.livesScoreboardService = livesScoreboardService;
        this.contestantManager = contestantManager;
    }

    @Override
    public String name() {
        return "lives";
    }

    @Override
    public String permission() {
        return Messages.ADMIN_PERMISSION;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Messages.error(sender, "Usage: /survivalisland lives <set|add|remove> <player> <amount>");
            return;
        }

        String action = args[0].toLowerCase();
        if (!action.equals("set") && !action.equals("add") && !action.equals("remove")) {
            Messages.error(sender, "Usage: /survivalisland lives <set|add|remove> <player> <amount>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            Messages.error(sender, "Player '" + args[1] + "' is not online.");
            return;
        }

        if (contestantManager.islandOf(target).isEmpty()) {
            Messages.error(sender, target.getName() + " isn't an assigned contestant.");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            Messages.error(sender, "'" + args[2] + "' isn't a valid number.");
            return;
        }

        int result = switch (action) {
            case "set" -> livesScoreboardService.setLives(target, amount);
            case "add" -> livesScoreboardService.addLives(target, amount);
            default -> livesScoreboardService.addLives(target, -amount);
        };

        Messages.success(sender, target.getName() + "'s lives: " + result + ".");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filter(List.of("set", "add", "remove"), args[0]);
        }
        if (args.length == 2) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(lower)).collect(Collectors.toCollection(ArrayList::new));
    }
}
