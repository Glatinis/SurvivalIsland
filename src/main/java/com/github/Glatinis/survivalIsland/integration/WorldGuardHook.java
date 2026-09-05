package com.github.Glatinis.survivalIsland.integration;

import com.github.Glatinis.survivalIsland.util.Cuboid;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Thin wrapper around WorldGuard/WorldEdit so the rest of the plugin never touches their APIs
 * directly. Deliberately narrow: we only ever resolve a small fixed set of named regions
 * (island1/island2/island3, safe-tower, the deep-freeze/arena region), never WorldGuard's full
 * priority-based applicable-region resolution, since that's all the show needs.
 */
public final class WorldGuardHook {

    private final JavaPlugin plugin;

    public WorldGuardHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private RegionManager regionManager(World world) {
        return WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
    }

    public Optional<ProtectedRegion> regionById(World world, String id) {
        RegionManager manager = regionManager(world);
        if (manager == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(manager.getRegion(id));
    }

    /**
     * Resolves which of the given candidate region ids a location falls in (first match wins).
     */
    public Optional<String> regionIdAt(Location location, List<String> candidateIds) {
        World world = location.getWorld();
        if (world == null) {
            return Optional.empty();
        }
        RegionManager manager = regionManager(world);
        if (manager == null) {
            return Optional.empty();
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        for (String id : candidateIds) {
            ProtectedRegion region = manager.getRegion(id);
            if (region != null && region.contains(x, y, z)) {
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }

    public Optional<Cuboid> regionCuboid(World world, String id) {
        return regionById(world, id).map(region -> toCuboid(world, region));
    }

    private Cuboid toCuboid(World world, ProtectedRegion region) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        return new Cuboid(world, min.x(), min.y(), min.z(), max.x(), max.y(), max.z());
    }

    public void setFlag(World world, String regionId, StateFlag flag, StateFlag.State state) {
        regionById(world, regionId).ifPresent(region -> region.setFlag(flag, state));
    }

    public void setFlagOnAll(World world, List<String> regionIds, StateFlag flag, StateFlag.State state) {
        for (String id : regionIds) {
            setFlag(world, id, flag, state);
        }
    }

    /**
     * Replaces every block of type {@code from} with {@code to} inside the given area in one
     * WorldEdit EditSession - the real (non-visual-only) block conversion Deep Freeze needs, in
     * both directions (water -> ice to start, ice -> water to revert).
     */
    public void replaceBlocks(Cuboid area, BlockType from, BlockType to) {
        World bukkitWorld = area.world();
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);
        BlockVector3 min = BlockVector3.at(area.minX(), area.minY(), area.minZ());
        BlockVector3 max = BlockVector3.at(area.maxX(), area.maxY(), area.maxZ());
        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                .world(weWorld)
                .maxBlocks(-1)
                .build()) {
            CuboidRegion region = new CuboidRegion(weWorld, min, max);
            editSession.replaceBlocks(region, new BlockTypeMask(editSession, from), to.getDefaultState());
        } catch (MaxChangedBlocksException e) {
            plugin.getLogger().log(Level.WARNING, "Deep freeze block edit exceeded the change limit", e);
        }
    }
}
