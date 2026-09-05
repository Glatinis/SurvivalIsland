package com.github.Glatinis.survivalIsland.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Small wrapper around the Bukkit scheduler so every repeating task in the plugin goes through
 * one place - keeps interval-based tasks (containment sweeps, effect ticks) consistent and easy
 * to find.
 */
public final class SchedulerUtil {

    private SchedulerUtil() {
    }

    public static BukkitTask repeat(Plugin plugin, long delayTicks, long periodTicks, Runnable task) {
        return Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
    }

    public static BukkitTask later(Plugin plugin, long delayTicks, Runnable task) {
        return Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    public static void sync(Plugin plugin, Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }
}
