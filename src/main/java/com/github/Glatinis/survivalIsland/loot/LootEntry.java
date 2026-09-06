package com.github.Glatinis.survivalIsland.loot;

import org.bukkit.Material;
import org.bukkit.potion.PotionType;

/**
 * One weighted entry in a restocking chest's loot pool. {@code potionType} is only meaningful
 * for potion-holding materials ({@code POTION}, {@code SPLASH_POTION}, {@code LINGERING_POTION})
 * and is {@code null} otherwise.
 */
public record LootEntry(Material material, int amount, int weight, PotionType potionType) {

    public LootEntry {
        if (weight <= 0) {
            throw new IllegalArgumentException("Loot entry weight must be positive: " + material);
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Loot entry amount must be positive: " + material);
        }
    }
}
