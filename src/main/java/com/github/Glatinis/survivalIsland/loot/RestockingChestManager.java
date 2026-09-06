package com.github.Glatinis.survivalIsland.loot;

import com.github.Glatinis.survivalIsland.config.ConfigManager;
import com.github.Glatinis.survivalIsland.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

/**
 * Tracks chests marked to periodically clear and refill themselves from the configured loot
 * pool - the "X marks the spot" chests that restock every few minutes rather than being a
 * one-time loot source. Restock timers run per-chest on the Bukkit scheduler and reset (rather
 * than persist to the exact tick) across a server restart, which is an accepted simplification.
 */
public final class RestockingChestManager {

    private record ChestKey(String world, int x, int y, int z) {
    }

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final File file;
    private final Random random = new Random();

    private final Map<ChestKey, BukkitTask> tasks = new LinkedHashMap<>();
    private final Map<ChestKey, Long> intervalOverrides = new LinkedHashMap<>();
    private final Map<ChestKey, Integer> itemCountOverrides = new LinkedHashMap<>();

    public RestockingChestManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.file = new File(plugin.getDataFolder(), "restocking-chests.yml");
    }

    public void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (Object raw : config.getList("chests", List.of())) {
            if (!(raw instanceof Map<?, ?> map)) {
                continue;
            }
            ChestKey key = new ChestKey(
                String.valueOf(map.get("world")),
                asInt(map.get("x")),
                asInt(map.get("y")),
                asInt(map.get("z"))
            );
            if (map.get("interval-ticks") != null) {
                intervalOverrides.put(key, asLong(map.get("interval-ticks")));
            }
            if (map.get("item-count") != null) {
                itemCountOverrides.put(key, asInt(map.get("item-count")));
            }
            scheduleChest(key);
        }
    }

    public void stop() {
        tasks.values().forEach(BukkitTask::cancel);
        tasks.clear();
    }

    /**
     * Marks the given chest block as restocking. Returns false if the block isn't a chest or is
     * already marked.
     */
    public boolean mark(Block block, Long intervalTicksOverride, Integer itemCountOverride) {
        if (!(block.getState() instanceof Chest)) {
            return false;
        }
        ChestKey key = keyOf(block);
        if (tasks.containsKey(key)) {
            return false;
        }
        if (intervalTicksOverride != null) {
            intervalOverrides.put(key, intervalTicksOverride);
        }
        if (itemCountOverride != null) {
            itemCountOverrides.put(key, itemCountOverride);
        }
        scheduleChest(key);
        save();
        return true;
    }

    public boolean unmark(Block block) {
        return unmark(keyOf(block));
    }

    private boolean unmark(ChestKey key) {
        BukkitTask task = tasks.remove(key);
        if (task == null) {
            return false;
        }
        task.cancel();
        intervalOverrides.remove(key);
        itemCountOverrides.remove(key);
        save();
        return true;
    }

    public boolean restockNow(Block block) {
        ChestKey key = keyOf(block);
        if (!tasks.containsKey(key)) {
            return false;
        }
        restock(key);
        return true;
    }

    public int count() {
        return tasks.size();
    }

    private void scheduleChest(ChestKey key) {
        long interval = intervalOverrides.getOrDefault(key, configManager.restockDefaultIntervalTicks());
        tasks.put(key, SchedulerUtil.repeat(plugin, interval, interval, () -> restock(key)));
    }

    private void restock(ChestKey key) {
        World world = Bukkit.getWorld(key.world());
        if (world == null) {
            return;
        }

        Block block = world.getBlockAt(key.x(), key.y(), key.z());
        if (!(block.getState() instanceof Chest chest)) {
            // the chest was broken or replaced since being marked - stop tracking it.
            unmark(key);
            return;
        }

        List<LootEntry> pool = configManager.lootPool();
        if (pool.isEmpty()) {
            return;
        }

        Inventory inventory = chest.getInventory();
        inventory.clear();

        int itemCount = itemCountOverrides.getOrDefault(key, configManager.restockDefaultItemCount());
        int totalWeight = pool.stream().mapToInt(LootEntry::weight).sum();
        for (int i = 0; i < itemCount; i++) {
            inventory.addItem(roll(pool, totalWeight));
        }
    }

    private ItemStack roll(List<LootEntry> pool, int totalWeight) {
        int target = random.nextInt(totalWeight);
        int cumulative = 0;
        LootEntry chosen = pool.get(pool.size() - 1);
        for (LootEntry entry : pool) {
            cumulative += entry.weight();
            if (target < cumulative) {
                chosen = entry;
                break;
            }
        }

        ItemStack item = new ItemStack(chosen.material(), chosen.amount());
        if (chosen.potionType() != null && item.getItemMeta() instanceof PotionMeta potionMeta) {
            potionMeta.setBasePotionType(chosen.potionType());
            item.setItemMeta(potionMeta);
        }
        return item;
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();
        for (ChestKey key : tasks.keySet()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("world", key.world());
            entry.put("x", key.x());
            entry.put("y", key.y());
            entry.put("z", key.z());
            if (intervalOverrides.containsKey(key)) {
                entry.put("interval-ticks", intervalOverrides.get(key));
            }
            if (itemCountOverrides.containsKey(key)) {
                entry.put("item-count", itemCountOverrides.get(key));
            }
            list.add(entry);
        }
        config.set("chests", list);
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save restocking-chests.yml", e);
        }
    }

    private ChestKey keyOf(Block block) {
        return new ChestKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    private int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}
