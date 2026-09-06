package com.github.Glatinis.survivalIsland.containment;

import com.github.Glatinis.survivalIsland.config.ConfigManager;
import com.github.Glatinis.survivalIsland.integration.WorldGuardHook;
import com.github.Glatinis.survivalIsland.util.Cuboid;
import com.github.Glatinis.survivalIsland.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Generic "this entity must stay inside this Cuboid" engine, reused for per-island mob leashing,
 * the targeted spawn command, and the ender dragon arena bound. Each tracked entity is PDC-tagged
 * with a bound id (an island id, or {@link #ARENA_BOUND_ID}); one shared repeating task checks
 * tracked entities against their bound's cached Cuboid and teleports them back if they stray.
 */
public final class EntityBoundsGuard {

    public static final String ARENA_BOUND_ID = "__arena__";

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final WorldGuardHook worldGuardHook;
    private final NamespacedKey boundKey;

    private final Set<UUID> tracked = new HashSet<>();
    private final Map<String, Cuboid> boundCache = new HashMap<>();
    private BukkitTask task;

    public EntityBoundsGuard(JavaPlugin plugin, ConfigManager configManager, WorldGuardHook worldGuardHook) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.worldGuardHook = worldGuardHook;
        this.boundKey = new NamespacedKey(plugin, "bound_id");
    }

    public void start() {
        long interval = configManager.containmentCheckIntervalTicks();
        task = SchedulerUtil.repeat(plugin, interval, interval, this::sweep);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Drops every cached region-to-Cuboid resolution, so the next containment check re-resolves
     * bounds fresh from WorldGuard/config - needed after a {@code /survivalisland reload}, since
     * bounds are otherwise cached forever once first resolved.
     */
    public void clearBoundCache() {
        boundCache.clear();
    }

    /**
     * Tags the entity with the given bound id and starts tracking it against that bound.
     */
    public void track(Entity entity, String boundId) {
        entity.getPersistentDataContainer().set(boundKey, PersistentDataType.STRING, boundId);
        tracked.add(entity.getUniqueId());
    }

    public Optional<String> boundIdOf(Entity entity) {
        return Optional.ofNullable(entity.getPersistentDataContainer().get(boundKey, PersistentDataType.STRING));
    }

    /**
     * Checks a candidate location against the entity's own bound (if it has one) without
     * teleporting anything - used to proactively cancel AI pathing that would leave the bound,
     * so the periodic sweep rarely has to visibly correct anything.
     */
    public boolean isWithinOwnBound(Entity entity, Location candidate) {
        return boundIdOf(entity)
            .flatMap(boundId -> boundCuboid(boundId, entity.getWorld()))
            .map(cuboid -> cuboid.contains(candidate))
            .orElse(true);
    }

    /**
     * Resolves the actual containment box for a bound id. For an island bound, this prefers the
     * island's tighter land-only region (see {@link ConfigManager#landRegionFor}) over the full
     * island region if one is configured, so leashed mobs stay off the water even when the full
     * island region includes some shoreline/ocean buffer. Falls back to the island's own region
     * if no land region is configured for it.
     */
    private Optional<Cuboid> boundCuboid(String boundId, World world) {
        Cuboid cached = boundCache.get(boundId);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<Cuboid> resolved;
        if (ARENA_BOUND_ID.equals(boundId)) {
            resolved = Optional.of(configManager.arenaCuboid());
        } else {
            String regionId = configManager.landRegionFor(boundId).orElse(boundId);
            resolved = worldGuardHook.regionCuboid(world, regionId);
        }
        resolved.ifPresent(cuboid -> boundCache.put(boundId, cuboid));
        return resolved;
    }

    private void sweep() {
        tracked.removeIf(uuid -> {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity == null || !entity.isValid()) {
                return true;
            }
            enforce(entity);
            return false;
        });
    }

    private void enforce(Entity entity) {
        String boundId = boundIdOf(entity).orElse(null);
        if (boundId == null) {
            return;
        }
        Cuboid bounds = boundCuboid(boundId, entity.getWorld()).orElse(null);
        if (bounds == null) {
            return;
        }
        Location location = entity.getLocation();
        if (!bounds.contains(location)) {
            entity.teleport(bounds.clamp(location));
        }
    }
}
