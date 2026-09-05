package com.github.Glatinis.survivalIsland.command;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * One category of the consolidated {@code /survivalisland <category> ...} command
 * (commandmode, event, rule, spawn, contestant). {@link SurvivalIslandCommand} routes to
 * whichever of these matches the first argument.
 */
public interface SubCommand {

    /**
     * The category name, e.g. "event" - matched against the first argument, case-insensitive.
     */
    String name();

    /**
     * Permission required to use this category at all. Individual actions within a category
     * may enforce the same or a stricter permission themselves.
     */
    String permission();

    /**
     * @param args the arguments after the category name itself, e.g. for
     *             {@code /survivalisland event acidrain Steve start} this is
     *             {@code ["acidrain", "Steve", "start"]}.
     */
    void execute(CommandSender sender, String[] args);

    /**
     * @param args same slicing as {@link #execute}, for the argument currently being typed.
     */
    List<String> tabComplete(CommandSender sender, String[] args);
}
