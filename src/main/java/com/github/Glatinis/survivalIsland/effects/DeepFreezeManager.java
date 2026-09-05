package com.github.Glatinis.survivalIsland.effects;

import com.github.Glatinis.survivalIsland.config.ConfigManager;
import com.github.Glatinis.survivalIsland.integration.WorldGuardHook;
import com.github.Glatinis.survivalIsland.util.Cuboid;
import com.github.Glatinis.survivalIsland.util.SchedulerUtil;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

/**
 * Deep Freeze: a real (non-visual-only) WorldEdit block conversion of every water block in the
 * configured area to ice, plus a global slowness effect, for a fixed duration or until turned off
 * early - reverted the same way (ice back to water) either way.
 */
public final class DeepFreezeManager {

    private static final int SLOWNESS_REFRESH_INTERVAL_TICKS = 100;
    private static final int SLOWNESS_DURATION_TICKS = 120;

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final WorldGuardHook worldGuardHook;

    private boolean active;
    private BukkitTask slownessTask;
    private BukkitTask autoOffTask;

    public DeepFreezeManager(JavaPlugin plugin, ConfigManager configManager, WorldGuardHook worldGuardHook) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.worldGuardHook = worldGuardHook;
    }

    public boolean isActive() {
        return active;
    }

    public void turnOn() {
        if (active) {
            return;
        }
        active = true;

        worldGuardHook.replaceBlocks(resolveArea(), BlockTypes.WATER, BlockTypes.ICE);

        int amplifier = configManager.deepFreezeSlownessAmplifier();
        applySlownessToAll(amplifier);
        slownessTask = SchedulerUtil.repeat(plugin, SLOWNESS_REFRESH_INTERVAL_TICKS, SLOWNESS_REFRESH_INTERVAL_TICKS,
            () -> applySlownessToAll(amplifier));

        autoOffTask = SchedulerUtil.later(plugin, configManager.deepFreezeDurationTicks(), this::turnOff);
    }

    public void turnOff() {
        if (!active) {
            return;
        }
        active = false;

        worldGuardHook.replaceBlocks(resolveArea(), BlockTypes.ICE, BlockTypes.WATER);

        if (slownessTask != null) {
            slownessTask.cancel();
            slownessTask = null;
        }
        if (autoOffTask != null) {
            autoOffTask.cancel();
            autoOffTask = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.removePotionEffect(PotionEffectType.SLOWNESS);
        }
    }

    private Cuboid resolveArea() {
        Cuboid arena = configManager.arenaCuboid();
        return worldGuardHook.regionCuboid(arena.world(), configManager.deepFreezeRegion()).orElse(arena);
    }

    private void applySlownessToAll(int amplifier) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, SLOWNESS_DURATION_TICKS, amplifier));
        }
    }
}
