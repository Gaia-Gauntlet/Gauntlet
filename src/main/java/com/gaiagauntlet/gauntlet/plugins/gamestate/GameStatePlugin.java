package com.gaiagauntlet.gauntlet.plugins.gamestate;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.games.interfaces.SimpleGamePlugin;
import com.gaiagauntlet.gauntlet.core.games.interfaces.UiGamePlugin;
import com.gaiagauntlet.gauntlet.core.session.registry.SessionRegistry;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.HudElement;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.PageFactory;
import com.gaiagauntlet.gauntlet.plugins.gamestate.components.MatchComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestate.components.VoteComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestate.ui.MatchTab;
import com.gaiagauntlet.gauntlet.plugins.gamestate.ui.VoteHud;
import com.gaiagauntlet.gauntlet.plugins.gamestate.ui.VotePage;
import com.gaiagauntlet.gauntlet.plugins.gamestate.utils.MatchUtils;
import com.gaiagauntlet.gauntlet.plugins.gamestate.utils.VoteUtils;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
        return List.of();
    }

    @Override
    public void setup(ComponentAccessor<EntityStore> accessor, String gameId) {
        
    }

    public void init(JavaPlugin host) {
        MatchComponent.setSessionComponentType(
                SessionRegistry.register(MatchComponent.ID, MatchComponent.class, MatchComponent.CODEC));
        VoteComponent.setSessionComponentType(
                SessionRegistry.register(VoteComponent.ID, VoteComponent.class, null));
        HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(GameStatePlugin::tick, 1, 1, TimeUnit.SECONDS);
    }

    /** Hands the countdown and vote checks to the hub thread. A throw would cancel the schedule, so nothing escapes. */
    private static void tick() {
        try {
            GauntletUtils.run(GauntletUtils.withHubWorld(), () -> {
                try {
                    MatchUtils.tick();
                    VoteUtils.tick();
                } catch (RuntimeException e) {
                    LOGGER.atWarning().withCause(e).log("Match tick failed");
                }
            });
        } catch (RuntimeException e) {
            // the universe is not up yet
        }
    }

    @Override
    public List<AdminTab> getAdminTabs() {
        return List.of(new MatchTab());
    }

    @Override
    public List<HudElement> getHudElements() {
        return List.of(new VoteHud());
    }

    @Override
    public Map<String, PageFactory> getPages() {
        return Map.of(VoteUtils.PAGE_ID, VotePage::new);
    }
}
