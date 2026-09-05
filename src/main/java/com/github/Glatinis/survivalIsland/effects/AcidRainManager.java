package com.github.Glatinis.survivalIsland.effects;

import com.github.Glatinis.survivalIsland.config.ConfigManager;
import com.github.Glatinis.survivalIsland.contestant.ContestantManager;
import com.github.Glatinis.survivalIsland.integration.WorldGuardHook;
import com.github.Glatinis.survivalIsland.util.SchedulerUtil;
import org.bukkit.WeatherType;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "Dancing Ryan" gift effect: a personal client-side rainstorm (no ProtocolLib needed - Paper's
 * per-player weather API is purely visual/client-only) that periodically damages the target and
 * any passive/tameable mob on their island, plus a poison effect on the player.
 */
public final class AcidRainManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final ContestantManager contestantManager;
    private final WorldGuardHook worldGuardHook;

    private final Map<UUID, BukkitTask> tickTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> autoStopTasks = new HashMap<>();

    public AcidRainManager(JavaPlugin plugin, ConfigManager configManager,
                            ContestantManager contestantManager, WorldGuardHook worldGuardHook) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.contestantManager = contestantManager;
        this.worldGuardHook = worldGuardHook;
    }

    public void start(Player player) {
        stop(player);

        player.setPlayerWeather(WeatherType.DOWNFALL);
        applyPoison(player);

        long interval = configManager.acidRainTickInterval();
        tickTasks.put(player.getUniqueId(), SchedulerUtil.repeat(plugin, interval, interval, () -> tick(player)));

        long duration = configManager.acidRainDurationTicks();
        autoStopTasks.put(player.getUniqueId(), SchedulerUtil.later(plugin, duration, () -> stop(player)));
    }

    public void stop(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitTask tickTask = tickTasks.remove(uuid);
        if (tickTask != null) {
            tickTask.cancel();
        }
        BukkitTask autoStop = autoStopTasks.remove(uuid);
        if (autoStop != null) {
            autoStop.cancel();
        }
        player.resetPlayerWeather();
        player.removePotionEffect(PotionEffectType.POISON);
    }

    private void tick(Player player) {
        if (!player.isOnline()) {
            stop(player);
            return;
        }
        player.damage(configManager.acidRainPlayerDamage());
        applyPoison(player);
        damageIslandAnimals(player);
    }

    private void applyPoison(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON,
            (int) configManager.acidRainPoisonDurationTicks(), configManager.acidRainPoisonAmplifier()));
    }

    private void damageIslandAnimals(Player player) {
        contestantManager.islandOf(player)
            .flatMap(islandId -> worldGuardHook.regionCuboid(player.getWorld(), islandId))
            .ifPresent(cuboid -> {
                for (Entity entity : player.getWorld().getNearbyEntities(cuboid.toBoundingBox())) {
                    if (entity instanceof Animals animal) {
                        animal.damage(configManager.acidRainMobDamage());
                    }
                }
            });
    }
}
