package com.gaiagauntlet.gauntlet.games.EliminationZone;

import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.EZBosses;
import com.gaiagauntlet.gauntlet.games.EliminationZone.combat.EZCombat;
import com.gaiagauntlet.gauntlet.games.EliminationZone.components.EZGameComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.components.EZPlayerComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.lobby.EZSpawnComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.lobby.EZLobby;
import com.gaiagauntlet.gauntlet.games.EliminationZone.weather.EZWeather;
import com.gaiagauntlet.gauntlet.games.EliminationZone.ui.EZUi;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.EZZones;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.events.EZLootHandlers;
import com.gaiagauntlet.gauntlet.plugins.config.components.assets.GameConfigAsset;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameComponentRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class EZGamePlugin extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public EZGamePlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Starting EZGame!");
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up EZGame!");
        registerComponents();
        // Register the game
        GameRegistry.registerGame(EZController.ID, EZController::new);

        registerSubcomponents();
        // register the config
        GameConfigAsset.CODEC.register(EZController.ID, EZGameConfig.class, EZGameConfig.CODEC);
    }

    private void registerSubcomponents() {
        // Setup each section - keeps the top-level plugin cleaner this way
        EZBosses.setup(this);
        EZCombat.setup(this);
        EZWeather.setup(this);
        EZZones.setup(this);
        EZLobby.setup(this);
        EZUi.setup();
        EZLootHandlers.setup();
    }

    private void registerComponents() {
        // hytale components
        var entityStore = getEntityStoreRegistry();
        EZPlayerComponent.setComponentType(
            entityStore.registerComponent(EZPlayerComponent.class, EZPlayerComponent::new)
        );
        // game components
        EZSpawnComponent.setComponentType(GameComponentRegistry.register(EZSpawnComponent.ID, EZSpawnComponent.class));
        EZGameComponent.setComponentType(GameComponentRegistry.register(EZGameComponent.ID, EZGameComponent.class));
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down EZGame!");
    }
}
