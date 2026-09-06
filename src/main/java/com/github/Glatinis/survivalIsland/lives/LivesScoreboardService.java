package com.github.Glatinis.survivalIsland.lives;

import com.github.Glatinis.survivalIsland.config.ConfigManager;
import com.github.Glatinis.survivalIsland.contestant.ContestantManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

/**
 * Owns the sidebar "lives" objective on a private scoreboard the plugin manages itself, not the
 * server's shared main scoreboard, so it never shows in {@code /scoreboard objectives list} and
 * vanilla {@code /scoreboard players} commands can't read or change it. Only assigned contestants
 * ever get a row on it; the Score itself is still the only store of truth for a contestant's
 * lives (no separate in-memory map), it's just only reachable through
 * {@code /survivalisland lives} rather than the vanilla command.
 */
public final class LivesScoreboardService {

    private static final String OBJECTIVE_NAME = "lives";

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final ContestantManager contestantManager;
    private Scoreboard scoreboard;
    private Objective objective;

    public LivesScoreboardService(JavaPlugin plugin, ConfigManager configManager, ContestantManager contestantManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.contestantManager = contestantManager;
    }

    public void setup() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            plugin.getLogger().warning("No ScoreboardManager available - lives scoreboard disabled.");
            return;
        }
        scoreboard = manager.getNewScoreboard();
        Component title = GsonComponentSerializer.gson().deserialize(configManager.livesScoreboardTitleJson());
        objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    /**
     * Shows the board and initializes a score for the player if they're an assigned contestant;
     * otherwise makes sure they don't have a stale row left over from before.
     */
    public void refresh(Player player) {
        if (scoreboard == null || objective == null) {
            return;
        }
        if (contestantManager.islandOf(player).isPresent()) {
            player.setScoreboard(scoreboard);
            Score score = objective.getScore(player.getName());
            if (!score.isScoreSet()) {
                score.setScore(configManager.startingLives());
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
        score.setScore(Math.max(0, score.getScore() - 1));
    }

    /**
     * Sets a contestant's lives to an absolute value (clamped to 0 or above). Returns the value
     * actually applied.
     */
    public int setLives(Player player, int value) {
        int clamped = Math.max(0, value);
        if (objective != null) {
            objective.getScore(player.getName()).setScore(clamped);
        }
        return clamped;
    }

    /**
     * Adds (or, with a negative delta, subtracts) from a contestant's current lives, clamped to 0
     * or above. Returns the resulting value.
     */
    public int addLives(Player player, int delta) {
        if (objective == null) {
            return 0;
        }
        Score score = objective.getScore(player.getName());
        int current = score.isScoreSet() ? score.getScore() : configManager.startingLives();
        int updated = Math.max(0, current + delta);
        score.setScore(updated);
        return updated;
    }

    public void remove(Player player) {
        if (scoreboard == null) {
            return;
        }
        scoreboard.resetScores(player.getName());
    }
}
