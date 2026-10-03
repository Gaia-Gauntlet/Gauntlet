package com.gaiagauntlet.gauntlet;

import com.gaiagauntlet.gauntlet.core.GauntletCore;
import com.gaiagauntlet.gauntlet.core.commands.GauntletCommand;
import com.gaiagauntlet.gauntlet.core.commands.PartyCommand;
import com.gaiagauntlet.gauntlet.core.components.PlayerComponent;
import com.gaiagauntlet.gauntlet.core.events.GauntletEventRegistry;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.party.resources.UniversePartyResource;
import com.gaiagauntlet.gauntlet.core.resources.UniverseGameResource;
import com.gaiagauntlet.gauntlet.plugins.announcer.AnnouncerPlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestate.GameStatePlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestore.GameStorePlugin;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.LobbyControllerPlugin;
import com.gaiagauntlet.gauntlet.plugins.proxychat.ProxyChatPlugin;
import com.gaiagauntlet.gauntlet.plugins.scoring.ScoringPlugin;
import com.gaiagauntlet.gauntlet.plugins.teams.TeamsPlugin;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;

import lombok.Getter;

public class GauntletPlugin extends JavaPlugin {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Store reference to command to allow sub-plugins to add subcommands */
    @Getter
    private static GauntletCommand gauntletCommand;

    public GauntletPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Starting Gauntlet!");
        setupCommands();
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up Gauntlet!");
        setupCore();
        setupResources();
        setupComponents();
        setupPlugins();
    }

    // sets up internal or core operations like registries or event handlers
    private void setupCore() {
        // Setup the events
        GauntletEventRegistry.setup(this);
        GauntletCore.setup(this);
    }

    private void setupResources() {
        UniverseGameResource.setResourceType(
            Universe.registerResource(UniverseGameResource.class, UniverseGameResource.ID,
                    UniverseGameResource.CODEC));
        UniversePartyResource.setResourceType(
            Universe.registerResource(UniversePartyResource.class, UniversePartyResource.ID,
                UniversePartyResource.CODEC));
    }

    private void setupCommands() {
        getCommandRegistry().registerCommand(new GauntletCommand());
        getCommandRegistry().registerCommand(new PartyCommand());
    }

    private void setupComponents() {
        var entityRegistry = getEntityStoreRegistry();
        PlayerComponent.setComponentType(
                entityRegistry.registerComponent(PlayerComponent.class, PlayerComponent.ID, PlayerComponent.CODEC));
    }

    private void setupPlugins() {
        GameRegistry.registerPlugin(AnnouncerPlugin.ID, this, AnnouncerPlugin::new);
        GameRegistry.registerPlugin(GameStatePlugin.ID, this, GameStatePlugin::new);
        GameRegistry.registerPlugin(GameStorePlugin.ID, this, GameStorePlugin::new);
        GameRegistry.registerPlugin(LobbyControllerPlugin.ID, this, LobbyControllerPlugin::new);
        GameRegistry.registerPlugin(ProxyChatPlugin.ID, this, ProxyChatPlugin::new);
        GameRegistry.registerPlugin(TeamsPlugin.ID, this, TeamsPlugin::new);
        GameRegistry.registerPlugin(ScoringPlugin.ID, this, ScoringPlugin::new);
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down Gauntlet!");
    }
}
