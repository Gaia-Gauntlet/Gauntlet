package com.gaiagauntlet.gauntlet.plugins.lobbycontroller;

import java.util.List;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestore.GameStorePlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameComponentRegistry;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.components.LobbyComponent;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.systems.PlayerSystems;
import com.gaiagauntlet.gauntlet.plugins.transfer.TransferPlugin;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

/**
 * A lobby controller plugin that lets you opt-into lobby logic.
 */
public class LobbyControllerPlugin implements GamePlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "LobbyControllerPlugin";

    @Override
    public void init(JavaPlugin host) {
        LobbyComponent.setComponentType(GameComponentRegistry.register(LobbyComponent.ID, LobbyComponent.class));
        var registry = host.getEventRegistry();
        registry.registerGlobal(PlayerReadyEvent.class, PlayerSystems::onPlayerConnect);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<String> getDependencies() {
        return List.of(GameStorePlugin.ID, TransferPlugin.ID);
    }
}
