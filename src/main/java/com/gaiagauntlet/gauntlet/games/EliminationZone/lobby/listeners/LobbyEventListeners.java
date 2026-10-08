package com.gaiagauntlet.gauntlet.games.EliminationZone.lobby.listeners;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfigAsset;
import com.gaiagauntlet.gauntlet.games.EliminationZone.components.GGPoi;
import com.gaiagauntlet.gauntlet.games.EliminationZone.lobby.TimerDisplay;
import com.gaiagauntlet.gauntlet.plugins.config.components.SessionGameConfigComponent;
import com.gaiagauntlet.gauntlet.plugins.config.components.assets.GameConfigAsset;
import com.gaiagauntlet.gauntlet.plugins.gamestate.constants.MatchState;
import com.gaiagauntlet.gauntlet.plugins.gamestate.events.MatchStateEvent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.components.LobbyComponent;

public final class LobbyEventListeners {
    private LobbyEventListeners() {}

    public static void onMatchState(MatchStateEvent event) {
        if (event.getTo() != MatchState.LOBBY_COUNTDOWN && event.getTo() != MatchState.TRANSFERRING) return;
        var session = GauntletUtils.sessionFor(event.getSessionId()).orElse(null);
        if (session == null) return;
        if (!EZController.isEz(session)) return;

        var gameId = session.getCurrentGame();
        var game = GameRegistry.getGame(gameId).orElse(null);
        if (game == null) return;

        var lobbyComp = GameStore.ensureHubStore(session.getId()).get(LobbyComponent.getComponentType());
        if (lobbyComp.isEmpty()) return;
        var world = lobbyComp.get().getWorld();

        var configComp = session.get(SessionGameConfigComponent.getSessionComponentType()).orElse(null);
        var config = configComp != null
            ? GameConfigAsset.getAssetMap().get(configComp.getConfig(gameId))
            : new EZGameConfigAsset();
        if (!(config instanceof EZGameConfigAsset ezConfig)) return;

        if (event.getTo() == MatchState.LOBBY_COUNTDOWN) {
            for (GGPoi arenaTimerPoi : ezConfig.getArenaTimerPois()) {
                TimerDisplay.showBase(world, arenaTimerPoi.getTransform());
            }
        } else {
            for (GGPoi arenaTimerPoi : ezConfig.getArenaTimerPois()) {
                TimerDisplay.clear(world, arenaTimerPoi.getTransform());
            }
        }

    }
}
