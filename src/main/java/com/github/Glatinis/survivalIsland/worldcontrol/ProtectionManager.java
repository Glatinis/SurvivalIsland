package com.github.Glatinis.survivalIsland.worldcontrol;

import com.github.Glatinis.survivalIsland.config.ConfigManager;

/**
 * The global island-breaking mode: fully locked, own-island-only, or free-for-all. Defaults to
 * {@code LOCKED} so islands start fully protected until an operator explicitly opens them up.
 */
public final class ProtectionManager {

    public enum Mode {
        LOCKED, OWN_ISLAND_ONLY, FREE_FOR_ALL
    }

    private Mode mode;

    public ProtectionManager(ConfigManager configManager) {
        Mode parsed;
        try {
            parsed = Mode.valueOf(configManager.defaultProtectionMode());
        } catch (IllegalArgumentException ex) {
            parsed = Mode.LOCKED;
        }
        this.mode = parsed;
    }

    public Mode mode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }
}
