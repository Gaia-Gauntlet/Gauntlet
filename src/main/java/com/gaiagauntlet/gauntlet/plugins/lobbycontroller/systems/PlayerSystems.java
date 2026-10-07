package com.gaiagauntlet.gauntlet.plugins.lobbycontroller.systems;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.party.utils.PartyUtils;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.LobbyController;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.components.LobbyComponent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

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

        // run on hub thread
        GauntletUtils.run(hub, () -> {
            var session = PartyUtils
                    .getPartyNullable(player)
                    .flatMap(PartyUtils::sessionFor)
                    .filter(s -> !s.available())
                    .orElse(null);
            if (session == null) return;
            var currentGame = session.getCurrentGame();
            if (currentGame == null)
                return;

            // get the lobby controller for the game
            if (!(GameRegistry.getGame(currentGame).orElse(null) instanceof LobbyController lobbyController)) {
                // not even the right controller
                return;
            }

            var gameWorld = GameStore.withStore(hub, session.getId())
                    .flatMap(s -> s.get(LobbyComponent.getComponentType()))
                    .map(lobby -> lobby.getWorld())
                    .filter(world -> world.isAlive() && world.getWorldConfig().getUuid().equals(destinationId))
                    .orElse(null);

            if (gameWorld == null) {
                // game world invalid
                return;
            }

            GauntletUtils.run(gameWorld, () -> {
                if (!ref.isValid())
                    return; // ref became invalid during setup
                try {
                    lobbyController.getLobbyManager().onJoin(ref, store, session.getId());
                } catch (Exception error) {
                    GaiaLog.atError(error).withSession(session)
                            .log("Failed to dispatch controller connection logic!");
                }
            });

        });

    }
}
