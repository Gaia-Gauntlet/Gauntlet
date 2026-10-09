package com.gaiagauntlet.gauntlet.plugins.events;

import java.util.List;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.gaiagauntlet.gauntlet.plugins.events.events.MatchEventRegistry;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

/**
 * Game-wide events and event listening. Listen based on `GameId` to only
 * receive local events with the relevant and oftentimes necessary context
 */
public class MatchEventsPlugin implements GamePlugin {
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
    }

}
