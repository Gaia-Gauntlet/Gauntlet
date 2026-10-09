package com.gaiagauntlet.gauntlet.plugins.auto;

import java.util.List;
import java.util.Map;
import com.gaiagauntlet.gauntlet.core.games.interfaces.SimpleGamePlugin;
import com.gaiagauntlet.gauntlet.core.games.interfaces.UiGamePlugin;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.HudElement;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.PageFactory;
import com.gaiagauntlet.gauntlet.plugins.auto.components.VoteComponent;
import com.gaiagauntlet.gauntlet.plugins.auto.ui.MatchTab;
import com.gaiagauntlet.gauntlet.plugins.auto.ui.VoteHud;
import com.gaiagauntlet.gauntlet.plugins.auto.ui.VotePage;
import com.gaiagauntlet.gauntlet.plugins.auto.utils.VoteUtils;
import com.gaiagauntlet.gauntlet.plugins.gamestore.GameStorePlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameComponentRegistry;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class AutoPlugin implements SimpleGamePlugin, UiGamePlugin {
    public static final String ID = "AutoPlugin";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Auto";
    }

    @Override
    public List<String> getDependencies() {
        return List.of(GameStorePlugin.ID);
    }

    @Override
    public void setup(ComponentAccessor<EntityStore> accessor, String sessionId, String gameId) {

    }

    public void init(JavaPlugin host) {
        VoteComponent.setComponentType(GameComponentRegistry.register(VoteComponent.ID, VoteComponent.class));
    }

    /**
     * Hands the countdown and vote checks to the hub thread. A throw would cancel
     * the schedule, so nothing escapes.
     */
    // private static void tick() {
    //     try {
    //         GauntletUtils.run(GauntletUtils.withHubWorld(), () -> {
    //             try {
    //                 MatchUtils.tick();
    //                 VoteUtils.tick();
    //             } catch (RuntimeException e) {
    //                 LOGGER.atWarning().withCause(e).log("Match tick failed");
    //             }
    //         });
    //     } catch (RuntimeException e) {
    //         // the universe is not up yet
    //     }
    // }

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
