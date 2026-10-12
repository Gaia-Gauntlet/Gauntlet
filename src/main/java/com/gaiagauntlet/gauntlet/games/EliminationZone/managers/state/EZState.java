package com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state;

import java.util.Set;

import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.teams.utils.TeamUtils;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.plugins.events.events.MatchEventRegistry;
import com.gaiagauntlet.gauntlet.plugins.gamestate.components.MatchComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestate.events.MatchStateEvent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

public class EZState {

    public static void setup(JavaPlugin plugin) {
        MatchEventRegistry.register(MatchStateEvent.class, EZController.ID, EZStateTitles::onMatchState);
    }

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

    public static void checkEnd(GameEcs game) {
        var state = currentState(game);
        if (state != EZStates.RUNNING && state != EZStates.SUDDEN_DEATH)
            return;
        var world = GameStore.getContext(game).getWorld();
        if (TeamUtils.withAliveTeams(world, game.getSessionId(), Set.of()).size() <= 1) {
            setState(game, EZStates.ENDED);
            return;
        }
        if (state != EZStates.SUDDEN_DEATH
                && TeamUtils.countAlive(game, Set.of()) <= EZGameConfig.get(game).getSuddenDeathAt())
            setState(game, EZStates.SUDDEN_DEATH);
    }

    // boolean on whether or not it was successful
    public static boolean setState(GameEcs game, EZStates desired) {
        var match = currentState(game);
        if (match.to(desired)) {
            var state = mc(game);
            state.setState(desired.name());
            MatchEventRegistry.dispatch(
                new MatchStateEvent(game.getSessionId(), game, match.name(), desired.name()),
                EZController.ID
            );
            return true;
        }
        GaiaLog.atWarning()
                .log("Unable to transition from " + match.name() + " to " + desired.name() + " as requested");
        return false;
    }

}
