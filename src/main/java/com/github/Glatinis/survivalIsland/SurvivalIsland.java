package com.github.Glatinis.survivalIsland;

import com.github.Glatinis.survivalIsland.command.SurvivalIslandCommand;
import com.github.Glatinis.survivalIsland.commandmode.CommandModeChatListener;
import com.github.Glatinis.survivalIsland.commandmode.CommandModeManager;
import com.github.Glatinis.survivalIsland.commandmode.CommandModeSubCommand;
import com.github.Glatinis.survivalIsland.config.ConfigManager;
import com.github.Glatinis.survivalIsland.containment.EntityBoundsGuard;
import com.github.Glatinis.survivalIsland.containment.MobLeashListener;
import com.github.Glatinis.survivalIsland.containment.NaturalSpawnListener;
import com.github.Glatinis.survivalIsland.containment.SpawnSubCommand;
import com.github.Glatinis.survivalIsland.contestant.ContestantManager;
import com.github.Glatinis.survivalIsland.contestant.ContestantSubCommand;
import com.github.Glatinis.survivalIsland.effects.AcidOceanManager;
import com.github.Glatinis.survivalIsland.effects.AcidRainManager;
import com.github.Glatinis.survivalIsland.effects.DeepFreezeManager;
import com.github.Glatinis.survivalIsland.effects.DeepFreezeSnowballListener;
import com.github.Glatinis.survivalIsland.effects.EventSubCommand;
import com.github.Glatinis.survivalIsland.integration.WorldGuardHook;
import com.github.Glatinis.survivalIsland.lives.LivesScoreboardService;
import com.github.Glatinis.survivalIsland.lives.LivesSubCommand;
import com.github.Glatinis.survivalIsland.lives.PlayerLifecycleListener;
import com.github.Glatinis.survivalIsland.loot.ChestSubCommand;
import com.github.Glatinis.survivalIsland.loot.RestockingChestBreakListener;
import com.github.Glatinis.survivalIsland.loot.RestockingChestManager;
import com.github.Glatinis.survivalIsland.worldcontrol.DragonBlockGuardListener;
import com.github.Glatinis.survivalIsland.worldcontrol.DragonControlManager;
import com.github.Glatinis.survivalIsland.worldcontrol.ProtectionListener;
import com.github.Glatinis.survivalIsland.worldcontrol.ProtectionManager;
import com.github.Glatinis.survivalIsland.worldcontrol.PvpListener;
import com.github.Glatinis.survivalIsland.worldcontrol.PvpManager;
import com.github.Glatinis.survivalIsland.worldcontrol.RuleSubCommand;
import com.github.Glatinis.survivalIsland.worldcontrol.TheftListener;
import com.github.Glatinis.survivalIsland.worldcontrol.TheftManager;
import com.github.Glatinis.survivalIsland.worldcontrol.TntExplosionListener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SurvivalIsland extends JavaPlugin {

    private ConfigManager configManager;
    private WorldGuardHook worldGuardHook;
    private ContestantManager contestantManager;
    private LivesScoreboardService livesScoreboardService;
    private PvpManager pvpManager;
    private TheftManager theftManager;
    private ProtectionManager protectionManager;
    private EntityBoundsGuard entityBoundsGuard;
    private AcidOceanManager acidOceanManager;
    private RestockingChestManager restockingChestManager;
    private SurvivalIslandCommand rootCommand;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.load();

        worldGuardHook = new WorldGuardHook(this);

        contestantManager = new ContestantManager(this);
        contestantManager.load();

        livesScoreboardService = new LivesScoreboardService(this, configManager, contestantManager);
        livesScoreboardService.setup();
        // re-apply immediately to anyone already online (e.g. after a /reload), so contestants
        // don't have to rejoin for the board to come back.
        for (Player onlinePlayer : getServer().getOnlinePlayers()) {
            livesScoreboardService.refresh(onlinePlayer);
        }
        getServer().getPluginManager().registerEvents(new PlayerLifecycleListener(livesScoreboardService), this);

        pvpManager = new PvpManager(configManager);
        theftManager = new TheftManager(configManager);
        protectionManager = new ProtectionManager(configManager);
        getServer().getPluginManager().registerEvents(new PvpListener(pvpManager), this);
        getServer().getPluginManager().registerEvents(
            new TheftListener(theftManager, contestantManager, worldGuardHook, configManager), this);
        getServer().getPluginManager().registerEvents(
            new ProtectionListener(protectionManager, contestantManager, worldGuardHook, configManager), this);
        getServer().getPluginManager().registerEvents(new TntExplosionListener(worldGuardHook, configManager), this);
        getServer().getPluginManager().registerEvents(new NaturalSpawnListener(configManager, worldGuardHook), this);

        entityBoundsGuard = new EntityBoundsGuard(this, configManager, worldGuardHook);
        entityBoundsGuard.start();
        getServer().getPluginManager().registerEvents(
            new MobLeashListener(entityBoundsGuard, worldGuardHook, configManager), this);

        rootCommand = new SurvivalIslandCommand();
        rootCommand.register(new ContestantSubCommand(contestantManager, configManager, livesScoreboardService));
        rootCommand.register(new LivesSubCommand(livesScoreboardService, contestantManager));
        rootCommand.register(new RuleSubCommand(pvpManager, theftManager, protectionManager));
        rootCommand.register(new SpawnSubCommand(contestantManager, configManager, entityBoundsGuard));

        CommandModeManager commandModeManager = new CommandModeManager(configManager);
        getServer().getPluginManager().registerEvents(new CommandModeChatListener(commandModeManager, this), this);
        rootCommand.register(new CommandModeSubCommand(commandModeManager, configManager));

        AcidRainManager acidRainManager = new AcidRainManager(this, configManager, contestantManager, worldGuardHook);
        acidOceanManager = new AcidOceanManager(this, configManager, contestantManager, worldGuardHook);
        acidOceanManager.start();
        DeepFreezeManager deepFreezeManager = new DeepFreezeManager(this, configManager, worldGuardHook, contestantManager);
        getServer().getPluginManager().registerEvents(new DeepFreezeSnowballListener(deepFreezeManager, this), this);

        DragonControlManager dragonControlManager = new DragonControlManager();
        getServer().getPluginManager().registerEvents(
            new DragonBlockGuardListener(dragonControlManager, entityBoundsGuard), this);

        rootCommand.register(new EventSubCommand(acidRainManager, acidOceanManager, deepFreezeManager, dragonControlManager));

        restockingChestManager = new RestockingChestManager(this, configManager);
        restockingChestManager.load();
        getServer().getPluginManager().registerEvents(new RestockingChestBreakListener(restockingChestManager), this);
        rootCommand.register(new ChestSubCommand(restockingChestManager));

        getCommand("survivalisland").setExecutor(rootCommand);
        getCommand("survivalisland").setTabCompleter(rootCommand);
    }

    @Override
    public void onDisable() {
        if (livesScoreboardService != null) {
            livesScoreboardService.shutdown();
        }
        if (entityBoundsGuard != null) {
            entityBoundsGuard.stop();
        }
        if (acidOceanManager != null) {
            acidOceanManager.stopTask();
        }
        if (restockingChestManager != null) {
            restockingChestManager.stop();
        }
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
