package com.gaiagauntlet.gauntlet.games.EliminationZone.lobby.listeners;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.games.EliminationZone.components.GGPoi;
import com.gaiagauntlet.gauntlet.games.EliminationZone.lobby.TimerDisplay;
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
        if (!Objects.equals(event.getTo(), EZStates.LOBBY.name())) return;
        var session = GauntletUtils.sessionFor(event.getSessionId()).orElse(null);
        if (session == null) return;
        if (!EZController.isEz(session)) return;

        var gameId = session.getCurrentGame();
        var game = GameRegistry.getGame(gameId).orElse(null);
        if (game == null) return;

        var lobbyComp = GameStore.ensureHubStore(session.getId()).get(LobbyComponent.getComponentType());
        if (lobbyComp.isEmpty()) return;
        var world = lobbyComp.get().getWorld();

        var config = EZGameConfig.get(world, session.getId());

        if (Objects.equals(event.getTo(), EZStates.LOBBY.name())) {
            for (GGPoi arenaTimerPoi : config.getArenaTimerPois()) {
                TimerDisplay.showBase(world, arenaTimerPoi.getTransform());
            }
        } else {
            for (GGPoi arenaTimerPoi : config.getArenaTimerPois()) {
                TimerDisplay.clear(world, arenaTimerPoi.getTransform());
            }
        }

    }
}
