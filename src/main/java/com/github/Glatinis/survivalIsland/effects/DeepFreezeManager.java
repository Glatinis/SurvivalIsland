package com.github.Glatinis.survivalIsland.effects;

import com.github.Glatinis.survivalIsland.config.ConfigManager;
import com.github.Glatinis.survivalIsland.contestant.ContestantManager;
import com.github.Glatinis.survivalIsland.integration.WorldGuardHook;
import com.github.Glatinis.survivalIsland.util.Cuboid;
import com.github.Glatinis.survivalIsland.util.SchedulerUtil;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;

/**
 * Deep Freeze: a real (non-visual-only) WorldEdit block conversion of every water block in the
 * configured area to ice, plus a global slowness effect and falling-snow particles over the same
 * area, for a fixed duration or until turned off early - all reverted the same way either way.
 * While active, every contestant also carries a "Deepfreeze!" snowball that never runs out.
 */
public final class DeepFreezeManager {

    private static final int SLOWNESS_REFRESH_INTERVAL_TICKS = 100;
    private static final int SLOWNESS_DURATION_TICKS = 120;
    private static final int SNOWBALL_AMOUNT = 1;
    private static final int SNOWBALL_CHECK_INTERVAL_TICKS = 20;
    private static final int SNOW_PARTICLE_INTERVAL_TICKS = 5;
    private static final int SNOW_PARTICLES_PER_CYCLE = 30;
    private static final double SNOW_PARTICLE_HEIGHT_ABOVE_AREA = 3.0;

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final WorldGuardHook worldGuardHook;
    private final ContestantManager contestantManager;
    private final NamespacedKey snowballKey;
    private final Random random = new Random();

    private boolean active;
    private BukkitTask slownessTask;
    private BukkitTask snowballTask;
    private BukkitTask snowParticleTask;
    private BukkitTask autoOffTask;

    public DeepFreezeManager(JavaPlugin plugin, ConfigManager configManager, WorldGuardHook worldGuardHook,
                              ContestantManager contestantManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.worldGuardHook = worldGuardHook;
        this.contestantManager = contestantManager;
        this.snowballKey = new NamespacedKey(plugin, "deepfreeze_snowball");
    }

    public boolean isActive() {
        return active;
    }

    public void turnOn() {
        if (active) {
            return;
        }
        active = true;

        Cuboid area = resolveArea();
        worldGuardHook.replaceBlocks(area, BlockTypes.WATER, BlockTypes.ICE);

        int amplifier = configManager.deepFreezeSlownessAmplifier();
        applySlownessToAll(amplifier);
        slownessTask = SchedulerUtil.repeat(plugin, SLOWNESS_REFRESH_INTERVAL_TICKS, SLOWNESS_REFRESH_INTERVAL_TICKS,
            () -> applySlownessToAll(amplifier));

        // Every contestant is topped back up to one Deepfreeze snowball on a short timer rather
        // than reacting to each individual throw - simpler, and self-healing regardless of why a
        // single throw's item might otherwise be lost (a cancelled event, a dropped item, etc.).
        ensureSnowballs();
        snowballTask = SchedulerUtil.repeat(plugin, SNOWBALL_CHECK_INTERVAL_TICKS, SNOWBALL_CHECK_INTERVAL_TICKS,
            this::ensureSnowballs);

        snowParticleTask = SchedulerUtil.repeat(plugin, SNOW_PARTICLE_INTERVAL_TICKS, SNOW_PARTICLE_INTERVAL_TICKS,
            () -> spawnSnowParticles(area));

        autoOffTask = SchedulerUtil.later(plugin, configManager.deepFreezeDurationTicks(), this::turnOff);
    }

    public void turnOff() {
        if (!active) {
            return;
        }
        active = false;

        worldGuardHook.replaceBlocks(resolveArea(), BlockTypes.ICE, BlockTypes.WATER);

        cancel(slownessTask);
        slownessTask = null;
        cancel(snowballTask);
        snowballTask = null;
        cancel(snowParticleTask);
        snowParticleTask = null;
        cancel(autoOffTask);
        autoOffTask = null;

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            removeSnowballs(player);
        }
    }

    private void cancel(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }

    private void ensureSnowballs() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (contestantManager.islandOf(player).isEmpty()) {
                continue;
            }
            if (!hasSnowball(player)) {
                player.getInventory().addItem(createSnowball());
            }
        }
    }

    private boolean hasSnowball(Player player) {
        if (isDeepfreezeSnowball(player.getInventory().getItemInOffHand())) {
            return true;
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (isDeepfreezeSnowball(item)) {
                return true;
            }
        }
        return false;
    }

    private void spawnSnowParticles(Cuboid area) {
        World world = area.world();
        double spanX = area.maxX() - area.minX();
        double spanZ = area.maxZ() - area.minZ();
        double y = area.maxY() + SNOW_PARTICLE_HEIGHT_ABOVE_AREA;
        for (int i = 0; i < SNOW_PARTICLES_PER_CYCLE; i++) {
            double x = area.minX() + random.nextDouble() * spanX;
            double z = area.minZ() + random.nextDouble() * spanZ;
            world.spawnParticle(Particle.SNOWFLAKE, x, y, z, 1, 0.0, 0.0, 0.0, 0.05);
        }
    }

    private boolean isDeepfreezeSnowball(ItemStack item) {
        if (item == null || item.getType() != Material.SNOWBALL) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(snowballKey, PersistentDataType.BYTE);
    }

    private ItemStack createSnowball() {
        ItemStack item = new ItemStack(Material.SNOWBALL, SNOWBALL_AMOUNT);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Deepfreeze!", NamedTextColor.BLUE, TextDecoration.BOLD));
        // a nonsense enchant just to make it glow - the level/category restriction is deliberately
        // ignored, and the enchant itself is hidden from the tooltip.
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(snowballKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private void removeSnowballs(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isDeepfreezeSnowball(contents[i])) {
                player.getInventory().setItem(i, null);
            }
        }
        if (isDeepfreezeSnowball(player.getInventory().getItemInOffHand())) {
            player.getInventory().setItemInOffHand(null);
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
