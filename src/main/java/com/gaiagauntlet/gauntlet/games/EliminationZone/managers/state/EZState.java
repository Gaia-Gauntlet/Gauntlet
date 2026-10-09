package com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state;

import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.plugins.events.events.MatchEventRegistry;
import com.gaiagauntlet.gauntlet.plugins.gamestate.components.MatchComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestate.events.MatchStateEvent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;

public class EZState {

    /** Very tiny helper to get the MatchComponent */
    private static MatchComponent mc(GameEcs game) {
        return game.ensure(MatchComponent.getComponentType(), () -> new MatchComponent(EZStates.LOBBY.name()));
    }

    public static EZStates currentState(GameEcs game) {
        var state = mc(game);
        var stateValue = state.getState();

        if (stateValue == null) {
            return null;
        }

        try {
            return EZStates.valueOf(stateValue);
        } catch (IllegalArgumentException exception) {
            GaiaLog.atError(exception).log("State " + stateValue + " is not a valid EZState");
            return EZStates.ERROR;
        }
    }

    // boolean on whether or not it was successful
    public static boolean setState(GameEcs game, EZStates desired) {
        var match = currentState(game);
        if (match.to(desired)) {
            var state = mc(game);
            state.setState(desired.name());
            MatchEventRegistry.dispatch(
                new MatchStateEvent(game.getSessionId(), match.name(), desired.name()),
                game.getSessionId()
            );
            return true;
        }
        GaiaLog.atWarning()
                .log("Unable to transition from " + match.name() + " to " + desired.name() + " as requested");
        return false;
    }
}
