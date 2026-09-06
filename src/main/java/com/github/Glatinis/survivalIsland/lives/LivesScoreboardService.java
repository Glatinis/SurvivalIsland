package com.github.Glatinis.survivalIsland.lives;

import com.github.Glatinis.survivalIsland.config.ConfigManager;
import com.github.Glatinis.survivalIsland.contestant.ContestantManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Owns the sidebar "lives" objective on a private scoreboard the plugin manages itself, not the
 * server's shared main scoreboard, so it never shows in {@code /scoreboard objectives list} and
 * vanilla {@code /scoreboard players} commands can't read or change it (also meaning nothing
 * outside this plugin, accidentally or otherwise, can touch it - only reachable through
 * {@code /survivalisland lives} or a death). Every online player sees the board, but only
 * assigned contestants ever get an actual row (a number) on it.
 *
 * <p>Two things make this resilient to what actually goes wrong on a live server:
 * <ul>
 *   <li>{@link #setup()} is idempotent and {@link #shutdown()} properly tears everything down
 *   (unregisters the objective, returns every viewer to the main scoreboard) - without this, a
 *   plugin reload that calls {@code onEnable} again without a clean {@code onDisable} in between
 *   would silently create a second, disconnected Scoreboard object and split contestants across
 *   two boards.</li>
 *   <li>Every value is mirrored into {@code lives.yml} as it changes and restored from there on
 *   the next {@link #refresh}, since this Scoreboard is an in-memory-only Bukkit construct (unlike
 *   the server's main scoreboard, it is never saved to disk on its own) - without this, a server
 *   crash or restart mid-show would silently reset every contestant back to full lives.</li>
 * </ul>
 */
public final class LivesScoreboardService {

    private static final String OBJECTIVE_NAME = "lives";

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final ContestantManager contestantManager;
    private final File file;
    private final Map<UUID, Integer> persisted = new HashMap<>();

    private Scoreboard scoreboard;
    private Objective objective;

    public LivesScoreboardService(JavaPlugin plugin, ConfigManager configManager, ContestantManager contestantManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.contestantManager = contestantManager;
        this.file = new File(plugin.getDataFolder(), "lives.yml");
    }

    public void setup() {
        if (scoreboard != null) {
            plugin.getLogger().warning("Lives scoreboard setup() called while already active - ignoring.");
            return;
        }
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            plugin.getLogger().warning("No ScoreboardManager available - lives scoreboard disabled.");
            return;
        }
        scoreboard = manager.getNewScoreboard();
        Component title = GsonComponentSerializer.gson().deserialize(configManager.livesScoreboardTitleJson());
        objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        loadPersisted();
    }

    /**
     * Unregisters the objective and returns every current viewer to the main scoreboard, so
     * nothing is left pointing at a Scoreboard object tied to this (possibly about-to-be-unloaded)
     * plugin instance.
     */
    public void shutdown() {
        if (scoreboard == null) {
            return;
        }
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard main = manager != null ? manager.getMainScoreboard() : null;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (main != null && player.getScoreboard().equals(scoreboard)) {
                player.setScoreboard(main);
            }
        }
        objective.unregister();
        objective = null;
        scoreboard = null;
    }

    /**
     * Shows the board to the player unconditionally - everyone sees it, contestant or not - and,
     * if they're an assigned contestant without a row yet, initializes one (restoring their last
     * known value from {@code lives.yml} if the show has been through a restart since they last
     * had a score). A non-contestant simply never gets a row, rather than being kept off the
     * board entirely.
     */
    public void refresh(Player player) {
        if (scoreboard == null || objective == null) {
            return;
        }
        player.setScoreboard(scoreboard);
        if (contestantManager.islandOf(player).isPresent()) {
            Score score = objective.getScore(player.getName());
            if (!score.isScoreSet()) {
                int initial = persisted.getOrDefault(player.getUniqueId(), configManager.startingLives());
                score.setScore(initial);
            }
            applyEliminationState(player, score.getScore());
        }
    }

    public void decrement(Player player) {
        if (objective == null || contestantManager.islandOf(player).isEmpty()) {
            return;
        }
        Score score = objective.getScore(player.getName());
        int updated = Math.max(0, score.getScore() - 1);
        score.setScore(updated);
        setPersisted(player.getUniqueId(), updated);
        applyEliminationState(player, updated);
    }

    /**
     * Sets a contestant's lives to an absolute value (clamped to 0 or above). Returns the value
     * actually applied.
     */
    public int setLives(Player player, int value) {
        int clamped = Math.max(0, value);
        if (objective != null) {
            objective.getScore(player.getName()).setScore(clamped);
            setPersisted(player.getUniqueId(), clamped);
            applyEliminationState(player, clamped);
        }
        return clamped;
    }

    /**
     * Adds (or, with a negative delta, subtracts) from a contestant's current lives, clamped to 0
     * or above. The addition is done in {@code long} arithmetic before clamping back into the
     * {@code int} range a Score actually stores, so a large delta can't wrap around through
     * integer overflow. Returns the resulting value.
     */
    public int addLives(Player player, int delta) {
        if (objective == null) {
            return 0;
        }
        Score score = objective.getScore(player.getName());
        long current = score.isScoreSet() ? score.getScore() : configManager.startingLives();
        long updatedLong = Math.max(0L, current + (long) delta);
        int updated = (int) Math.min(updatedLong, Integer.MAX_VALUE);
        score.setScore(updated);
        setPersisted(player.getUniqueId(), updated);
        applyEliminationState(player, updated);
        return updated;
    }

    /**
     * Puts a contestant into spectator mode the moment their lives hit 0, and pulls them back
     * into survival mode if their lives go from 0 back to a positive number (a manual revive via
     * {@code /survivalisland lives add}). Only touches game mode on an actual state change, never
     * fights a manually-set spectator mode that isn't the result of hitting 0 lives.
     */
    private void applyEliminationState(Player player, int lives) {
        if (lives <= 0) {
            if (player.getGameMode() != GameMode.SPECTATOR) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        } else if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    /**
     * Clears a contestant's row only - called when their contestant assignment is explicitly
     * removed. Everyone always keeps seeing the board itself regardless of contestant status, so
     * this deliberately does not touch the player's scoreboard assignment, only the row. Does not
     * touch their persisted lives value either - that's a separate, explicit action; see
     * {@link #forgetProgress}.
     */
    public void clearRow(Player player) {
        if (scoreboard == null) {
            return;
        }
        scoreboard.resetScores(player.getName());
    }

    /**
     * Erases a player's persisted lives value entirely, so if they're assigned as a contestant
     * again later they start fresh at the configured starting lives rather than resuming their
     * old count. Called specifically when a contestant assignment is removed, not on every
     * disconnect.
     */
    public void forgetProgress(UUID uuid) {
        if (persisted.remove(uuid) != null) {
            persistAll();
        }
    }

    private void setPersisted(UUID uuid, int value) {
        persisted.put(uuid, value);
        persistAll();
    }

    private void loadPersisted() {
        persisted.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                persisted.put(UUID.fromString(key), config.getInt(key));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Invalid UUID in lives.yml, skipping: " + key);
            }
        }
    }

    private void persistAll() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Integer> entry : persisted.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save lives.yml", e);
        }
    }
}
