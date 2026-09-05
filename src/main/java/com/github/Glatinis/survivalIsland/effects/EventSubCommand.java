package com.github.Glatinis.survivalIsland.effects;

import com.github.Glatinis.survivalIsland.command.SubCommand;
import com.github.Glatinis.survivalIsland.util.Messages;
import com.github.Glatinis.survivalIsland.worldcontrol.DragonControlManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code /survivalisland event <acidrain|acidocean> <player|all> <start|stop>} - the gift-effect
 * commands. (Deep Freeze and ender dragon control join this same category once built.)
 */
public final class EventSubCommand implements SubCommand {

    private final AcidRainManager acidRainManager;
    private final AcidOceanManager acidOceanManager;
    private final DeepFreezeManager deepFreezeManager;
    private final DragonControlManager dragonControlManager;

    public EventSubCommand(AcidRainManager acidRainManager, AcidOceanManager acidOceanManager,
                            DeepFreezeManager deepFreezeManager, DragonControlManager dragonControlManager) {
        this.acidRainManager = acidRainManager;
        this.acidOceanManager = acidOceanManager;
        this.deepFreezeManager = deepFreezeManager;
        this.dragonControlManager = dragonControlManager;
    }

    @Override
    public String name() {
        return "event";
    }

    @Override
    public String permission() {
        return Messages.ADMIN_PERMISSION;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            Messages.error(sender, "Usage: /survivalisland event <acidrain|acidocean|deepfreeze|enderdragon> ...");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "acidrain" -> handleAcidRain(sender, args);
            case "acidocean" -> handleAcidOcean(sender, args);
            case "deepfreeze" -> handleDeepFreeze(sender, args);
            case "enderdragon" -> handleEnderDragon(sender, args);
            default -> Messages.error(sender, "Usage: /survivalisland event <acidrain|acidocean|deepfreeze|enderdragon> ...");
        }
    }

    private void handleEnderDragon(CommandSender sender, String[] args) {
        if (args.length < 3 || !args[1].equalsIgnoreCase("destruction")) {
            Messages.error(sender, "Usage: /survivalisland event enderdragon destruction <on|off>");
            return;
        }
        switch (args[2].toLowerCase()) {
            case "on" -> {
                dragonControlManager.setDestructionEnabled(true);
                Messages.success(sender, "Ender dragons can now destroy blocks.");
            }
            case "off" -> {
                dragonControlManager.setDestructionEnabled(false);
                Messages.success(sender, "Ender dragons can no longer destroy blocks.");
            }
            default -> Messages.error(sender, "Usage: /survivalisland event enderdragon destruction <on|off>");
        }
    }

    private void handleDeepFreeze(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Messages.error(sender, "Usage: /survivalisland event deepfreeze <on|off>");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "on" -> {
                deepFreezeManager.turnOn();
                Messages.success(sender, "Deep freeze is now on.");
            }
            case "off" -> {
                deepFreezeManager.turnOff();
                Messages.success(sender, "Deep freeze is now off.");
            }
            default -> Messages.error(sender, "Usage: /survivalisland event deepfreeze <on|off>");
        }
    }

    private void handleAcidRain(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Messages.error(sender, "Usage: /survivalisland event acidrain <player|all> <start|stop>");
            return;
        }
        List<Player> targets = resolveTargets(sender, args[1]);
        if (targets == null) {
            return;
        }
        Boolean start = parseStartStop(sender, args[2]);
        if (start == null) {
            return;
        }
        for (Player target : targets) {
            if (start) {
                acidRainManager.start(target);
            } else {
                acidRainManager.stop(target);
            }
        }
        Messages.success(sender, "Acid rain " + (start ? "started" : "stopped") + " for " + describe(args[1], targets) + ".");
    }

    private void handleAcidOcean(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Messages.error(sender, "Usage: /survivalisland event acidocean <player|all> <start|stop>");
            return;
        }
        List<Player> targets = resolveTargets(sender, args[1]);
        if (targets == null) {
            return;
        }
        Boolean start = parseStartStop(sender, args[2]);
        if (start == null) {
            return;
        }
        for (Player target : targets) {
            if (start) {
                acidOceanManager.enable(target);
            } else {
                acidOceanManager.disable(target);
            }
        }
        Messages.success(sender, "Acid ocean " + (start ? "started" : "stopped") + " for " + describe(args[1], targets) + ".");
    }

    private List<Player> resolveTargets(CommandSender sender, String arg) {
        if (arg.equalsIgnoreCase("all")) {
            return new ArrayList<>(Bukkit.getOnlinePlayers());
        }
        Player player = Bukkit.getPlayerExact(arg);
        if (player == null) {
            Messages.error(sender, "Player '" + arg + "' is not online.");
            return null;
        }
        return List.of(player);
    }

    private Boolean parseStartStop(CommandSender sender, String arg) {
        if (arg.equalsIgnoreCase("start")) {
            return Boolean.TRUE;
        }
        if (arg.equalsIgnoreCase("stop")) {
            return Boolean.FALSE;
        }
        Messages.error(sender, "Expected 'start' or 'stop', got '" + arg + "'.");
        return null;
    }

    private String describe(String arg, List<Player> targets) {
        return arg.equalsIgnoreCase("all") ? "all players" : targets.get(0).getName();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filter(List.of("acidrain", "acidocean", "deepfreeze", "enderdragon"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("deepfreeze")) {
            return filter(List.of("on", "off"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("enderdragon")) {
            return filter(List.of("destruction"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("enderdragon") && args[1].equalsIgnoreCase("destruction")) {
            return filter(List.of("on", "off"), args[2]);
        }
        if (args.length == 2) {
            List<String> options = new ArrayList<>();
            options.add("all");
            Bukkit.getOnlinePlayers().forEach(player -> options.add(player.getName()));
            return filter(options, args[1]);
        }
        if (args.length == 3) {
            return filter(List.of("start", "stop"), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(lower)).collect(Collectors.toCollection(ArrayList::new));
    }
}
