package com.gaiagauntlet.gauntlet.plugins.config;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.session.registry.SessionRegistry;
import com.gaiagauntlet.gauntlet.plugins.config.components.GameConfigComponent;
import com.gaiagauntlet.gauntlet.plugins.config.components.SessionGameConfigComponent;
import com.gaiagauntlet.gauntlet.plugins.config.components.assets.EmptyGameConfigAsset;
import com.gaiagauntlet.gauntlet.plugins.config.components.assets.GameConfigAsset;
import com.gaiagauntlet.gauntlet.plugins.gamestore.GameStorePlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.interfaces.PersistentGamePlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestore.interfaces.SessionWriter;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameComponentRegistry;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.type.gameplay.GameplayConfig;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;

public class ConfigPlugin implements GamePlugin, PersistentGamePlugin {

    public static final String ID = "ConfigPlugin";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void init(JavaPlugin plugin) {
        plugin.getAssetRegistry().register(HytaleAssetStore.builder(GameConfigAsset.class,
                new IndexedLookupTableAssetMap<>(GameConfigAsset[]::new))
            .setPath("Gauntlet/Plugins/" + ID + "/GameConfigAsset")
            .setCodec(GameConfigAsset.CODEC)
            .setKeyFunction(GameConfigAsset::getId)
            .setReplaceOnRemove(_ -> new EmptyGameConfigAsset())
            .loadsAfter(GameplayConfig.class)
            .build());

        SessionGameConfigComponent.setSessionComponentType(SessionRegistry.register(
            SessionGameConfigComponent.ID,
            SessionGameConfigComponent.class,
            SessionGameConfigComponent.CODEC
        ));
        SessionGameConfigComponent.setGameComponentType(GameComponentRegistry.register(
            SessionGameConfigComponent.ID,
            SessionGameConfigComponent.class
        ));
        GameConfigComponent.setComponentType(
            GameComponentRegistry.register(GameConfigComponent.ID, GameConfigComponent.class)
        );
    }

    @Override
    public List<String> getDependencies() {
        return List.of(
            GameStorePlugin.ID
        );
    }

    @Override
    public void setup(ComponentAccessor<EntityStore> accessor, GameSession session, String gameId) {
        var sessionGameConfigComponent = session.ensure(
            SessionGameConfigComponent.getSessionComponentType(),
            new SessionGameConfigComponent()
        );
        var overrideConfig = sessionGameConfigComponent.getConfig(gameId);
        var config = overrideConfig == null
            ? getId() // Game ID is the default config ID
            : overrideConfig;
        GameStore.ensureStore(accessor, gameId).put(
            GameConfigComponent.getComponentType(),
            new GameConfigComponent(config)
        );
    }

    @Override
    public SessionWriter capture(World arenaWorld, GameEcs store, String sessionId) {
        return null;
    }
}
