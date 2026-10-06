package com.gaiagauntlet.gauntlet.plugins.transfer;

import java.util.Collection;
import java.util.List;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.gaiagauntlet.gauntlet.core.games.interfaces.SimpleGamePlugin;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.plugins.gamestore.GameStorePlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.interfaces.PersistentGamePlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestore.interfaces.SessionWriter;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameComponentRegistry;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.transfer.components.TransferComponent;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class TransferPlugin implements GamePlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "TransferPlugin";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<String> getDependencies() {
        return List.of(GameStorePlugin.ID);
    }

    @Override
    public void init(JavaPlugin host) {
        TransferComponent
                .setComponentType(GameComponentRegistry.register(TransferComponent.ID, TransferComponent.class));

    }

    /** Queues a player to join, will be added on the next batch */
    public void queue(ComponentAccessor<EntityStore> hubAccessor, World destination, String sessionId,
            Collection<PlayerRef> players, Runnable onComplete) {
        var hubEcs = GameStore.ensureStore(hubAccessor, sessionId);
        var transferComponent = hubEcs.ensure(TransferComponent.getComponentType(), () -> new TransferComponent(destination));
        
    }
}
