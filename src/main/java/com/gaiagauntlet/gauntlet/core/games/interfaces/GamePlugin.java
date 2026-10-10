package com.gaiagauntlet.gauntlet.core.games.interfaces;

import java.util.List;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;

/**
 * Standardized plugin interface
 * 
 * Add params to the methods as-needed. For now, they are empty to prevent param bloat 
 */
public interface GamePlugin {
    public String getId();

    /** The name admins see for this plugin. */
    public default String getDisplayName() {
        return getId();
    }

    /** Gets a list of required plugin IDs */
    public List<String> getDependencies();

    /** initializes the plugin itself */
    public void init(JavaPlugin plugin);
}
