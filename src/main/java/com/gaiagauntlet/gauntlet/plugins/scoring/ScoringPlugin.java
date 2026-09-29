package com.gaiagauntlet.gauntlet.plugins.scoring;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GamePlugin;
import com.gaiagauntlet.gauntlet.plugins.teams.TeamsPlugin;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamComponent;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import lombok.Getter;

import java.util.List;

public class ScoringPlugin extends GamePlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    @Getter public static final String ID = "ScoringPlugin";

    @Override
    public void install() {
        //
    }

    public void setup(JavaPlugin host) {}

    @Override
    public List<String> getDependencies() {
        var deps = super.getDependencies();
        deps.add(TeamsPlugin.getID());
        return deps;
    }
}
