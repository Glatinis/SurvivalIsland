package com.github.Glatinis.survivalIsland.effects;

import com.github.Glatinis.survivalIsland.config.ConfigManager;
import com.github.Glatinis.survivalIsland.contestant.ContestantManager;
import com.github.Glatinis.survivalIsland.integration.WorldGuardHook;
import com.github.Glatinis.survivalIsland.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * "Kitten UwU" gift effect: while active for a player, standing in water deals lava-like periodic
 * damage plus poison - deliberately never touches fire ticks, so there's no burning visual.
 */
public final class AcidOceanManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final ContestantManager contestantManager;
    private final WorldGuardHook worldGuardHook;

    private final Set<UUID> active = new HashSet<>();
    private BukkitTask task;

    public AcidOceanManager(JavaPlugin plugin, ConfigManager configManager,
                             ContestantManager contestantManager, WorldGuardHook worldGuardHook) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.contestantManager = contestantManager;
        this.worldGuardHook = worldGuardHook;
    }

    public void start() {
        long interval = configManager.acidOceanTickInterval();
        task = SchedulerUtil.repeat(plugin, interval, interval, this::tick);
    }

    public void stopTask() {
        if (task != null) {
            task.cancel();
        }
    }

    public void enable(Player player) {
        active.add(player.getUniqueId());
    }

    public void disable(Player player) {
        active.remove(player.getUniqueId());
        player.removePotionEffect(PotionEffectType.POISON);
    }

    private void tick() {
        active.removeIf(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                return true;
            }
            if (isSubmerged(player)) {
                player.damage(configManager.acidOceanPlayerDamage());
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON,
                    (int) configManager.acidOceanPoisonDurationTicks(), configManager.acidOceanPoisonAmplifier()));
            }
            return false;
        });
    }

    private boolean isSubmerged(Player player) {
        if (!player.isInWater()) {
            return false;
        }
        if (!configManager.acidOceanRestrictToIsland()) {
            return true;
        }
        return contestantManager.islandOf(player)
            .map(islandId -> worldGuardHook.regionIdAt(player.getLocation(), List.of(islandId)).isPresent())
            .orElse(false);
    }
}
