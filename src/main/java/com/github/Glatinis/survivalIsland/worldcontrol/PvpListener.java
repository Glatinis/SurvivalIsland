package com.github.Glatinis.survivalIsland.worldcontrol;

import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Enforces {@link PvpManager}: player-vs-player damage is cancelled while PvP is globally off,
 * unless either side has bought it. Snowballs and fishing rods always land regardless of PvP
 * state, per the client's ask.
 */
public final class PvpListener implements Listener {

    private final PvpManager pvpManager;

    public PvpListener(PvpManager pvpManager) {
        this.pvpManager = pvpManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Entity damager = event.getDamager();
        if (damager instanceof Snowball || damager instanceof FishHook) {
            return;
        }

        Player attacker = resolveAttacker(damager);
        if (attacker == null || attacker.equals(victim)) {
            return;
        }

        if (!pvpManager.canFight(attacker.getUniqueId(), victim.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }
}
