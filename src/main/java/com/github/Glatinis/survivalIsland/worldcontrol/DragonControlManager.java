package com.github.Glatinis.survivalIsland.worldcontrol;

/**
 * Whether ender dragons are currently allowed to change/destroy blocks. Off by default so a
 * "god gift" dragon can damage players without wrecking islands, the ocean, bedrock, or rafts.
 */
public final class DragonControlManager {

    private boolean destructionEnabled = false;

    public boolean isDestructionEnabled() {
        return destructionEnabled;
    }

    public void setDestructionEnabled(boolean destructionEnabled) {
        this.destructionEnabled = destructionEnabled;
    }
}
