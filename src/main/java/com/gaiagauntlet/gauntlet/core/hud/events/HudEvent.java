package com.gaiagauntlet.gauntlet.core.hud.events;

import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.hud.interfaces.HudDataSnapshot;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.world.World;

import lombok.Getter;

/** Abstract hud update event */
public final class HudEvent implements IEvent<Void> {
    @Nullable @Getter private final HudDataSnapshot hudData;
    @Getter private final String hudId;
    @Getter private final String sessionId;
    /** The world the hud update takes place in. Falls back to the current thread */
    @Nullable @Getter private final World world;

    public HudEvent(String sessionId, String hudId, HudDataSnapshot snapshot) {
        this(sessionId, hudId, snapshot, null);
    }
    public HudEvent(String sessionId, String hudId, HudDataSnapshot snapshot, World world) {
        hudData = snapshot;
        this.sessionId = sessionId;
        this.hudId = hudId;
        this.world = world;
    }
}
