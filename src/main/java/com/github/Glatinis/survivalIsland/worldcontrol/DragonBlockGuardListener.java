package com.github.Glatinis.survivalIsland.worldcontrol;

import com.github.Glatinis.survivalIsland.containment.EntityBoundsGuard;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

/**
 * Tracks every ender dragon against the arena bound (never an island bound - a "god gift" dragon
 * roams the whole map) and, while destruction is off, lets it damage players without breaking any
 * blocks: block-change events are cancelled outright and its explosions are stripped of their
 * block list, entity damage untouched either way.
 */
public final class DragonBlockGuardListener implements Listener {

    private final DragonControlManager dragonControlManager;
    private final EntityBoundsGuard entityBoundsGuard;

    public DragonBlockGuardListener(DragonControlManager dragonControlManager, EntityBoundsGuard entityBoundsGuard) {
        this.dragonControlManager = dragonControlManager;
        this.entityBoundsGuard = entityBoundsGuard;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() == EntityType.ENDER_DRAGON) {
            entityBoundsGuard.track(event.getEntity(), EntityBoundsGuard.ARENA_BOUND_ID);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof EnderDragon && !dragonControlManager.isDestructionEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof EnderDragon && !dragonControlManager.isDestructionEnabled()) {
            event.blockList().clear();
        }
    }
}
