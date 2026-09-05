package com.github.Glatinis.survivalIsland.worldcontrol;

import com.github.Glatinis.survivalIsland.config.ConfigManager;

/**
 * Global theft toggle. When off (default), a player can only open containers on their own
 * assigned island; when on, any container on any island can be opened by anyone.
 */
public final class TheftManager {

    private boolean enabled;

    public TheftManager(ConfigManager configManager) {
        this.enabled = configManager.theftDefaultEnabled();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
