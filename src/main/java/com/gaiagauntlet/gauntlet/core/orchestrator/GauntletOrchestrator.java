package com.gaiagauntlet.gauntlet.core.orchestrator;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;
import com.gaiagauntlet.gauntlet.core.events.GauntletEventRegistry;
import com.gaiagauntlet.gauntlet.core.events.events.*;
import com.gaiagauntlet.gauntlet.core.games.interfaces.UiGamePlugin;
import com.gaiagauntlet.gauntlet.core.games.registries.GameRegistry;
import com.gaiagauntlet.gauntlet.core.orchestrator.handlers.GameHandlers;
import com.gaiagauntlet.gauntlet.core.orchestrator.handlers.PlayerHandlers;
import com.gaiagauntlet.gauntlet.core.orchestrator.handlers.SessionHandlers;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.huds.GauntletHud;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.HudElement;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.PageFactory;
import com.gaiagauntlet.gauntlet.core.ui.pages.AdminPage;
import com.gaiagauntlet.gauntlet.core.ui.tabs.LogTab;
import com.gaiagauntlet.gauntlet.core.ui.tabs.SessionTab;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The very thin big boi router
 * 90% of the business logic for this should exist within the GameController
 * 
 * The Orchestrator is simply there to route and standardize implementations
 * 
 * All business-logic implementations should be within the handlers/
 */
public class GauntletOrchestrator {

    private static final Map<String, PageFactory> corePages = Map.of(AdminPage.ID, AdminPage::new);

    /** The HUD currently shown to each player, so a replaced or abandoned HUD stops refreshing */
    private static final Map<UUID, GauntletHud> huds = new ConcurrentHashMap<>();

    /**
     * Registers all of the events LATE so that they can be intercepted easily
     */
    public static void setupListeners(JavaPlugin plugin) {
        // session event handling
        GauntletEventRegistry.on(EventPriority.LAST, SessionEvent.class, wrap(SessionHandlers::handleSession));
        GauntletEventRegistry.on(EventPriority.LAST, SessionQueueEvent.class,
                wrap(SessionHandlers::handleSessionQueue));
        GauntletEventRegistry.on(EventPriority.LAST, NewSessionEvent.class, wrap(SessionHandlers::handleNewSession));

        // game event handling
        GauntletEventRegistry.on(EventPriority.LAST, GameEvent.class, wrap(GameHandlers::handleGame));
        GauntletEventRegistry.on(EventPriority.LAST, GameEndEvent.class, wrap(GameHandlers::handleGameEnd));

        // player event handling
        GauntletEventRegistry.on(EventPriority.LAST, PlayerGameEvent.class, wrap(PlayerHandlers::handleGamePlayer));
        GauntletEventRegistry.on(EventPriority.LAST, PlayerPartyEvent.class, wrap(PlayerHandlers::handlePartyPlayer));
        var registry = plugin.getEventRegistry();
        registry.register(PlayerConnectEvent.class, PlayerHandlers::onPlayerConnect);
        registry.registerGlobal(PlayerReadyEvent.class, PlayerHandlers::onPlayerReady);
        registry.register(PlayerDisconnectEvent.class, PlayerHandlers::onPlayerDisconnect);
    }

    /** Ensures the event handler is always run on the hub world */
    private static <T extends GauntletEvent> Consumer<T> wrap(@Nonnull BiConsumer<World, T> listener) {
        return (T event) -> {
            // always run events on the hub world
            var hubWorld = GauntletUtils.withHubWorld();
            GauntletUtils.run(hubWorld, () -> {
                listener.accept(hubWorld, event);
            });
        };
    }

    /** Collects new admin tabs from the orchestrator, every UI plugin and every game, for one admin page, in tab order */
    public static List<AdminTab> getAdminTabs() {
        var tabs = new ArrayList<AdminTab>(List.of(new SessionTab(), new LogTab()));
        for (var plugin : GameRegistry.getPlugins(UiGamePlugin.class)) {
            tabs.addAll(plugin.getAdminTabs());
        }
        for (var gameId : GameRegistry.getGameIds()) {
            GameRegistry.getGame(gameId).ifPresent(game -> tabs.addAll(game.getAdminTabs()));
        }
        tabs.sort(Comparator.comparingInt(AdminTab::getOrder).thenComparing(AdminTab::getId));
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
            GaiaLog.atWarning().log("Page " + pageId + " is not registered, cannot open it!");
            return;
        }
        player.getPageManager().openCustomPage(ref, store, factory.get().create(playerRef, session));
    }

    /** Collects new HUD elements from every UI plugin and every game, for one player's HUD, in draw order */
    public static List<HudElement> getHudElements() {
        var elements = new ArrayList<HudElement>();
        for (var plugin : GameRegistry.getPlugins(UiGamePlugin.class)) {
            elements.addAll(plugin.getHudElements());
        }
        for (var gameId : GameRegistry.getGameIds()) {
            GameRegistry.getGame(gameId).ifPresent(game -> elements.addAll(game.getHudElements()));
        }
        elements.sort(Comparator.comparingInt(HudElement::getOrder).thenComparing(HudElement::getId));
        return elements;
    }

    /** Shows the player HUD, replacing the one the player had. Every HUD is shown through here */
    public static void showHud(Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef) {
        var player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        var hud = new GauntletHud(playerRef);
        var previous = huds.put(playerRef.getUuid(), hud);
        if (previous != null) previous.stop();
        player.getHudManager().addCustomHud(playerRef, hud);
    }

    /** Stops the HUD of a player who left */
    public static void forgetHud(PlayerRef playerRef) {
        var hud = huds.remove(playerRef.getUuid());
        if (hud != null) hud.stop();
    }
}
