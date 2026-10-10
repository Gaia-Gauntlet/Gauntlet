package com.gaiagauntlet.gauntlet.plugins.gamestate.components;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponentType;
import lombok.Getter;
import lombok.Setter;

/**
 * Where a session's match is in its life cycle, the countdown for the current
 * phase, and the
 * standings once it ends. Kept in the session's game store on the hub world,
 * since the session
 * itself is locked while a game runs. Written on the hub thread through
 * MatchUtils. HUDs read it
 * from their refresh timer, so every field is safe to read from any thread.
 */
public final class MatchComponent implements GameComponent {

    public static final String ID = "MatchComponent";

    @Getter
    @Setter
    private static GameComponentType<@NotNull MatchComponent> componentType;

    @Setter 
    @Getter
    private volatile String state;

    public MatchComponent() {
        this("Idle");
    }

    public MatchComponent(String state) {
        this.state = state;
    }
}
