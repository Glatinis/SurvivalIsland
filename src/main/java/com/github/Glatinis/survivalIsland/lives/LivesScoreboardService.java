package com.github.Glatinis.survivalIsland.lives;

import com.github.Glatinis.survivalIsland.config.ConfigManager;
import com.github.Glatinis.survivalIsland.contestant.ContestantManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
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
 * {@code /survivalisland lives} or a death). Only assigned contestants ever get a row on it.
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
     * Shows the board and initializes a score for the player if they're an assigned contestant
     * (restoring their last known value from {@code lives.yml} if the show has been through a
     * restart since they last had a score); otherwise makes sure they don't have a stale row left
     * over from before.
     */
    public void refresh(Player player) {
        if (scoreboard == null || objective == null) {
            return;
        }
        if (contestantManager.islandOf(player).isPresent()) {
            player.setScoreboard(scoreboard);
            Score score = objective.getScore(player.getName());
            if (!score.isScoreSet()) {
                int initial = persisted.getOrDefault(player.getUniqueId(), configManager.startingLives());
                score.setScore(initial);
            }
        } else {
            remove(player);
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
        return updated;
    }

    /**
     * Clears a player's row and takes the shared board off their screen entirely - without this,
     * a removed contestant would keep seeing the board (with everyone else's rows) even though
     * their own row is gone, since it's one shared Scoreboard object handed out to every
     * contestant rather than one per viewer. Does not touch their persisted lives value - a
     * player quitting (or momentarily not being a contestant during a reassignment) should not
     * erase their progress; see {@link #forgetProgress} for genuine removal.
     */
    public void remove(Player player) {
        if (scoreboard == null) {
            return;
        }
        scoreboard.resetScores(player.getName());

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null && player.getScoreboard().equals(scoreboard)) {
            player.setScoreboard(manager.getMainScoreboard());
        }
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
