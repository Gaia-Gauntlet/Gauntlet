package com.gaiagauntlet.gauntlet;

import java.util.concurrent.TimeUnit;

import com.gaiagauntlet.gauntlet.core.commands.GgCommand;
import com.gaiagauntlet.gauntlet.core.components.PlayerComponent;
import com.gaiagauntlet.gauntlet.core.resources.UniverseGameResource;
import com.gaiagauntlet.gauntlet.core.session.registry.SessionRegistry;
import com.gaiagauntlet.gauntlet.core.session.testing.SessionComponentTest;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;

import lombok.Getter;

public class GauntletPlugin extends JavaPlugin {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Store reference to command to allow sub-plugins to add subcommands */
    @Getter
    private static GgCommand ggCommand;

    public GauntletPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Starting Gauntlet!");    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up Gauntlet!");
        setupResources();
        setupCommands();
        setupComponents();

    }

    private void setupResources() {
        UniverseGameResource.setResourceType(
                Universe.registerResource(UniverseGameResource.class, UniverseGameResource.ID,
                        UniverseGameResource.CODEC));
    }

    private void setupCommands() {
        ggCommand = new GgCommand();
        getCommandRegistry().registerCommand(ggCommand);
    }

    private void setupComponents() {
        var entityRegistry = getEntityStoreRegistry();
        PlayerComponent.setComponentType(
                entityRegistry.registerComponent(PlayerComponent.class, PlayerComponent.ID, PlayerComponent.CODEC));
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down Gauntlet!");
    }
}
