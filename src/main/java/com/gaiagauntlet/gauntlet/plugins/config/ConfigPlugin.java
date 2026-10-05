package com.gaiagauntlet.gauntlet.plugins.config;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.Getter;

import java.util.List;

public class ConfigPlugin implements GamePlugin {

    @Getter public static final String Id = "ConfigPlugin";


    @Override
    public void init(JavaPlugin plugin) {

    }

    @Override
    public List<String> getDependencies() {
        return List.of();
    }
}
