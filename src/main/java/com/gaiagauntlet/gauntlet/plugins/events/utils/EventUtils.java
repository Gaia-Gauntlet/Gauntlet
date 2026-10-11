package com.gaiagauntlet.gauntlet.plugins.events.utils;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.plugins.events.assets.GameEventConfig;
import com.gaiagauntlet.gauntlet.plugins.events.assets.GameEvents;
import com.gaiagauntlet.gauntlet.plugins.events.components.GameEventComponent;
import com.gaiagauntlet.gauntlet.plugins.events.events.SequenceEvent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.hypixel.hytale.server.core.HytaleServer;

public final class EventUtils {

    private EventUtils() {
    }

    public static Optional<GameEventComponent> get(@Nullable GameEcs game) {
        return game == null ? Optional.empty() : game.get(GameEventComponent.getComponentType());
    }

    public static void setup(@Nonnull GameEcs game, @Nullable GameEvents events) {
        game.ensure(GameEventComponent.getComponentType(), GameEventComponent::new).hydrateFrom(events);
    }

    public static void fire(@Nonnull GameEcs game, @Nonnull GameEventConfig config) {
        var dispatcher = HytaleServer.get().getEventBus().dispatchFor(SequenceEvent.class, config.getId());
        if (!dispatcher.hasListener()) {
            GaiaLog.atWarning().withSession(game.getSessionId()).log("No handler for game event " + config.getId());
            return;
        }
        GaiaLog.atInfo().withSession(game.getSessionId()).log("Dispatching game event: " + config.describe());
        dispatcher.dispatch(new SequenceEvent(game.getSessionId(), config, game));
    }

    @Nonnull
    public static GameEventConfig fireNext(@Nonnull GameEcs game) {
        var next = running(game).popNext();
        if (next == null) {
            throw new IllegalStateException("No scheduled events are left");
        }
        fire(game, next.getGameConfig());
        return next.getGameConfig();
    }

    public static void queue(@Nonnull GameEcs game, @Nonnull GameEventConfig config, double inSeconds) {
        running(game).queue(config, inSeconds);
    }

    public static void pause(@Nonnull GameEcs game) {
        running(game).pause();
    }

    public static void resume(@Nonnull GameEcs game) {
        running(game).resume();
    }

    public static void delay(@Nonnull GameEcs game, double seconds) {
        if (seconds <= 0) {
            throw new IllegalArgumentException("The delay needs at least one second");
        }
        running(game).delay(seconds);
    }

    public static void cancel(@Nonnull GameEcs game, int index, double scheduledTime) {
        if (!running(game).cancel(index, scheduledTime)) {
            throw new IllegalStateException("The queue changed before the cancel arrived, try again");
        }
    }

    @Nonnull
    private static GameEventComponent running(@Nonnull GameEcs game) {
        return get(game).orElseThrow(() -> new IllegalStateException("Events are not running in this game"));
    }
}
