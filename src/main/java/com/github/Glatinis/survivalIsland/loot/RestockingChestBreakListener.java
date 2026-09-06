package com.github.Glatinis.survivalIsland.loot;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Stops tracking a restocking chest the moment it's actually broken, rather than waiting for the
 * next restock tick to notice it's gone.
 */
public final class RestockingChestBreakListener implements Listener {

    private final RestockingChestManager restockingChestManager;

    public RestockingChestBreakListener(RestockingChestManager restockingChestManager) {
        this.restockingChestManager = restockingChestManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        restockingChestManager.unmark(event.getBlock());
    }
}
