package com.github.Glatinis.survivalIsland.lives;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Keeps the lives scoreboard in sync with players joining, dying, and leaving.
 */
public final class PlayerLifecycleListener implements Listener {

    private final LivesScoreboardService livesScoreboardService;

    public PlayerLifecycleListener(LivesScoreboardService livesScoreboardService) {
        this.livesScoreboardService = livesScoreboardService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        livesScoreboardService.applyTo(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        livesScoreboardService.decrement(event.getEntity());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        livesScoreboardService.remove(event.getPlayer());
    }
}
