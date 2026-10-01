package com.gaiagauntlet.gauntlet.plugins.lobbycontroller;

import java.util.List;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.gaiagauntlet.gauntlet.core.gamestore.registry.GameComponentRegistry;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.components.ArenaComponent;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

/**
 * A lobby controller plugin that lets you opt-into lobby logic.
 */
public class LobbyControllerPlugin implements GamePlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "LobbyControllerPlugin";

    @Override
    public void init(JavaPlugin host) {
        ArenaComponent.setComponentType(GameComponentRegistry.register(ArenaComponent.ID, ArenaComponent.class));
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<String> getDependencies() {
        return List.of();
    }
}
