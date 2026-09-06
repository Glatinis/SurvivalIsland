package com.github.Glatinis.survivalIsland.loot;

import com.github.Glatinis.survivalIsland.command.SubCommand;
import com.github.Glatinis.survivalIsland.util.Messages;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@code /survivalisland chest <mark|unmark|restock|list> ...} - marks the chest a player is
 * looking at as one that periodically clears and refills itself from the loot pool.
 */
public final class ChestSubCommand implements SubCommand {

    private static final int REACH_DISTANCE = 6;

    private final RestockingChestManager restockingChestManager;

    public ChestSubCommand(RestockingChestManager restockingChestManager) {
        this.restockingChestManager = restockingChestManager;
    }

    @Override
    public String name() {
        return "chest";
    }

    @Override
    public String permission() {
        return Messages.ADMIN_PERMISSION;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            Messages.error(sender, "Usage: /survivalisland chest <mark|unmark|restock|list> ...");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "mark" -> handleMark(sender, args);
            case "unmark" -> handleUnmark(sender);
            case "restock" -> handleRestock(sender);
            case "list" -> Messages.info(sender, restockingChestManager.count() + " restocking chest(s) currently tracked.");
            default -> Messages.error(sender, "Usage: /survivalisland chest <mark|unmark|restock|list> ...");
        }
    }

    private void handleMark(CommandSender sender, String[] args) {
        Block block = targetChest(sender);
        if (block == null) {
            return;
        }

        Long intervalTicks = null;
        if (args.length >= 2) {
            try {
                intervalTicks = Long.parseLong(args[1]) * 20L;
            } catch (NumberFormatException ex) {
                Messages.error(sender, "'" + args[1] + "' isn't a valid number of seconds.");
                return;
            }
        }

        Integer itemCount = null;
        if (args.length >= 3) {
            try {
                itemCount = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                Messages.error(sender, "'" + args[2] + "' isn't a valid item count.");
                return;
            }
        }

        if (restockingChestManager.mark(block, intervalTicks, itemCount)) {
            Messages.success(sender, "That chest will now restock" + (args.length >= 2 ? " every " + args[1] + "s" : "") + ".");
        } else {
            Messages.error(sender, "That's either not a chest, or it's already marked.");
        }
    }

    private void handleUnmark(CommandSender sender) {
        Block block = targetChest(sender);
        if (block == null) {
            return;
        }
        if (restockingChestManager.unmark(block)) {
            Messages.success(sender, "That chest will no longer restock.");
        } else {
            Messages.error(sender, "That chest wasn't marked.");
        }
    }

    private void handleRestock(CommandSender sender) {
        Block block = targetChest(sender);
        if (block == null) {
            return;
        }
        if (restockingChestManager.restockNow(block)) {
            Messages.success(sender, "Restocked.");
        } else {
            Messages.error(sender, "That chest wasn't marked.");
        }
    }

    private Block targetChest(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Messages.error(sender, "Only a player can target a chest.");
            return null;
        }
        Block block = player.getTargetBlockExact(REACH_DISTANCE);
        if (block == null || !(block.getState() instanceof Chest)) {
            Messages.error(sender, "You need to be looking directly at a chest.");
            return null;
        }
        return block;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String lower = args[0].toLowerCase();
            return Stream.of("mark", "unmark", "restock", "list")
                .filter(s -> s.startsWith(lower))
                .collect(Collectors.toCollection(ArrayList::new));
        }
        return List.of();
    }
}
