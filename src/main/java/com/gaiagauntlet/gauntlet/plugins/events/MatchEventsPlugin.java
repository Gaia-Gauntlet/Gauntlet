package com.gaiagauntlet.gauntlet.plugins.events;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.gaiagauntlet.gauntlet.core.games.interfaces.UiGamePlugin;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.plugins.events.events.MatchEventRegistry;
import com.gaiagauntlet.gauntlet.plugins.events.systems.EventTickSystem;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import java.util.List;

/**
 * Game-wide events and event listening. Listen based on `GameId` to only
 * receive local events with the relevant and oftentimes necessary context
 */
public class MatchEventsPlugin implements GamePlugin, UiGamePlugin {
    public static final String ID = "GameEventsPlugin";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<String> getDependencies() {
        return List.of();
    }

    @Override
    public void init(JavaPlugin plugin) {
        MatchEventRegistry.setup(plugin);
        plugin.getEntityStoreRegistry().registerSystem(new EventTickSystem());
    }

    @Override
    public List<AdminTab> getAdminTabs() {
        return List.of();
    }

}
