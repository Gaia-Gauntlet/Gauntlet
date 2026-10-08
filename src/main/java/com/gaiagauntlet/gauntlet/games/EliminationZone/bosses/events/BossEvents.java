package com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.events;

import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.components.BossScalingComponent;

import javax.annotation.Nonnull;

/** Events the boss feature fires on the arena thread. */
public final class BossEvents {

    private BossEvents() {
    }

    public static final class Spawned extends GauntletEvent {
        private final BossScalingComponent boss;
        private final String zoneId;

        public Spawned(@Nonnull BossScalingComponent boss, @Nonnull String zoneId) {
            super();
            this.boss = boss;
            this.zoneId = zoneId;
        }

        @Nonnull
        public BossScalingComponent boss() {
            return boss;
        }

        @Nonnull
        public String zoneId() {
            return zoneId;
        }

        @Override
        public String toString() {
            return "BossEvents.Spawned[" + zoneId + ": " + boss.roleId() + "]";
        }
    }

    public static final class Defeated extends GauntletEvent {
        private final String bossId;

        public Defeated(@Nonnull String bossId) {
            super();
            this.bossId = bossId;
        }

        @Nonnull
        public String bossId() {
            return bossId;
        }

        @Override
        public String toString() {
            return "BossEvents.Defeated[" + bossId + "]";
        }
    }
}
