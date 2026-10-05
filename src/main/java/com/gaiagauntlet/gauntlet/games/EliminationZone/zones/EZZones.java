package com.gaiagauntlet.gauntlet.games.EliminationZone.zones;

import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.ZoneVisualisationComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.assets.ZonesAsset;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

// placeholder file to get the folder structure setup, will be deleted later prolly
public class EZZones {
    public static void setup(JavaPlugin plugin) {
        ZoneVisualisationComponent.setComponentType(
            plugin.getEntityStoreRegistry().registerComponent(ZoneVisualisationComponent.class, ZoneVisualisationComponent::new)
        );
        plugin.getAssetRegistry().register(HytaleAssetStore.builder(ZonesAsset.class, new DefaultAssetMap<>())
            .setPath("Gauntlet/Games/EZ/Zones")
            .setCodec(ZonesAsset.CODEC)
            .setKeyFunction(ZonesAsset::getId)
            .build());
    }
}
