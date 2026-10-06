package com.gaiagauntlet.gauntlet.plugins.config;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.gaiagauntlet.gauntlet.plugins.config.components.assets.EmptyGameConfig;
import com.gaiagauntlet.gauntlet.plugins.config.components.assets.GameConfig;
import com.gaiagauntlet.gauntlet.plugins.gamestore.GameStorePlugin;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.type.gameplay.GameplayConfig;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import lombok.Getter;

import java.util.List;

public class ConfigPlugin implements GamePlugin {

    public static final String ID = "ConfigPlugin";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void init(JavaPlugin plugin) {
        plugin.getAssetRegistry().register(HytaleAssetStore.builder(GameConfig.class,
                new IndexedLookupTableAssetMap<>(GameConfig[]::new))
            .setPath("Gauntlet/Plugins/" + ID + "/GameConfig")
            .setCodec(GameConfig.CODEC)
            .setKeyFunction(GameConfig::getId)
            .setReplaceOnRemove(_ -> new EmptyGameConfig())
            .loadsAfter(GameplayConfig.class)
            .build());
    }

    @Override
    public List<String> getDependencies() {
        return List.of(
            GameStorePlugin.ID
        );
    }
}
