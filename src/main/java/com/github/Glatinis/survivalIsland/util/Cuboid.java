package com.github.Glatinis.survivalIsland.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

/**
 * A simple axis-aligned box in a specific world. Used both for WorldGuard-region-derived
 * bounds (islands, safe tower) and for the config-defined dragon arena bound.
 */
public final class Cuboid {

    private final World world;
    private final double minX, minY, minZ;
    private final double maxX, maxY, maxZ;

    public Cuboid(World world, double x1, double y1, double z1, double x2, double y2, double z2) {
        this.world = world;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public World world() {
        return world;
    }

    public double minX() {
        return minX;
    }

    public double minY() {
        return minY;
    }

    public double minZ() {
        return minZ;
    }

    public double maxX() {
        return maxX;
    }

    public double maxY() {
        return maxY;
    }

    public double maxZ() {
        return maxZ;
    }

    public boolean contains(Location location) {
        if (location.getWorld() == null || !location.getWorld().equals(world)) {
            return false;
        }
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public Location clamp(Location location) {
        double x = Math.max(minX, Math.min(maxX, location.getX()));
        double y = Math.max(minY, Math.min(maxY, location.getY()));
        double z = Math.max(minZ, Math.min(maxZ, location.getZ()));
        return new Location(world, x, y, z, location.getYaw(), location.getPitch());
    }

    public BoundingBox toBoundingBox() {
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
