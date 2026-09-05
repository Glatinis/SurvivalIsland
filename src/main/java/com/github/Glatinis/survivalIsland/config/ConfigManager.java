package com.github.Glatinis.survivalIsland.config;

import com.github.Glatinis.survivalIsland.util.Cuboid;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Loads/reloads config.yml and exposes typed getters for every tunable used elsewhere in the
 * plugin, so nothing else touches raw {@link FileConfiguration} paths directly.
 */
public final class ConfigManager {

    private final JavaPlugin plugin;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
    }

    public void reload() {
        plugin.reloadConfig();
    }

    private FileConfiguration config() {
        return plugin.getConfig();
    }

    public int startingLives() {
        return config().getInt("lives.starting-lives", 20);
    }

    public String livesScoreboardTitleJson() {
        return config().getString("lives.scoreboard-title-json", "{\"text\":\"Lives\"}");
    }

    public List<String> commandModeTriggerCommands(String trigger) {
        ConfigurationSection section = config().getConfigurationSection("command-mode.triggers");
        if (section == null) {
            return List.of();
        }
        return section.getStringList(trigger);
    }

    public Set<String> commandModeTriggers() {
        ConfigurationSection section = config().getConfigurationSection("command-mode.triggers");
        return section == null ? Set.of() : section.getKeys(false);
    }

    public long acidRainDurationTicks() {
        return config().getLong("acid-rain.duration-ticks", 8000L);
    }

    public long acidRainTickInterval() {
        return config().getLong("acid-rain.tick-interval", 20L);
    }

    public double acidRainPlayerDamage() {
        return config().getDouble("acid-rain.player-damage", 1.0);
    }

    public double acidRainMobDamage() {
        return config().getDouble("acid-rain.mob-damage", 2.0);
    }

    public int acidRainPoisonAmplifier() {
        return config().getInt("acid-rain.poison-amplifier", 0);
    }

    public long acidRainPoisonDurationTicks() {
        return config().getLong("acid-rain.poison-duration-ticks", 8100L);
    }

    public long acidOceanTickInterval() {
        return config().getLong("acid-ocean.tick-interval", 10L);
    }

    public double acidOceanPlayerDamage() {
        return config().getDouble("acid-ocean.player-damage", 2.0);
    }

    public int acidOceanPoisonAmplifier() {
        return config().getInt("acid-ocean.poison-amplifier", 0);
    }

    public long acidOceanPoisonDurationTicks() {
        return config().getLong("acid-ocean.poison-duration-ticks", 60L);
    }

    public boolean acidOceanRestrictToIsland() {
        return config().getBoolean("acid-ocean.restrict-to-island", false);
    }

    public long deepFreezeDurationTicks() {
        return config().getLong("deep-freeze.duration-ticks", 18000L);
    }

    public String deepFreezeRegion() {
        return config().getString("deep-freeze.region", "arena");
    }

    public int deepFreezeSlownessAmplifier() {
        return config().getInt("deep-freeze.slowness-amplifier", 1);
    }

    public long containmentCheckIntervalTicks() {
        return config().getLong("containment.check-interval-ticks", 10L);
    }

    public Cuboid arenaCuboid() {
        String worldName = config().getString("containment.arena.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().log(Level.WARNING, "Arena world '" + worldName + "' is not loaded, falling back to the first loaded world.");
            world = Bukkit.getWorlds().get(0);
        }
        List<Integer> min = config().getIntegerList("containment.arena.min");
        List<Integer> max = config().getIntegerList("containment.arena.max");
        if (min.size() < 3 || max.size() < 3) {
            return new Cuboid(world, -500, 0, -500, 500, 256, 500);
        }
        return new Cuboid(world, min.get(0), min.get(1), min.get(2), max.get(0), max.get(1), max.get(2));
    }

    public String safeTowerRegion() {
        return config().getString("containment.safe-tower-region", "safe-tower");
    }

    public List<String> islands() {
        return config().getStringList("islands");
    }

    public String defaultProtectionMode() {
        return config().getString("protection.default-mode", "LOCKED");
    }

    public Set<CreatureSpawnEvent.SpawnReason> blockedNaturalSpawnReasons() {
        Set<CreatureSpawnEvent.SpawnReason> reasons = EnumSet.noneOf(CreatureSpawnEvent.SpawnReason.class);
        for (String name : config().getStringList("natural-spawn-block-reasons")) {
            try {
                reasons.add(CreatureSpawnEvent.SpawnReason.valueOf(name));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Unknown spawn reason in config: " + name);
            }
        }
        return reasons;
    }

    public boolean pvpDefaultEnabled() {
        return config().getBoolean("pvp.default-enabled", true);
    }

    public boolean theftDefaultEnabled() {
        return config().getBoolean("theft.default-enabled", false);
    }
}
