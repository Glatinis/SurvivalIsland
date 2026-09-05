package com.github.Glatinis.survivalIsland.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

/**
 * Permission constants and consistent player-facing message formatting, so every command reports
 * success/failure the same way.
 */
public final class Messages {

    public static final String ADMIN_PERMISSION = "survivalisland.admin";
    public static final String COMMAND_MODE_PERMISSION = "survivalisland.commandmode";

    private Messages() {
    }

    public static void info(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.AQUA));
    }

    public static void success(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.GREEN));
    }

    public static void error(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.RED));
    }
}
