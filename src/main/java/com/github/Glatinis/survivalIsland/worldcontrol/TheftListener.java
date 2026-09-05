package com.github.Glatinis.survivalIsland.worldcontrol;

import com.github.Glatinis.survivalIsland.config.ConfigManager;
import com.github.Glatinis.survivalIsland.contestant.ContestantManager;
import com.github.Glatinis.survivalIsland.integration.WorldGuardHook;
import com.github.Glatinis.survivalIsland.util.Messages;
import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;

/**
 * Enforces {@link TheftManager}: while theft is off, only the assigned contestant of an island
 * may open containers physically located on that island. Containers outside any island (or any
 * non-block inventory, like a player's own inventory) are never guarded. Ops bypass this
 * entirely.
 */
public final class TheftListener implements Listener {

    private final TheftManager theftManager;
    private final ContestantManager contestantManager;
    private final WorldGuardHook worldGuardHook;
    private final ConfigManager configManager;

    public TheftListener(TheftManager theftManager, ContestantManager contestantManager,
                          WorldGuardHook worldGuardHook, ConfigManager configManager) {
        this.theftManager = theftManager;
        this.contestantManager = contestantManager;
        this.worldGuardHook = worldGuardHook;
        this.configManager = configManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (theftManager.isEnabled()) {
            return;
        }

        HumanEntity opener = event.getPlayer();
        if (!(opener instanceof Player player) || player.isOp()) {
            return;
        }

        Location location = event.getInventory().getLocation();
        if (location == null) {
            return;
        }

        worldGuardHook.regionIdAt(location, configManager.islands()).ifPresent(island -> {
            boolean isOwner = contestantManager.islandOf(player).map(island::equals).orElse(false);
            if (!isOwner) {
                event.setCancelled(true);
                Messages.error(player, "This chest belongs to another island.");
            }
        });
    }
}
