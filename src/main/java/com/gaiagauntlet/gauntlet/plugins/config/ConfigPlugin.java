package com.gaiagauntlet.gauntlet.plugins.config;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.session.registry.SessionRegistry;
import com.gaiagauntlet.gauntlet.core.games.interfaces.UiGamePlugin;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.plugins.config.components.GameConfigComponent;
import com.gaiagauntlet.gauntlet.plugins.config.ui.ConfigTab;
import com.gaiagauntlet.gauntlet.plugins.config.components.SessionGameConfigComponent;
import com.gaiagauntlet.gauntlet.plugins.config.components.assets.EmptyGameConfigAsset;
import com.gaiagauntlet.gauntlet.plugins.config.components.assets.GameConfigAsset;
import com.gaiagauntlet.gauntlet.plugins.gamestore.GameStorePlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.interfaces.PersistentGamePlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestore.interfaces.SessionWriter;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameComponentRegistry;
import com.gaiagauntlet.gauntlet.plugins.teams.components.assets.TeamListAsset;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.type.gameplay.GameplayConfig;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;

public class ConfigPlugin implements GamePlugin, PersistentGamePlugin, UiGamePlugin {

    public static final String ID = "ConfigPlugin";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Config";
    }

    @Override
    public void init(JavaPlugin plugin) {
        plugin.getAssetRegistry().register(HytaleAssetStore.builder(GameConfigAsset.class,
                new IndexedLookupTableAssetMap<>(GameConfigAsset[]::new))
            .setPath("Gauntlet/Plugins/" + ID + "/GameConfig")
            .setCodec(GameConfigAsset.CODEC)
            .setKeyFunction(GameConfigAsset::getId)
            .setReplaceOnRemove(_ -> new EmptyGameConfigAsset())
            .loadsAfter(TeamListAsset.class,GameplayConfig.class)
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
    public void setup(ComponentAccessor<EntityStore> arenaAccessor, GameSession session, GameEcs gameStore, String gameId) {
        var configId = session.get(SessionGameConfigComponent.getSessionComponentType())
            .map(configs -> configs.getConfigId(gameId))
            .orElse(gameId);
        gameStore.put(GameConfigComponent.getComponentType(), new GameConfigComponent(configId));
    }

    @Override
    public List<AdminTab> getAdminTabs() {
        return List.of(new ConfigTab());
    }

    @Override
    public SessionWriter capture(World arenaWorld, GameEcs store, String sessionId) {
        return null;
    }
}
