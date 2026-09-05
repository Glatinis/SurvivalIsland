package com.github.Glatinis.survivalIsland.containment;

import com.github.Glatinis.survivalIsland.command.SubCommand;
import com.github.Glatinis.survivalIsland.config.ConfigManager;
import com.github.Glatinis.survivalIsland.contestant.ContestantManager;
import com.github.Glatinis.survivalIsland.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@code /survivalisland spawn <mobtype> <island|player> [amount]} - the concrete answer to
 * "mobs should target the player on their island, not wander": spawns near the resolved player,
 * sets its target, and immediately leashes it to that island via {@link EntityBoundsGuard}. This
 * is deliberately the only new spawn-related command; plain {@code /summon} is left alone for
 * anything that doesn't need targeting + containment.
 */
public final class SpawnSubCommand implements SubCommand {

    private final ContestantManager contestantManager;
    private final ConfigManager configManager;
    private final EntityBoundsGuard entityBoundsGuard;

    public SpawnSubCommand(ContestantManager contestantManager, ConfigManager configManager, EntityBoundsGuard entityBoundsGuard) {
        this.contestantManager = contestantManager;
        this.configManager = configManager;
        this.entityBoundsGuard = entityBoundsGuard;
    }

    @Override
    public String name() {
        return "spawn";
    }

    @Override
    public String permission() {
        return Messages.ADMIN_PERMISSION;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Messages.error(sender, "Usage: /survivalisland spawn <mobtype> <island|player> [amount]");
            return;
        }

        EntityType type = parseMobType(args[0]);
        if (type == null) {
            Messages.error(sender, "'" + args[0] + "' isn't a spawnable mob type.");
            return;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Integer.parseInt(args[2]));
            } catch (NumberFormatException ex) {
                Messages.error(sender, "'" + args[2] + "' isn't a valid amount.");
                return;
            }
        }

        String targetArg = args[1];
        String islandId;
        Player targetPlayer;

        if (configManager.islands().contains(targetArg)) {
            islandId = targetArg;
            targetPlayer = contestantManager.contestantUuid(islandId).map(Bukkit::getPlayer).orElse(null);
            if (targetPlayer == null) {
                Messages.error(sender, "No contestant is currently online for island '" + islandId + "'.");
                return;
            }
        } else {
            targetPlayer = Bukkit.getPlayerExact(targetArg);
            if (targetPlayer == null) {
                Messages.error(sender, "'" + targetArg + "' isn't a known island or an online player.");
                return;
            }
            islandId = contestantManager.islandOf(targetPlayer).orElse(null);
            if (islandId == null) {
                Messages.error(sender, targetPlayer.getName() + " isn't assigned to an island yet - use /survivalisland contestant add first.");
                return;
            }
        }

        Location spawnLocation = targetPlayer.getLocation();
        for (int i = 0; i < amount; i++) {
            Entity entity = spawnLocation.getWorld().spawnEntity(spawnLocation, type, CreatureSpawnEvent.SpawnReason.COMMAND);
            if (entity instanceof Mob mob) {
                mob.setTarget(targetPlayer);
            }
            entityBoundsGuard.track(entity, islandId);
        }

        Messages.success(sender, "Spawned " + amount + "x " + type.name() + " on " + islandId + " targeting " + targetPlayer.getName() + ".");
    }

    private EntityType parseMobType(String raw) {
        try {
            EntityType type = EntityType.valueOf(raw.toUpperCase(Locale.ROOT));
            return type.isAlive() ? type : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toUpperCase(Locale.ROOT);
            return Stream.of(EntityType.values())
                .filter(EntityType::isAlive)
                .map(Enum::name)
                .filter(name -> name.startsWith(prefix))
                .limit(20)
                .collect(Collectors.toCollection(ArrayList::new));
        }
        if (args.length == 2) {
            List<String> options = new ArrayList<>(configManager.islands());
            Bukkit.getOnlinePlayers().forEach(player -> options.add(player.getName()));
            String lower = args[1].toLowerCase(Locale.ROOT);
            return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(lower)).collect(Collectors.toCollection(ArrayList::new));
        }
        return List.of();
    }
}
