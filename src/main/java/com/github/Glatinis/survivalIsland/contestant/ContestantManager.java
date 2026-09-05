package com.github.Glatinis.survivalIsland.contestant;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Owns the player <-> island assignment ("which island is this contestant's") - the single
 * source of truth every other feature (effects, protection, targeted spawns) resolves against,
 * instead of WorldGuard region ownership. Persisted to contestants.yml so it survives a restart
 * mid-show.
 */
public final class ContestantManager {

    public enum AssignResult {
        OK, ISLAND_TAKEN, UNKNOWN_ISLAND
    }

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, UUID> islandToPlayer = new LinkedHashMap<>();
    private final Map<String, String> islandToName = new LinkedHashMap<>();

    public ContestantManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "contestants.yml");
    }

    public void load() {
        islandToPlayer.clear();
        islandToName.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String island : config.getKeys(false)) {
            String uuidString = config.getString(island + ".uuid");
            if (uuidString == null) {
                continue;
            }
            try {
                islandToPlayer.put(island, UUID.fromString(uuidString));
                islandToName.put(island, config.getString(island + ".name", uuidString));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Invalid contestant UUID for '" + island + "' in contestants.yml, skipping.");
            }
        }
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, UUID> entry : islandToPlayer.entrySet()) {
            String island = entry.getKey();
            config.set(island + ".uuid", entry.getValue().toString());
            config.set(island + ".name", islandToName.get(island));
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save contestants.yml", e);
        }
    }

    /**
     * Assigns a player to an island. A player can only hold one island at a time, so their
     * previous assignment (if any) is cleared first. Fails if the island is already held by a
     * different player - remove them first.
     */
    public AssignResult assign(OfflinePlayer player, String islandId, List<String> knownIslands) {
        if (!knownIslands.contains(islandId)) {
            return AssignResult.UNKNOWN_ISLAND;
        }
        UUID existingOwner = islandToPlayer.get(islandId);
        if (existingOwner != null && !existingOwner.equals(player.getUniqueId())) {
            return AssignResult.ISLAND_TAKEN;
        }

        islandOf(player.getUniqueId()).ifPresent(previous -> {
            islandToPlayer.remove(previous);
            islandToName.remove(previous);
        });

        islandToPlayer.put(islandId, player.getUniqueId());
        islandToName.put(islandId, player.getName() != null ? player.getName() : player.getUniqueId().toString());
        save();
        return AssignResult.OK;
    }

    public boolean remove(OfflinePlayer player) {
        return islandOf(player.getUniqueId())
            .map(island -> {
                islandToPlayer.remove(island);
                islandToName.remove(island);
                save();
                return true;
            })
            .orElse(false);
    }

    public Optional<String> islandOf(OfflinePlayer player) {
        return islandOf(player.getUniqueId());
    }

    public Optional<String> islandOf(UUID uuid) {
        return islandToPlayer.entrySet().stream()
            .filter(entry -> entry.getValue().equals(uuid))
            .map(Map.Entry::getKey)
            .findFirst();
    }

    public Optional<UUID> contestantUuid(String islandId) {
        return Optional.ofNullable(islandToPlayer.get(islandId));
    }

    public Optional<OfflinePlayer> contestant(String islandId) {
        return contestantUuid(islandId).map(Bukkit::getOfflinePlayer);
    }

    public Optional<String> contestantName(String islandId) {
        return Optional.ofNullable(islandToName.get(islandId));
    }

    public Map<String, UUID> all() {
        return Collections.unmodifiableMap(islandToPlayer);
    }
}
