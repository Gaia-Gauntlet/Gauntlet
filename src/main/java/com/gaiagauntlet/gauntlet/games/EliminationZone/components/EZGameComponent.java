package com.gaiagauntlet.gauntlet.games.EliminationZone.components;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.plugins.auto.components.Standing;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponentType;

import lombok.Getter;
import lombok.Setter;

/** Logic and holds for the current game */
public class EZGameComponent implements GameComponent {
    @Getter @Setter private static GameComponentType<@NotNull EZGameComponent> componentType;
    public static final String ID = "EZGameComponent";
    /** List of participants, require X participants before the game can start */
    @Getter private int participants;
    @Getter @Setter private List<Standing> standings = List.of();

    public EZGameComponent() {

    }

    public void incrementParticipants() {
        participants++;
    }

    public void decrementParticipants() {
        participants--;
    }
}