package com.github.Glatinis.survivalIsland.worldcontrol;

import com.github.Glatinis.survivalIsland.config.ConfigManager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Global PvP on/off plus a per-player "bought PvP" override. Both are purely in-memory runtime
 * state - viewers pay on E-pal, an operator runs the in-game toggle, the plugin never touches
 * payment itself.
 */
public final class PvpManager {

    private boolean enabled;
    private final Set<UUID> boughtPvp = new HashSet<>();

    public PvpManager(ConfigManager configManager) {
        this.enabled = configManager.pvpDefaultEnabled();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void grantBoughtPvp(UUID uuid) {
        boughtPvp.add(uuid);
    }

    public void revokeBoughtPvp(UUID uuid) {
        boughtPvp.remove(uuid);
    }

    public boolean hasBoughtPvp(UUID uuid) {
        return boughtPvp.contains(uuid);
    }

    public boolean canFight(UUID attacker, UUID victim) {
        return enabled || boughtPvp.contains(attacker) || boughtPvp.contains(victim);
    }
}
