package com.gaiagauntlet.gauntlet.core.games.interfaces;

import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;

/**
 * UiGamePlugin
 */
public interface UiGamePlugin extends GamePlugin {

    /** Admin tab or admin configurations */
    public AdminTab getAdminTab();
}
