package com.github.Glatinis.survivalIsland.command;

import com.github.Glatinis.survivalIsland.config.ConfigManager;
import com.github.Glatinis.survivalIsland.containment.EntityBoundsGuard;
import com.github.Glatinis.survivalIsland.util.Messages;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * {@code /survivalisland reload} - reloads config.yml and clears the containment bounds cache so
 * region/arena changes actually take effect without a full server restart. Everything else reads
 * its config values fresh on every use already, so nothing further needs invalidating.
 */
public final class ReloadSubCommand implements SubCommand {

    private final ConfigManager configManager;
    private final EntityBoundsGuard entityBoundsGuard;

    public ReloadSubCommand(ConfigManager configManager, EntityBoundsGuard entityBoundsGuard) {
        this.configManager = configManager;
        this.entityBoundsGuard = entityBoundsGuard;
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public String permission() {
        return Messages.ADMIN_PERMISSION;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        configManager.reload();
        entityBoundsGuard.clearBoundCache();
        Messages.success(sender, "Reloaded config.yml.");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
