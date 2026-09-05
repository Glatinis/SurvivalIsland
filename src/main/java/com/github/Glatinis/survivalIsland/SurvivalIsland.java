package com.github.Glatinis.survivalIsland;

import com.github.Glatinis.survivalIsland.command.SurvivalIslandCommand;
import com.github.Glatinis.survivalIsland.config.ConfigManager;
import com.github.Glatinis.survivalIsland.contestant.ContestantManager;
import com.github.Glatinis.survivalIsland.contestant.ContestantSubCommand;
import com.github.Glatinis.survivalIsland.integration.WorldGuardHook;
import org.bukkit.plugin.java.JavaPlugin;

public final class SurvivalIsland extends JavaPlugin {

    private ConfigManager configManager;
    private WorldGuardHook worldGuardHook;
    private ContestantManager contestantManager;
    private SurvivalIslandCommand rootCommand;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.load();

        worldGuardHook = new WorldGuardHook(this);

        contestantManager = new ContestantManager(this);
        contestantManager.load();

        rootCommand = new SurvivalIslandCommand();
        rootCommand.register(new ContestantSubCommand(contestantManager, configManager));
        getCommand("survivalisland").setExecutor(rootCommand);
        getCommand("survivalisland").setTabCompleter(rootCommand);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public ConfigManager configManager() {
        return configManager;
    }

    public WorldGuardHook worldGuardHook() {
        return worldGuardHook;
    }

    public ContestantManager contestantManager() {
        return contestantManager;
    }
}
