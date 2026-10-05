package com.gaiagauntlet.gauntlet.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.admin.AdminLog;
import com.gaiagauntlet.gauntlet.core.games.interfaces.GameController;
import com.gaiagauntlet.gauntlet.core.games.interfaces.UiGamePlugin;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.PageFactory;
import com.gaiagauntlet.gauntlet.core.ui.pages.AdminPage;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * The very thin big boi router
 * 90% of the business logic for this should exist within the GameController
 * 
 * The Orchestrator is simply there to route and standardize implementations
 */
public class GauntletOrchestrator {

    private static final Map<String, PageFactory> corePages = Map.of(AdminPage.ID, AdminPage::new);

    /**
     * Business rules because I have nowhere else to put them.
     * 1) All controller methods should be invoked on the HUB thread
     * 2) SessionId states should be cleared between games
     * 3) Games should be setup before any player is allowed to join
     * 4) Eventing needs
     *     a) Failure Events
     *     b) Session status updates
     * 5) threading needs
     *     a) player join
     *     b) player leave
     *     c) server shutdown
     *     d) server startup (load up from crashed server - attempt recovery?)
     */

    /**
     * Sets up a game to allow for sending players to and, later, starting the game
     * itself
     */
    public static CompletableFuture<GameController> setupGame(ComponentAccessor<EntityStore> accessor,
            String sessionId) {
        var sessionRes = GauntletUtils.sessionFor(sessionId);
        if (!sessionRes.isPresent() || !sessionRes.get().available()) {
            AdminLog.add("Unable to setup the session's game. The session is not in a valid state!");
            return CompletableFuture.completedFuture(null);
        }
        var session = sessionRes.get();

        var nextGameId = session.getNext();
        var gameRes = GameRegistry.getGame(nextGameId);
        if (!gameRes.isPresent()) {
            AdminLog.add("Game " + nextGameId + " is not registered, cannot set up!");
            return CompletableFuture.completedFuture(null);
        }
        var game = gameRes.get();

        // pass stuff to the game
        var future = game.setupGame(accessor, session);

        // ensure this runs AFTER the game is made, needs to finalize what the game
        // actually needs in order to be created
        return CompletableFuture.completedFuture(game);
    }

    /** Collects new admin tabs from every UI plugin and game, for one admin page */
    public static List<AdminTab> getAdminTabs() {
        var tabs = new ArrayList<AdminTab>();
        for (var plugin : GameRegistry.getPlugins(UiGamePlugin.class)) {
            tabs.addAll(plugin.getAdminTabs());
        }
        for (var gameId : GameRegistry.getGameIds()) {
            GameRegistry.getGame(gameId).ifPresent(game -> tabs.addAll(game.getAdminTabs()));
        }
        return tabs;
    }

    /** Finds a page by id among the core pages and the pages UI plugins provide */
    public static Optional<PageFactory> getPage(String pageId) {
        var core = corePages.get(pageId);
        if (core != null) return Optional.of(core);
        for (var plugin : GameRegistry.getPlugins(UiGamePlugin.class)) {
            var page = plugin.getPages().get(pageId);
            if (page != null) return Optional.of(page);
        }
        return Optional.empty();
    }

    /** Opens a page for the player. Every page is opened through here */
    public static void openPage(Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef,
            String pageId, @Nullable GameSession session) {
        var player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        var factory = getPage(pageId);
        if (!factory.isPresent()) {
            AdminLog.add("Page " + pageId + " is not registered, cannot open it!");
            return;
        }
        player.getPageManager().openCustomPage(ref, store, factory.get().create(playerRef, session));
    }
}
