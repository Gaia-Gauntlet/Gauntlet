package com.gaiagauntlet.gauntlet.games.EliminationZone.lobby.listeners;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.games.EliminationZone.components.GGPoi;
import com.gaiagauntlet.gauntlet.games.EliminationZone.lobby.TimerDisplay;
import com.gaiagauntlet.gauntlet.games.EliminationZone.lobby.components.GameTimerComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state.EZStates;
import com.gaiagauntlet.gauntlet.plugins.config.components.SessionGameConfigComponent;
import com.gaiagauntlet.gauntlet.plugins.config.components.assets.GameConfigAsset;
import com.gaiagauntlet.gauntlet.plugins.gamestate.events.MatchStateEvent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.components.LobbyComponent;

import java.util.Objects;

public final class LobbyEventListeners {
    private LobbyEventListeners() {}

    public static void onMatchState(MatchStateEvent event) {
        if (!Objects.equals(event.getTo(), EZStates.RUNNING.name())) return;

        var session = GauntletUtils.sessionFor(event.getSessionId()).orElse(null);
        if (session == null) return;
        if (!EZController.isEz(session)) return;

        var gameId = session.getCurrentGame();
        var gameController = GameRegistry.getGame(gameId).orElse(null);
        if (gameController == null) return;

        var lobbyComp = GameStore.ensureHubStore(session.getId()).get(LobbyComponent.getComponentType());
        if (lobbyComp.isEmpty()) return;
        var world = lobbyComp.get().getWorld();

        var gameEcs = GameStore.withStore(world, session.getId()).orElse(null);
        if (gameEcs == null) return;
        gameEcs.put(GameTimerComponent.getComponentType(), new GameTimerComponent());
    }
}
