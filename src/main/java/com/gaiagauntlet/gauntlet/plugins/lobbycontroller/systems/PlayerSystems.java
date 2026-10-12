package com.gaiagauntlet.gauntlet.plugins.lobbycontroller.systems;

import com.gaiagauntlet.gauntlet.core.session.constants.SessionState;
import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.party.utils.PartyUtils;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.LobbyController;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.components.LobbyComponent;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.components.PlayerMarker;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;

public class PlayerSystems {
    // runs when the player is connecting to a world
    public static void onPlayerConnect(PlayerReadyEvent event) {
        var ref = event.getPlayerRef();
        var store = ref.getStore();
        var destination = store.getExternalData().getWorld();
        var hub = GauntletUtils.withHubWorld();
        var destinationId = destination.getWorldConfig().getUuid();
        if (destinationId.equals(hub.getWorldConfig().getUuid()))
            return; // joining the hub - known early return

        var player = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null)
            return;

        var playerMarker = store.getComponent(ref, PlayerMarker.getComponentType());
        if (playerMarker == null) return;

        store.removeComponent(ref, PlayerMarker.getComponentType());
        var desiredUuid = playerMarker.getDesiredWorld();
        var sessionId = playerMarker.getSessionId();
        if (!desiredUuid.equals(destinationId)) {
            strand(player, sessionId, "landed in " + destination.getName() + " instead of the arena");
            return;
        }

        GauntletUtils.run(hub, () -> {
            var session = GauntletUtils.sessionFor(sessionId).orElse(null);
            if (session == null) {
                strand(player, sessionId, "the session no longer exists");
                return;
            }
            if (session.getSessionState() != SessionState.RUNNING) {
                strand(player, sessionId, "the session's game is no longer running");
                return;
            }
            var currentGame = session.getCurrentGame();
            if (currentGame == null
                    || !(GameRegistry.getGame(currentGame).orElse(null) instanceof LobbyController lobbyController)) {
                strand(player, sessionId, "the session has no lobby game running");
                return;
            }

            var party = PartyUtils.getParty(player).orElse(null);
            var partySession = party == null ? null
                    : GauntletUtils.withResource().sessionFor(party.getId()).orElse(null);
            if (partySession == null || !partySession.getId().equals(sessionId)) {
                strand(player, sessionId, "their party is no longer in the session");
                return;
            }

            var gameWorld = LobbyController.withArenaWorld(sessionId)
                    .filter(world -> world.isAlive() && world.getWorldConfig().getUuid().equals(destinationId))
                    .orElse(null);
            if (gameWorld == null) {
                strand(player, sessionId, "the arena is no longer live");
                return;
            }

            GauntletUtils.run(gameWorld, () -> {
                if (!ref.isValid())
                    return;
                try {
                    lobbyController.getLobbyManager().onJoin(ref, player, store, session.getId());
                } catch (Exception error) {
                    GaiaLog.atError(error).withSession(session)
                            .log("Failed to dispatch controller connection logic!");
                    strand(player, sessionId, "the game failed to receive them");
                }
            });
        });
    }

    private static void strand(PlayerRef player, String sessionId, String reason) {
        GaiaLog.atWarning().withSession(sessionId)
                .log("Sending " + player.getUsername() + " back to the hub: " + reason);
        PlayerUtils.sendToHub(player).exceptionally(error -> {
            GaiaLog.atError(error).withSession(sessionId)
                    .log("Could not return " + player.getUsername() + " to the hub");
            return null;
        });
    }
}
