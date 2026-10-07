package com.gaiagauntlet.gauntlet.plugins.gamestate.components;

import java.util.List;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.core.session.components.SessionComponent;
import com.gaiagauntlet.gauntlet.core.session.components.SessionComponentType;
import com.gaiagauntlet.gauntlet.plugins.gamestate.constants.MatchState;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import lombok.Getter;
import lombok.Setter;

/**
 * Where a session's match is in its life cycle, the countdown for the current phase, and the
 * standings once it ends. Written on the hub thread through MatchUtils. HUDs read it from their
 * refresh timer, so every field is safe to read from any thread.
 */
public final class MatchComponent implements SessionComponent {

    public static final String ID = "MatchComponent";

    private static final long NOT_PAUSED = -1;

    public static final BuilderCodec<@NotNull MatchComponent> CODEC = BuilderCodec
            .builder(MatchComponent.class, MatchComponent::new)
            .append(new KeyedCodec<>("State", new EnumCodec<>(MatchState.class)),
                    (m, v) -> m.state = v == null ? MatchState.IDLE : v, m -> m.state)
            .add()
            .append(new KeyedCodec<>("EndsAt", Codec.LONG), (m, v) -> m.endsAt = v == null ? 0 : v, m -> m.endsAt)
            .add()
            .append(new KeyedCodec<>("PausedRemaining", Codec.LONG),
                    (m, v) -> m.pausedRemaining = v == null ? NOT_PAUSED : v, m -> m.pausedRemaining)
            .add()
            .append(new KeyedCodec<>("Standings", new ArrayCodec<>(Standing.CODEC, Standing[]::new)),
                    (m, v) -> m.standings = v == null ? List.of() : List.of(v),
                    m -> m.standings.toArray(new Standing[0]))
            .add()
            .build();


    @Getter @Setter private static SessionComponentType<MatchComponent> sessionComponentType;

    @Getter private volatile MatchState state = MatchState.IDLE;

    /** Epoch millis the countdown reaches zero, or 0 when there is no countdown. */
    private volatile long endsAt;

    /** Millis left on a paused countdown, or NOT_PAUSED. */
    private volatile long pausedRemaining = NOT_PAUSED;

    /** Best first. Empty until the match ends. */
    @Getter private volatile List<Standing> standings = List.of();

    public void setState(@Nonnull MatchState state) {
        this.state = state;
    }

    public boolean hasCountdown() {
        return endsAt > 0 || pausedRemaining != NOT_PAUSED;
    }

    public boolean isPaused() {
        return pausedRemaining != NOT_PAUSED;
    }

    /** Whole seconds left, rounded up so the display reaches 0 exactly when the countdown expires. */
    public int remainingSeconds(long now) {
        long millis = remainingMillis(now);
        return (int) ((millis + 999) / 1000);
    }

    public boolean isExpired(long now) {
        return endsAt > 0 && pausedRemaining == NOT_PAUSED && now >= endsAt;
    }

    private long remainingMillis(long now) {
        if (pausedRemaining != NOT_PAUSED) return pausedRemaining;
        if (endsAt <= 0) return 0;
        return Math.max(0, endsAt - now);
    }

    public void startCountdown(int seconds, long now) {
        pausedRemaining = NOT_PAUSED;
        endsAt = now + seconds * 1000L;
    }

    public void clearCountdown() {
        endsAt = 0;
        pausedRemaining = NOT_PAUSED;
    }

    public void pause(long now) {
        if (endsAt <= 0 || pausedRemaining != NOT_PAUSED) return;
        pausedRemaining = Math.max(0, endsAt - now);
    }

    public void resume(long now) {
        if (pausedRemaining == NOT_PAUSED) return;
        endsAt = now + pausedRemaining;
        pausedRemaining = NOT_PAUSED;
    }

    public void setStandings(@Nonnull List<Standing> standings) {
        this.standings = List.copyOf(standings);
    }
}
