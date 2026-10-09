package com.gaiagauntlet.gauntlet.plugins.events.events;

import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

public class MatchEventRegistry {
    private static EventRegistry eventRegistry;

    public static void setup(JavaPlugin plugin) {
        eventRegistry = plugin.getEventRegistry();
    }

    /** Registers for events dispatched for a specific game */
    public static <T extends MatchEvent> void register(Class<? super T> evtClass, String GameId, Consumer<T> consumer) {
        eventRegistry.register(evtClass, GameId, consumer);
    }

    public static <T extends MatchEvent> void dispatch(@NotNull T event, String sessionId) {
        var dispatch = HytaleServer.get().getEventBus().dispatchFor(MatchEvent.class, sessionId);
        GaiaLog.atInfo().withGameId(sessionId).log("Dispatching event " + event.toString() + " for " + sessionId);
        dispatch.dispatch(event);
    }

}
