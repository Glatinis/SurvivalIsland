package com.github.Glatinis.survivalIsland.lives;

import com.github.Glatinis.survivalIsland.config.ConfigManager;
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
 * Owns the sidebar "lives" objective. The Score itself is the only store of truth for a
 * player's lives - there is deliberately no separate in-memory map, so an admin can also just
 * run the vanilla {@code /scoreboard players set <player> lives <value>} command and it works.
 */
public final class LivesScoreboardService {

    private static final String OBJECTIVE_NAME = "lives";

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private Scoreboard scoreboard;
    private Objective objective;

    public LivesScoreboardService(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
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

    public void applyTo(Player player) {
        if (scoreboard == null || objective == null) {
            return;
        }
        player.setScoreboard(scoreboard);
        Score score = objective.getScore(player.getName());
        if (!score.isScoreSet()) {
            score.setScore(configManager.startingLives());
        }
    }

    public void decrement(Player player) {
        if (objective == null) {
            return;
        }
        Score score = objective.getScore(player.getName());
        score.setScore(score.getScore() - 1);
    }

    public void remove(Player player) {
        if (scoreboard == null) {
            return;
        }
        scoreboard.resetScores(player.getName());
    }
}
