package com.gaiagauntlet.gauntlet.core.games.interfaces;

import java.util.List;
import java.util.Map;

import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.HudElement;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.PageFactory;

/**
 * UiGamePlugin
 */
public interface UiGamePlugin extends GamePlugin {

    /** New admin tabs for the dashboard. Called once per opened admin page */
    public List<AdminTab> getAdminTabs();

    /** Pages the orchestrator can open, keyed by page id */
    public default Map<String, PageFactory> getPages() {
        return Map.of();
    }

    /** New HUD elements for the player HUD. Called once per shown HUD */
    public default List<HudElement> getHudElements() {
        return List.of();
    }
}
