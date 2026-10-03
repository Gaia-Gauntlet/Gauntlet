package com.gaiagauntlet.gauntlet.core.events;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

/**
 * Gauntlet Events serve as a way to suggest mutations / changes to the game
 * through a single pipeline
 */
public class GauntletEventRegistry {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static EventRegistry registry = null;

    public static void setup(JavaPlugin plugin) {
        registry = plugin.getEventRegistry();
    }

    public static <T extends GauntletEvent> void on(@Nonnull Class<T> type, @Nonnull Consumer<T> listener) {
        on(null, type, listener);
    }

    public static <T extends GauntletEvent> void on(EventPriority priority, @Nonnull Class<T> type,
            @Nonnull Consumer<T> listener) {
        if (registry == null) {
            LOGGER.atSevere().log("Failed to listen for event because registry is not setup");
            return;
        }
        if (priority == null) {
            priority = EventPriority.NORMAL;
        }
        registry.registerGlobal(priority, type, event -> {
            if (!GauntletUtils.withHubWorld().isInThread()) {
                LOGGER.atSevere().log("Event %s was dispatched outside of the hub thread! Mods must use GauntletEvents.dispatch", type.getSimpleName());
                return;
            }
            try {
                listener.accept(event);
            } catch (RuntimeException e) {
                LOGGER.atSevere().withCause(e).log("Listener for %s failed", type.getSimpleName());
            }
        });
    }

    public static void dispatch(@Nonnull GauntletEvent event) {
        // ensure on hub world
        GauntletUtils.run(GauntletUtils.withHubWorld(), () -> {
            LOGGER.atFine().log("Event %s", event);
            try {
                var dispatcher = HytaleServer.get().getEventBus().dispatchFor((Class) event.getClass());
                if (dispatcher.hasListener()) {
                    dispatcher.dispatch(event);
                }
            } catch (RuntimeException e) {
                LOGGER.atSevere().withCause(e).log("Dispatch of %s failed", event);
            }
        });
    }
}
