package com.gaiagauntlet.gauntlet.core.orchestrator;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.admin.AdminLog;
import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;
import com.gaiagauntlet.gauntlet.core.events.GauntletEventRegistry;
import com.gaiagauntlet.gauntlet.core.events.events.GameEndEvent;
import com.gaiagauntlet.gauntlet.core.events.events.GameEvent;
import com.gaiagauntlet.gauntlet.core.events.events.GamePlayerEvent;
import com.gaiagauntlet.gauntlet.core.events.events.NewSessionEvent;
import com.gaiagauntlet.gauntlet.core.events.events.SessionEvent;
import com.gaiagauntlet.gauntlet.core.events.events.SessionQueueEvent;
import com.gaiagauntlet.gauntlet.core.events.events.UniversePlayerEvent;
import com.gaiagauntlet.gauntlet.core.games.interfaces.GameController;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.orchestrator.handlers.GameHandlers;
import com.gaiagauntlet.gauntlet.core.orchestrator.handlers.PlayerHandlers;
import com.gaiagauntlet.gauntlet.core.orchestrator.handlers.SessionHandlers;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.server.core.event.events.player.PlayerEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

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
     * Business rules because I have nowhere else to put them.
     * 1) All controller methods should be invoked on the HUB thread
     * 2) SessionId states should be cleared between games
     * 3) Games should be setup before any player is allowed to join
     * 4) Eventing needs
     *     a) Failure Events
     *     b) Session status updates
     * 5) threading needs
     *     a) player join
     *     b) player leave
     *     c) server shutdown
     *     d) server startup (load up from crashed server - attempt recovery?)
     */

    /**
     * Registers all of the events LATE so that they can be intercepted easily
     */
    public static void setupListeners() {
        // session event handling
        GauntletEventRegistry.on(EventPriority.LATE, SessionEvent.class, wrap(SessionHandlers::handleSession));
        GauntletEventRegistry.on(EventPriority.LATE, SessionQueueEvent.class,
                wrap(SessionHandlers::handleSessionQueue));
        GauntletEventRegistry.on(EventPriority.LATE, NewSessionEvent.class, wrap(SessionHandlers::handleNewSession));

        // game event handling
        GauntletEventRegistry.on(EventPriority.LATE, GameEvent.class, wrap(GameHandlers::handleGame));
        GauntletEventRegistry.on(EventPriority.LATE, GameEndEvent.class, wrap(GameHandlers::handleGameEnd));

        // player event handling
        GauntletEventRegistry.on(EventPriority.LATE, UniversePlayerEvent.class, wrap(PlayerHandlers::handlePlayer));
        GauntletEventRegistry.on(EventPriority.LATE, GamePlayerEvent.class, wrap(PlayerHandlers::handleGamePlayer));
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
