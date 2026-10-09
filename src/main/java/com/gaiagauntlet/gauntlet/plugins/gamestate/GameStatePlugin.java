package com.gaiagauntlet.gauntlet.plugins.gamestate;

import com.gaiagauntlet.gauntlet.core.games.interfaces.SimpleGamePlugin;
import com.gaiagauntlet.gauntlet.core.games.interfaces.UiGamePlugin;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.HudElement;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.PageFactory;
import com.gaiagauntlet.gauntlet.plugins.gamestate.components.MatchComponent;
import com.gaiagauntlet.gauntlet.plugins.auto.ui.MatchTab;
import com.gaiagauntlet.gauntlet.plugins.auto.ui.VotePage;
import com.gaiagauntlet.gauntlet.plugins.auto.utils.VoteUtils;
import com.gaiagauntlet.gauntlet.plugins.events.MatchEventsPlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestore.GameStorePlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameComponentRegistry;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.Map;

/** Simple state machine handler implementation */
public class GameStatePlugin implements SimpleGamePlugin, UiGamePlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "GameStatePlugin";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<String> getDependencies() {
        return List.of(GameStorePlugin.ID, MatchEventsPlugin.ID);
    }

    @Override
    public void setup(ComponentAccessor<EntityStore> accessor, String sessionId, String gameId) {
        
    }

    public void init(JavaPlugin host) {
        MatchComponent.setComponentType(
                GameComponentRegistry.register(MatchComponent.ID, MatchComponent.class));
    }

    @Override
    public List<AdminTab> getAdminTabs() {
        return List.of();
    }

    @Override
    public List<HudElement> getHudElements() {
        return List.of();
    }

    @Override
    public Map<String, PageFactory> getPages() {
        return Map.of(VoteUtils.PAGE_ID, VotePage::new);
    }
}
