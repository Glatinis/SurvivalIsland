package com.github.Glatinis.survivalIsland.containment;

import com.github.Glatinis.survivalIsland.config.ConfigManager;
import com.github.Glatinis.survivalIsland.integration.WorldGuardHook;
import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

/**
 * Registers newly-spawned (allowed-reason) mobs with {@link EntityBoundsGuard}, tagging each with
 * the island region it spawned in - this covers Rcian's existing command-block spawns
 * automatically, no extra step needed on his end. Also proactively cancels AI pathing outside a
 * mob's bound, so the periodic containment correction rarely has to visibly "pop" anything back.
 */
public final class MobLeashListener implements Listener {

    private final EntityBoundsGuard entityBoundsGuard;
    private final WorldGuardHook worldGuardHook;
    private final ConfigManager configManager;

    public MobLeashListener(EntityBoundsGuard entityBoundsGuard, WorldGuardHook worldGuardHook, ConfigManager configManager) {
        this.entityBoundsGuard = entityBoundsGuard;
        this.worldGuardHook = worldGuardHook;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        worldGuardHook.regionIdAt(entity.getLocation(), configManager.islands())
            .ifPresent(islandId -> entityBoundsGuard.track(entity, islandId));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPathfind(EntityPathfindEvent event) {
        if (!entityBoundsGuard.isWithinOwnBound(event.getEntity(), event.getLoc())) {
            event.setCancelled(true);
        }
    }
}
