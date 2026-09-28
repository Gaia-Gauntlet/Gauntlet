package com.gaiagauntlet.gauntlet.core;

import java.util.concurrent.ConcurrentHashMap;

import com.gaiagauntlet.gauntlet.core.session.components.SessionState;

/**
 * The very thin big boi router
 * 90% of the business logic for this should exist within the GameController
 * 
 * The Orchestrator is simply there to route and standardize implementations
 */
public class GauntletOrchestrator {
    private static ConcurrentHashMap<String, SessionState> activeGames = new ConcurrentHashMap<>();


}
