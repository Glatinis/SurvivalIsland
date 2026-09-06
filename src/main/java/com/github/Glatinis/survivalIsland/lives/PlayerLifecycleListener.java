package com.github.Glatinis.survivalIsland.lives;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Keeps the lives scoreboard in sync with players joining and dying. Quitting deliberately does
 * nothing here: a contestant's row should stay visible to everyone else (with their last known
 * lives) while they're briefly offline, not disappear the moment they disconnect.
 */
public final class PlayerLifecycleListener implements Listener {

    private final LivesScoreboardService livesScoreboardService;

    public PlayerLifecycleListener(LivesScoreboardService livesScoreboardService) {
        this.livesScoreboardService = livesScoreboardService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        livesScoreboardService.refresh(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        livesScoreboardService.decrement(event.getEntity());
    }
}
