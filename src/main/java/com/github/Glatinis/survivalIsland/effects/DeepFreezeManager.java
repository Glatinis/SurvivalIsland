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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

/**
 * Deep Freeze: a real (non-visual-only) WorldEdit block conversion of every water block in the
 * configured area to ice, plus a global slowness effect, for a fixed duration or until turned off
 * early - reverted the same way (ice back to water) either way. While active, every contestant
 * also carries an unlimited "Deepfreeze!" snowball (see {@link #restockHand}), taken away again
 * the moment it turns off.
 */
public final class DeepFreezeManager {

    private static final int SLOWNESS_REFRESH_INTERVAL_TICKS = 100;
    private static final int SLOWNESS_DURATION_TICKS = 120;
    private static final int SNOWBALL_AMOUNT = 1;

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final WorldGuardHook worldGuardHook;
    private final ContestantManager contestantManager;
    private final NamespacedKey snowballKey;

    private boolean active;
    private BukkitTask slownessTask;
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

        worldGuardHook.replaceBlocks(resolveArea(), BlockTypes.WATER, BlockTypes.ICE);

        int amplifier = configManager.deepFreezeSlownessAmplifier();
        applySlownessToAll(amplifier);
        slownessTask = SchedulerUtil.repeat(plugin, SLOWNESS_REFRESH_INTERVAL_TICKS, SLOWNESS_REFRESH_INTERVAL_TICKS,
            () -> applySlownessToAll(amplifier));

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (contestantManager.islandOf(player).isPresent()) {
                player.getInventory().addItem(createSnowball());
            }
        }

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
            removeSnowballs(player);
        }
    }

    public boolean isDeepfreezeSnowball(ItemStack item) {
        if (item == null || item.getType() != Material.SNOWBALL) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(snowballKey, PersistentDataType.BYTE);
    }

    /**
     * Replaces whatever is left in the given hand with a fresh full stack - called a tick after
     * a Deepfreeze snowball is thrown, so it's effectively unlimited no matter how many are used.
     */
    public void restockHand(Player player, EquipmentSlot hand) {
        if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(createSnowball());
        } else {
            player.getInventory().setItemInMainHand(createSnowball());
        }
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
