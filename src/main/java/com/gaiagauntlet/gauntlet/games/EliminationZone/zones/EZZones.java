package com.gaiagauntlet.gauntlet.games.EliminationZone.zones;

import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.ZoneVisualisationComponent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

// placeholder file to get the folder structure setup, will be deleted later prolly
public class EZZones {
    public static void setup(JavaPlugin plugin) {
        ZoneVisualisationComponent.setComponentType(
            plugin.getEntityStoreRegistry().registerComponent(ZoneVisualisationComponent.class, ZoneVisualisationComponent::new)
        );
    }
}
