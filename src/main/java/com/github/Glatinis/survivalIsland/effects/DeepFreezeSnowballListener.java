package com.github.Glatinis.survivalIsland.effects;

import com.github.Glatinis.survivalIsland.util.SchedulerUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Makes the Deep Freeze snowball effectively unlimited: a tick after one is thrown (letting
 * Minecraft's own stack decrement happen first), the hand that threw it is refilled with a fresh
 * full stack.
 */
public final class DeepFreezeSnowballListener implements Listener {

    private final DeepFreezeManager deepFreezeManager;
    private final JavaPlugin plugin;

    public DeepFreezeSnowballListener(DeepFreezeManager deepFreezeManager, JavaPlugin plugin) {
        this.deepFreezeManager = deepFreezeManager;
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (!deepFreezeManager.isDeepfreezeSnowball(item)) {
            return;
        }

        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        SchedulerUtil.later(plugin, 1L, () -> {
            if (deepFreezeManager.isActive()) {
                deepFreezeManager.restockHand(player, hand);
            }
        });
    }
}
