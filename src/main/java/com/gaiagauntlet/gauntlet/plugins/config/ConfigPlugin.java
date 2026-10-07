package com.gaiagauntlet.gauntlet.plugins.config;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.gaiagauntlet.gauntlet.core.session.registry.SessionRegistry;
import com.gaiagauntlet.gauntlet.plugins.config.components.GameConfigComponent;
import com.gaiagauntlet.gauntlet.plugins.config.components.SessionGameConfigComponent;
import com.gaiagauntlet.gauntlet.plugins.config.components.assets.EmptyGameConfigAsset;
import com.gaiagauntlet.gauntlet.plugins.config.components.assets.GameConfigAsset;
import com.gaiagauntlet.gauntlet.plugins.gamestore.GameStorePlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameComponentRegistry;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.type.gameplay.GameplayConfig;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import java.util.List;

public class ConfigPlugin implements GamePlugin {

    public static final String ID = "ConfigPlugin";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void init(JavaPlugin plugin) {
        plugin.getAssetRegistry().register(HytaleAssetStore.builder(GameConfigAsset.class,
                new IndexedLookupTableAssetMap<>(GameConfigAsset[]::new))
            .setPath("Gauntlet/Plugins/" + ID + "/GameConfig")
            .setCodec(GameConfigAsset.CODEC)
            .setKeyFunction(GameConfigAsset::getId)
            .setReplaceOnRemove(_ -> new EmptyGameConfigAsset())
            .loadsAfter(GameplayConfig.class)
            .build());

        SessionGameConfigComponent.setComponentType(SessionRegistry.register(
                SessionGameConfigComponent.ID,
                SessionGameConfigComponent.class,
                SessionGameConfigComponent.CODEC
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
}
