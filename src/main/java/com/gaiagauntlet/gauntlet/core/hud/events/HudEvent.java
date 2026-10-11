package com.gaiagauntlet.gauntlet.core.hud.events;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.hud.interfaces.HudDataSnapshot;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import lombok.Getter;

/** Abstract hud update event */
public final class HudEvent implements IEvent<Void> {
    @Nullable @Getter private final HudDataSnapshot hudData;
    /** The element to redraw. A refresh with no id redraws every element */
    @Nullable @Getter private final String hudId;
    @Nullable @Getter private final String sessionId;
    /** Players targeted directly, for players no longer reachable through their session */
    @Nullable @Getter private final Collection<PlayerRef> players;
    /** Each HUD gathers its own data instead of drawing the event's snapshot */
    @Getter private final boolean refresh;
    /** The world the hud update takes place in. Falls back to the current thread */
    @Nullable @Getter private final World world;

    public HudEvent(String sessionId, String hudId, @Nullable HudDataSnapshot snapshot) {
        this(sessionId, hudId, snapshot, null);
    }

    public HudEvent(String sessionId, String hudId, @Nullable HudDataSnapshot snapshot, World world) {
        this(sessionId, null, hudId, snapshot, false, world);
    }

    private HudEvent(@Nullable String sessionId, @Nullable Collection<PlayerRef> players, @Nullable String hudId,
            @Nullable HudDataSnapshot snapshot, boolean refresh, @Nullable World world) {
        this.hudData = snapshot;
        this.sessionId = sessionId;
        this.players = players;
        this.hudId = hudId;
        this.refresh = refresh;
        this.world = world;
    }

    public static HudEvent refresh(@Nonnull String sessionId) {
        return refresh(sessionId, null);
    }

    public static HudEvent refresh(@Nonnull String sessionId, @Nullable String hudId) {
        return new HudEvent(sessionId, null, hudId, null, true, null);
    }

    public static HudEvent refresh(@Nonnull Collection<PlayerRef> players) {
        return new HudEvent(null, List.copyOf(players), null, null, true, null);
    }

    public void dispatch() {
        var dispatcher = HytaleServer.get().getEventBus().dispatchFor(HudEvent.class);
        if (dispatcher.hasListener()) {
            dispatcher.dispatch(this);
        }
    }
}
