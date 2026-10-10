package com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state;

import java.util.Set;

public enum EZStates {
    LOBBY(Set.of()),
    RUNNING(Set.of(LOBBY)),
    SUDDEN_DEATH(Set.of(RUNNING)),
    ENDED(Set.of(RUNNING, SUDDEN_DEATH)),
    ERROR(Set.of(LOBBY, RUNNING, SUDDEN_DEATH, ENDED));

    private Set<EZStates> validSources;

    private EZStates(Set<EZStates> transitionFrom) {
        validSources = transitionFrom;
    }

    public boolean to(EZStates destination) {
        return destination.validSources.contains(this);
    }
}
