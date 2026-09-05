package com.github.Glatinis.survivalIsland.commandmode;

import com.github.Glatinis.survivalIsland.util.Messages;
import com.github.Glatinis.survivalIsland.util.SchedulerUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;

/**
 * While a player has command mode active, their chat messages are intercepted (never broadcast)
 * and matched against the configured trigger map instead - lets Rcian fire a whole command-block
 * chain by typing a short code rather than manually pulling levers.
 */
public final class CommandModeChatListener implements Listener {

    private final CommandModeManager commandModeManager;
    private final JavaPlugin plugin;

    public CommandModeChatListener(CommandModeManager commandModeManager, JavaPlugin plugin) {
        this.commandModeManager = commandModeManager;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!commandModeManager.isActive(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);

        String trigger = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Optional<List<String>> commands = commandModeManager.resolve(trigger);

        // Chat events fire off the main thread; command dispatch must happen back on it.
        SchedulerUtil.sync(plugin, () -> {
            if (commands.isEmpty()) {
                Messages.error(player, "Unknown trigger: " + trigger);
                return;
            }
            for (String command : commands.get()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
            }
            Messages.success(player, "Ran command set: " + trigger);
        });
    }
}
