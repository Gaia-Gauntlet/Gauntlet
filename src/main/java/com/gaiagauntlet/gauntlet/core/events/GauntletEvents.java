package com.gaiagauntlet.gauntlet.core.events;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.World;

/**
 * Gauntlet Events serve as a way to suggest mutations / changes to the game
 * through a single pipeline
 */
public class GauntletEvents {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static EventRegistry registry = null;

    public static void setup(JavaPlugin plugin) {
        registry = plugin.getEventRegistry();
    }

    public static <T extends GauntletEvent.Event> void on(@Nonnull Class<T> type, @Nonnull BiConsumer<World, T> listener) {
        if (registry == null) {
            LOGGER.atSevere().log("Failed to listen for event because registry is not setup");
            return;
        }
        registry.registerGlobal(type, event -> {
            try {
                // always run events on the hub world
                var hubWorld = GauntletUtils.withHubWorld();
                GauntletUtils.run(hubWorld, () -> {
                    listener.accept(hubWorld, event);
                });
            } catch (RuntimeException e) {
                LOGGER.atSevere().withCause(e).log("Listener for %s failed", type.getSimpleName());
            }
        });
    }

    public static void dispatch(@Nonnull GauntletEvent.Event event) {
        LOGGER.atFine().log("Event %s", event);
        try {
            var dispatcher = HytaleServer.get().getEventBus().dispatchFor((Class) event.getClass());
            if (dispatcher.hasListener()) {
                dispatcher.dispatch(event);
            }
        } catch (RuntimeException e) {
            LOGGER.atSevere().withCause(e).log("Dispatch of %s failed", event);
        }
    }
}
