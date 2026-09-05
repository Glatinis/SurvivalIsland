package com.github.Glatinis.survivalIsland.containment;

import com.github.Glatinis.survivalIsland.config.ConfigManager;
import com.github.Glatinis.survivalIsland.integration.WorldGuardHook;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.List;

/**
 * Blocks ambient/natural mob spawning entirely (mobs should only ever come from the audience via
 * gift commands, spawn eggs, or spawners) and additionally blocks *any* spawn - regardless of
 * reason - inside the safe-tower region.
 */
public final class NaturalSpawnListener implements Listener {

    private final ConfigManager configManager;
    private final WorldGuardHook worldGuardHook;

    public NaturalSpawnListener(ConfigManager configManager, WorldGuardHook worldGuardHook) {
        this.configManager = configManager;
        this.worldGuardHook = worldGuardHook;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (configManager.blockedNaturalSpawnReasons().contains(event.getSpawnReason())) {
            event.setCancelled(true);
            return;
        }

        List<String> safeTowerOnly = List.of(configManager.safeTowerRegion());
        if (worldGuardHook.regionIdAt(event.getLocation(), safeTowerOnly).isPresent()) {
            event.setCancelled(true);
        }
    }
}
