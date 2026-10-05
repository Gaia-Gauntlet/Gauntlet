package com.gaiagauntlet.gauntlet.plugins.gamestate;

import com.gaiagauntlet.gauntlet.core.games.interfaces.SimpleGamePlugin;
import com.gaiagauntlet.gauntlet.core.games.interfaces.UiGamePlugin;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;

/** Simple state machine handler implementation */
public class GameStatePlugin implements SimpleGamePlugin, UiGamePlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "GameStatePlugin";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<String> getDependencies() {
        return List.of();
    }

    @Override
    public void setup(ComponentAccessor<EntityStore> accessor, String gameId) {
        
    }

    public void init(JavaPlugin host) {}

    @Override
    public List<AdminTab> getAdminTabs() {
        return List.of();
    }
}
