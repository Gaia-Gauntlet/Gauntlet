package com.gaiagauntlet.gauntlet.plugins.timer;

import java.util.List;

import com.gaiagauntlet.gauntlet.core.games.interfaces.UiGamePlugin;
import com.gaiagauntlet.gauntlet.core.hud.interfaces.HudElement;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.plugins.events.MatchEventsPlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestore.GameStorePlugin;
import com.gaiagauntlet.gauntlet.plugins.timer.systems.GameTimerSystem;
import com.gaiagauntlet.gauntlet.plugins.timer.ui.TimerHud;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

public class TimerPlugin implements UiGamePlugin {

    public static final String ID = "TimerPlugin";
    private static final List<String> dependancies = List.of(GameStorePlugin.ID, MatchEventsPlugin.ID);

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<String> getDependencies() {
        return dependancies;
    }

    @Override
    public void init(JavaPlugin plugin) {
        plugin.getEntityStoreRegistry().registerSystem(new GameTimerSystem());
    }

    @Override
    public List<AdminTab> getAdminTabs() {
        return List.of();
    }

    @Override
    public List<HudElement> getHudElements() {
        // TODO: Actually finish the timerHud
        return List.of(new TimerHud());
    }

}
