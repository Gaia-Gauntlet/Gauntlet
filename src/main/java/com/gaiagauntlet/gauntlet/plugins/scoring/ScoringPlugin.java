package com.gaiagauntlet.gauntlet.plugins.scoring;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.gaiagauntlet.gauntlet.plugins.teams.TeamsPlugin;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import java.util.List;

public class ScoringPlugin implements GamePlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "ScoringPlugin";

    @Override
    public void init(JavaPlugin host) {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<String> getDependencies() {
        return List.of(TeamsPlugin.ID);
    }
}
