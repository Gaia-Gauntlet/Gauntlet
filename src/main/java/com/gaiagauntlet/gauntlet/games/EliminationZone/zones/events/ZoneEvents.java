package com.gaiagauntlet.gauntlet.games.EliminationZone.zones.events;

import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.ZoneDefinition;

import javax.annotation.Nonnull;

/** Events the zone feature fires on the arena thread as zones close. */
public final class ZoneEvents {

    private ZoneEvents() {
    }

    /** A zone started closing: its edge begins sweeping inward. */
    public static final class ClosingStarted extends GauntletEvent {
        private final int step;
        private final ZoneDefinition zone;
        private final double durationSeconds;

        public ClosingStarted(int step, @Nonnull ZoneDefinition zone, double durationSeconds) {
            super();
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
    }

    /** The closing zone seals in a few seconds. */
    public static final class Warning extends GauntletEvent {
        private final ZoneDefinition zone;
        private final double secondsLeft;

        public Warning(@Nonnull ZoneDefinition zone, double secondsLeft) {
            super();
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
    }

    /** A zone sealed: its whole band is void now. */
    public static final class Closed extends GauntletEvent {
        private final int step;
        private final ZoneDefinition zone;
        private final boolean last;

        public Closed(int step, @Nonnull ZoneDefinition zone, boolean last) {
            super();
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
    }
}
