package com.gaiagauntlet.gauntlet.core.interfaces;

import java.util.List;

import com.gaiagauntlet.gauntlet.core.session.components.SessionState;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;

/**
 * Standardized plugin interface
 * 
 * Add params to the methods as-needed. For now, they are empty to prevent param bloat 
 */
public abstract class GamePlugin {
    public abstract String getId();

    /** Gets a list of required plugin IDs */
    public abstract List<String> getDependencies();

    /** Installs the plugin into a game */
    public void install() {}

    /** Writes any state onto the session during the transition out of the game */
    public void writeSession(SessionState sessionObject) {}

    /** Admin tab or admin configurations */
    public AdminTab getAdminTab() { return null; };
    
    // add more as needed, I'm not entirely sure what else to put here.
}
