package com.gaiagauntlet.gauntlet.core.games.interfaces;

import java.util.List;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

/**
 * Standardized plugin interface
 * 
 * Add params to the methods as-needed. For now, they are empty to prevent param bloat 
 */
public abstract class GamePlugin {
    public abstract String getId();

    /** Gets a list of required plugin IDs */
    public List<String> getDependencies() { return List.of(); };

    /** Installs the plugin into a game */
    public void install() {}

    /** Writes any state onto the session during the transition out of the game */
    public void writeSession(GameSession sessionObject) {}

    /** Admin tab or admin configurations */
    public AdminTab getAdminTab() { return null; };

    /** Initialisation of resources and components on the respective stores */
    public abstract void setup(JavaPlugin host);
}
