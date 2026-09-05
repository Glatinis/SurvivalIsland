package com.github.Glatinis.survivalIsland.commandmode;

import com.github.Glatinis.survivalIsland.config.ConfigManager;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks which players currently have command mode active, and resolves a typed trigger to the
 * console commands it maps to (from config.yml, reloadable live).
 */
public final class CommandModeManager {

    private final ConfigManager configManager;
    private final Set<UUID> activePlayers = new HashSet<>();

    public CommandModeManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void enable(UUID uuid) {
        activePlayers.add(uuid);
    }

    public void disable(UUID uuid) {
        activePlayers.remove(uuid);
    }

    public boolean isActive(UUID uuid) {
        return activePlayers.contains(uuid);
    }

    public Optional<List<String>> resolve(String trigger) {
        List<String> commands = configManager.commandModeTriggerCommands(trigger);
        return commands.isEmpty() ? Optional.empty() : Optional.of(commands);
    }
}
