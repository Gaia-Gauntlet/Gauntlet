package com.gaiagauntlet.gauntlet.plugins.gamestate.utils;

import com.gaiagauntlet.gauntlet.plugins.gamestate.components.MatchComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;

public class MatchUtils {
    public static final String DEFAULT = "default";

    public static MatchComponent withMatch(GameEcs game) {
        return game.ensure(MatchComponent.getComponentType(), () -> new MatchComponent(DEFAULT));

    }

    public static void transition(GameEcs game, String newState) {
        var match = withMatch(game);
        match.setState(newState);
        

    }
}
