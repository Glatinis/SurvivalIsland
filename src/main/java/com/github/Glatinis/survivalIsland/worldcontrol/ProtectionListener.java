package com.github.Glatinis.survivalIsland.worldcontrol;

import com.github.Glatinis.survivalIsland.config.ConfigManager;
import com.github.Glatinis.survivalIsland.contestant.ContestantManager;
import com.github.Glatinis.survivalIsland.integration.WorldGuardHook;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Enforces {@link ProtectionManager}'s three global modes for ordinary block break/place. The
 * safe-tower region is always fully protected, checked before the mode itself - see
 * {@link TntExplosionListener} for the one exception to island protection (TNT).
 */
public final class ProtectionListener implements Listener {

    private final ProtectionManager protectionManager;
    private final ContestantManager contestantManager;
    private final WorldGuardHook worldGuardHook;
    private final ConfigManager configManager;

    public ProtectionListener(ProtectionManager protectionManager, ContestantManager contestantManager,
                               WorldGuardHook worldGuardHook, ConfigManager configManager) {
        this.protectionManager = protectionManager;
        this.contestantManager = contestantManager;
        this.worldGuardHook = worldGuardHook;
        this.configManager = configManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!isAllowed(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!isAllowed(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    private boolean isAllowed(Player player, Location location) {
        List<String> candidates = new ArrayList<>(configManager.islands());
        candidates.add(configManager.safeTowerRegion());

        Optional<String> regionId = worldGuardHook.regionIdAt(location, candidates);
        if (regionId.isEmpty()) {
            return true;
        }

        String region = regionId.get();
        if (region.equals(configManager.safeTowerRegion())) {
            return false;
        }

        return switch (protectionManager.mode()) {
            case LOCKED -> false;
            case FREE_FOR_ALL -> true;
            case OWN_ISLAND_ONLY -> contestantManager.islandOf(player).map(region::equals).orElse(false);
        };
    }
}
