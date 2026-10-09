package com.gaiagauntlet.gauntlet.plugins.lobbycontroller.events;

import com.gaiagauntlet.gauntlet.plugins.events.events.MatchEvent;
import com.hypixel.hytale.server.core.universe.world.World;

import lombok.Getter;

public class ArenaLoadedEvent extends MatchEvent {

    @Getter 
    private World world;

    public ArenaLoadedEvent(String sessionId, World world) {
        super(sessionId);
        this.world = world;
    }
    
}
