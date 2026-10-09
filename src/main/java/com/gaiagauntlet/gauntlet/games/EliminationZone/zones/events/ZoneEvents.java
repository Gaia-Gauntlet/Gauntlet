package com.gaiagauntlet.gauntlet.games.EliminationZone.zones.events;

import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.ZoneDefinition;
import com.gaiagauntlet.gauntlet.plugins.events.events.MatchEvent;

import javax.annotation.Nonnull;

/** Events the zone feature fires on the arena thread as zones close. */
public final class ZoneEvents {

    private ZoneEvents() {
    }

    /** A zone started closing: its edge begins sweeping inward. */
    public static final class ClosingStarted extends MatchEvent {
        private final int step;
        private final ZoneDefinition zone;
        private final double durationSeconds;

        public ClosingStarted(String sessionId, int step, @Nonnull ZoneDefinition zone, double durationSeconds) {
            super(sessionId);
            this.step = step;
            this.zone = zone;
            this.durationSeconds = durationSeconds;
        }

        public int step() {
            return step;
        }

        @Nonnull
        public ZoneDefinition zone() {
            return zone;
        }

        public double durationSeconds() {
            return durationSeconds;
        }

        @Override
        public String toString() {
            return "ZoneEvents.ClosingStarted[step " + step + ": " + zone + " over " + durationSeconds + "s]";
        }
    }

    /** The closing zone seals in a few seconds. */
    public static final class Warning extends MatchEvent {
        private final ZoneDefinition zone;
        private final double secondsLeft;

        public Warning(String sessionId, @Nonnull ZoneDefinition zone, double secondsLeft) {
            super(sessionId);
            this.zone = zone;
            this.secondsLeft = secondsLeft;
        }

        @Nonnull
        public ZoneDefinition zone() {
            return zone;
        }

        public double secondsLeft() {
            return secondsLeft;
        }

        @Override
        public String toString() {
            return "ZoneEvents.Warning[" + zone + ": " + secondsLeft + "s left]";
        }
    }

    /** A zone sealed: its whole band is void now. */
    public static final class Closed extends MatchEvent {
        private final int step;
        private final ZoneDefinition zone;
        private final boolean last;

        public Closed(String sessionId, int step, @Nonnull ZoneDefinition zone, boolean last) {
            super(sessionId);
            this.step = step;
            this.zone = zone;
            this.last = last;
        }

        public int step() {
            return step;
        }

        @Nonnull
        public ZoneDefinition zone() {
            return zone;
        }

        public boolean last() {
            return last;
        }

        @Override
        public String toString() {
            return "ZoneEvents.Closed[step " + step + ": " + zone + (last ? " (last)" : "") + "]";
        }
    }
}
