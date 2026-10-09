package com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state;

import java.util.Set;

public enum EZStates {
    LOBBY(Set.of()),
    RUNNING(Set.of(LOBBY)),
    ELIMINATION(Set.of(RUNNING)),
    STOPPED(Set.of(RUNNING, ELIMINATION)),
    ERROR(Set.of(LOBBY, LOBBY, RUNNING, ELIMINATION, STOPPED));

    private Set<EZStates> validSources;

    private EZStates(Set<EZStates> transitionFrom) {
        validSources = transitionFrom;
    }

    public boolean to(EZStates destination) {
        return destination.validSources.contains(this);
    }
}
