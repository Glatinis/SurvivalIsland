package com.github.Glatinis.survivalIsland.worldcontrol;

import com.github.Glatinis.survivalIsland.config.ConfigManager;
import com.github.Glatinis.survivalIsland.integration.WorldGuardHook;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.List;

/**
 * TNT always breaks island blocks no matter what {@link ProtectionManager} mode is active - the
 * only thing it can never touch is the safe tower.
 */
public final class TntExplosionListener implements Listener {

    private final WorldGuardHook worldGuardHook;
    private final ConfigManager configManager;

    public TntExplosionListener(WorldGuardHook worldGuardHook, ConfigManager configManager) {
        this.worldGuardHook = worldGuardHook;
        this.configManager = configManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed)) {
            return;
        }

        List<String> safeTowerOnly = List.of(configManager.safeTowerRegion());
        event.blockList().removeIf(block ->
            worldGuardHook.regionIdAt(block.getLocation(), safeTowerOnly).isPresent());
    }
}
