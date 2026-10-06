package com.gaiagauntlet.gauntlet.core.orchestrator;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;
import com.gaiagauntlet.gauntlet.core.events.GauntletEventRegistry;
import com.gaiagauntlet.gauntlet.core.events.events.GameEndEvent;
import com.gaiagauntlet.gauntlet.core.events.events.GameEvent;
import com.gaiagauntlet.gauntlet.core.events.events.PlayerGameEvent;
import com.gaiagauntlet.gauntlet.core.events.events.PlayerPartyEvent;
import com.gaiagauntlet.gauntlet.core.events.events.NewSessionEvent;
import com.gaiagauntlet.gauntlet.core.events.events.SessionEvent;
import com.gaiagauntlet.gauntlet.core.events.events.SessionQueueEvent;
import com.gaiagauntlet.gauntlet.core.orchestrator.handlers.GameHandlers;
import com.gaiagauntlet.gauntlet.core.orchestrator.handlers.PlayerHandlers;
import com.gaiagauntlet.gauntlet.core.orchestrator.handlers.SessionHandlers;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.World;

/**
 * The very thin big boi router
 * 90% of the business logic for this should exist within the GameController
 * 
 * The Orchestrator is simply there to route and standardize implementations
 * 
 * All business-logic implementations should be within the handlers/
 */
public class GauntletOrchestrator {

    /**
     * Registers all of the events LATE so that they can be intercepted easily
     */
    public static void setupListeners(JavaPlugin plugin) {
        // session event handling
        GauntletEventRegistry.on(EventPriority.LAST, SessionEvent.class, wrap(SessionHandlers::handleSession));
        GauntletEventRegistry.on(EventPriority.LAST, SessionQueueEvent.class,
                wrap(SessionHandlers::handleSessionQueue));
        GauntletEventRegistry.on(EventPriority.LAST, NewSessionEvent.class, wrap(SessionHandlers::handleNewSession));

        // game event handling
        GauntletEventRegistry.on(EventPriority.LAST, GameEvent.class, wrap(GameHandlers::handleGame));
        GauntletEventRegistry.on(EventPriority.LAST, GameEndEvent.class, wrap(GameHandlers::handleGameEnd));

        // player event handling
        GauntletEventRegistry.on(EventPriority.LAST, PlayerGameEvent.class, wrap(PlayerHandlers::handleGamePlayer));
        GauntletEventRegistry.on(EventPriority.LAST, PlayerPartyEvent.class, wrap(PlayerHandlers::handlePartyPlayer));
        var registry = plugin.getEventRegistry();
        registry.register(PlayerConnectEvent.class, PlayerHandlers::onPlayerConnect);
        registry.registerGlobal(PlayerReadyEvent.class, PlayerHandlers::onPlayerReady);
        registry.register(PlayerDisconnectEvent.class, PlayerHandlers::onPlayerDisconnect);
    }

    /** Ensures the event handler is always run on the hub world */
    private static <T extends GauntletEvent> Consumer<T> wrap(@Nonnull BiConsumer<World, T> listener) {
        return (T event) -> {
            // always run events on the hub world
            var hubWorld = GauntletUtils.withHubWorld();
            GauntletUtils.run(hubWorld, () -> {
                listener.accept(hubWorld, event);
            });
        };
    }
}
