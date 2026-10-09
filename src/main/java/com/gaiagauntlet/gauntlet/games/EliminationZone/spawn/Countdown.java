package com.gaiagauntlet.gauntlet.games.EliminationZone.spawn;

import com.hypixel.hytale.logger.HytaleLogger;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * A one second countdown advanced by the world's tick. It has no thread of its own: a
 * ticking system feeds it the world's delta time, so every callback runs on the world thread, and
 * pause, set, skip, and cancel are plain state changes.
 */
public final class Countdown {

    /** Receives the seconds remaining and whether the countdown is paused. */
    public interface TickHandler {
        void tick(int remaining, boolean paused);
    }

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final String name;
    private final TickHandler onTick;
    /** Receives true when the countdown was cancelled rather than completed. */
    private final Consumer<Boolean> onEnd;

    @Getter private int remaining;
    private float accumulated;
    @Getter private boolean paused;
    @Getter private boolean finished;
    private boolean started;

    public Countdown(@Nonnull String name, int seconds, @Nonnull TickHandler onTick, @Nonnull Consumer<Boolean> onEnd) {
        this.name = name;
        this.remaining = Math.max(0, seconds);
        this.onTick = onTick;
        this.onEnd = onEnd;
    }

    /** Announces the starting value. The first decrement happens one second of tick time later. */
    public void start() {
        started = true;
        LOGGER.atInfo().log("Countdown %s started at %ds", name, remaining);
        notifyTick();
        if (remaining <= 0) {
            end(false);
        }
    }

    /** Advances by the world's delta time. Called every tick by the world controller. */
    public void tick(float dt) {
        if (!started || finished || paused) {
            return;
        }
        accumulated += dt;
        while (accumulated >= 1f && !finished) {
            accumulated -= 1f;
            remaining--;
            notifyTick();
            if (remaining <= 0) {
                end(false);
            }
        }
    }

    public void pause() {
        if (finished || paused) {
            return;
        }
        paused = true;
        notifyTick();
    }

    public void resume() {
        if (!paused || finished) {
            return;
        }
        paused = false;
        notifyTick();
    }

    public void set(int seconds) {
        if (finished) {
            return;
        }
        remaining = Math.max(1, seconds);
        accumulated = 0;
        notifyTick();
    }

    /** Ends the countdown now as if it reached zero. */
    public void skip() {
        if (finished) {
            return;
        }
        remaining = 0;
        notifyTick();
        end(false);
    }

    /** Stops the countdown; the end callback receives cancelled = true. */
    public void cancel() {
        if (!finished) {
            end(true);
        }
    }

    private void notifyTick() {
        try {
            onTick.tick(remaining, paused);
        } catch (RuntimeException e) {
            LOGGER.atWarning().withCause(e).log("Countdown %s tick handler failed", name);
        }
    }

    private void end(boolean cancelled) {
        finished = true;
        LOGGER.atInfo().log("Countdown %s %s", name, cancelled ? "cancelled" : "finished");
        try {
            onEnd.accept(cancelled);
        } catch (RuntimeException e) {
            LOGGER.atSevere().withCause(e).log("Countdown %s end handler failed", name);
        }
    }
}
