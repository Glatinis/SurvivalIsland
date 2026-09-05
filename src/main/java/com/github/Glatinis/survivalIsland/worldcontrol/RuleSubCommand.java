package com.github.Glatinis.survivalIsland.worldcontrol;

import com.github.Glatinis.survivalIsland.command.SubCommand;
import com.github.Glatinis.survivalIsland.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code /survivalisland rule <pvp|theft|protection> ...} - the global world-behavior toggles.
 */
public final class RuleSubCommand implements SubCommand {

    private final PvpManager pvpManager;
    private final TheftManager theftManager;
    private final ProtectionManager protectionManager;

    public RuleSubCommand(PvpManager pvpManager, TheftManager theftManager, ProtectionManager protectionManager) {
        this.pvpManager = pvpManager;
        this.theftManager = theftManager;
        this.protectionManager = protectionManager;
    }

    @Override
    public String name() {
        return "rule";
    }

    @Override
    public String permission() {
        return Messages.ADMIN_PERMISSION;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            Messages.error(sender, "Usage: /survivalisland rule <pvp|theft|protection> ...");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "pvp" -> handlePvp(sender, args);
            case "theft" -> handleTheft(sender, args);
            case "protection" -> handleProtection(sender, args);
            default -> Messages.error(sender, "Usage: /survivalisland rule <pvp|theft|protection> ...");
        }
    }

    private void handlePvp(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Messages.error(sender, "Usage: /survivalisland rule pvp <on|off|buy> [player]");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "on" -> {
                pvpManager.setEnabled(true);
                Messages.success(sender, "PvP is now on for everyone.");
            }
            case "off" -> {
                pvpManager.setEnabled(false);
                Messages.success(sender, "PvP is now off (unless bought).");
            }
            case "buy" -> handlePvpBuy(sender, args);
            default -> Messages.error(sender, "Usage: /survivalisland rule pvp <on|off|buy> [player]");
        }
    }

    private void handlePvpBuy(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Messages.error(sender, "Usage: /survivalisland rule pvp buy <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            Messages.error(sender, "Player '" + args[2] + "' is not online.");
            return;
        }
        pvpManager.grantBoughtPvp(target.getUniqueId());
        Messages.success(sender, target.getName() + " can now fight regardless of the global PvP state.");
    }

    private void handleTheft(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Messages.error(sender, "Usage: /survivalisland rule theft <on|off>");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "on" -> {
                theftManager.setEnabled(true);
                Messages.success(sender, "Theft is now allowed on any island.");
            }
            case "off" -> {
                theftManager.setEnabled(false);
                Messages.success(sender, "Theft is now blocked outside a player's own island.");
            }
            default -> Messages.error(sender, "Usage: /survivalisland rule theft <on|off>");
        }
    }

    private void handleProtection(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Messages.error(sender, "Usage: /survivalisland rule protection <locked|own|free>");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "locked" -> {
                protectionManager.setMode(ProtectionManager.Mode.LOCKED);
                Messages.success(sender, "Protection mode: locked - no one can break islands.");
            }
            case "own" -> {
                protectionManager.setMode(ProtectionManager.Mode.OWN_ISLAND_ONLY);
                Messages.success(sender, "Protection mode: own island only.");
            }
            case "free" -> {
                protectionManager.setMode(ProtectionManager.Mode.FREE_FOR_ALL);
                Messages.success(sender, "Protection mode: free for all.");
            }
            default -> Messages.error(sender, "Usage: /survivalisland rule protection <locked|own|free>");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filter(List.of("pvp", "theft", "protection"), args[0]);
        }
        if (args.length == 2) {
            List<String> options = switch (args[0].toLowerCase()) {
                case "pvp" -> List.of("on", "off", "buy");
                case "theft" -> List.of("on", "off");
                case "protection" -> List.of("locked", "own", "free");
                default -> List.<String>of();
            };
            return filter(options, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("pvp") && args[1].equalsIgnoreCase("buy")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(lower)).collect(Collectors.toCollection(ArrayList::new));
    }
}
