package com.gaiagauntlet.gauntlet.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.gaiagauntlet.gauntlet.core.games.interfaces.GameController;
import com.gaiagauntlet.gauntlet.core.games.interfaces.UiGamePlugin;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.PageFactory;
import com.gaiagauntlet.gauntlet.core.ui.pages.AdminPage;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

class GauntletOrchestratorTest {

    private static final PageFactory TEST_PAGE = (player, session) -> {
        throw new UnsupportedOperationException();
    };

    private record Tab(String id, int order) implements AdminTab {
        @Nonnull @Override public String getId() {
            return id;
        }

        @Nonnull @Override public String getPanel() {
            return "Test/" + id + ".ui";
        }

        @Override public int getOrder() {
            return order;
        }

        @Override public void bind(@Nonnull UIEventBuilder evt) {
        }
    }

    private static final class TestUiPlugin implements UiGamePlugin {
        @Override public String getId() {
            return "TestUiPlugin";
        }

        @Override public List<String> getDependencies() {
            return List.of();
        }

        @Override public void init(JavaPlugin plugin) {
        }

        @Override public List<AdminTab> getAdminTabs() {
            return List.of(new Tab("Zeta", 0), new Tab("Late", 100));
        }

        @Override public Map<String, PageFactory> getPages() {
            return Map.of("TestPage", TEST_PAGE);
        }
    }

    private static final class TestGame extends GameController {
        @Override public String getId() {
            return "TestGame";
        }

        @Override public CompletableFuture<Void> setupGame(ComponentAccessor<EntityStore> hubAccessor, GameSession session) {
            return CompletableFuture.completedFuture(null);
        }

        @Override public CompletableFuture<Void> cleanGame(ComponentAccessor<EntityStore> hubAccessor, GameSession session) {
            return CompletableFuture.completedFuture(null);
        }

        @Override public CompletableFuture<Void> playerJoin(ComponentAccessor<EntityStore> hubAccessor, String sessionId, PlayerRef player) {
            return CompletableFuture.completedFuture(null);
        }

        @Override public CompletableFuture<Void> playerLeave(ComponentAccessor<EntityStore> hubAccessor, String sessionId, PlayerRef player) {
            return CompletableFuture.completedFuture(null);
        }

        @Override public List<AdminTab> getAdminTabs() {
            return List.of(new Tab("Alpha", 0), new Tab("First", -10));
        }

        @Override public List<String> requiredPlugins() {
            return List.of();
        }
    }

    @BeforeAll
    static void register() {
        GameRegistry.registerPlugin("TestUiPlugin", null, TestUiPlugin::new);
        GameRegistry.registerGame("TestGame", TestGame::new);
    }

    @Test
    void collectsCoreTabsAndTabsFromPluginsAndGamesInOrder() {
        var ids = GauntletOrchestrator.getAdminTabs().stream().map(AdminTab::getId).toList();
        assertEquals(List.of("First", "Alpha", "Session", "Zeta", "Late", "Log"), ids);
    }

    @Test
    void givesEachCallFreshTabs() {
        var first = GauntletOrchestrator.getAdminTabs();
        var second = GauntletOrchestrator.getAdminTabs();
        for (int i = 0; i < first.size(); i++) {
            assertNotSame(first.get(i), second.get(i));
        }
    }

    @Test
    void findsCoreAndPluginPages() {
        assertTrue(GauntletOrchestrator.getPage(AdminPage.ID).isPresent());
        assertSame(TEST_PAGE, GauntletOrchestrator.getPage("TestPage").orElseThrow());
        assertTrue(GauntletOrchestrator.getPage("Missing").isEmpty());
    }
}
