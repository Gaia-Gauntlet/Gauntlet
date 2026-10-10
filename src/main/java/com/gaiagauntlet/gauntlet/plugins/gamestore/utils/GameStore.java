package com.gaiagauntlet.gauntlet.plugins.gamestore.utils;

import java.util.Optional;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.party.utils.PartyUtils;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.ContextComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.resource.WorldGameStore;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class GameStore {
    public static WorldGameStore withResource(ComponentAccessor<EntityStore> accessor) {
        return accessor.getResource(WorldGameStore.getResourceType());
    }

    public static WorldGameStore withResource(World world) {
        return withResource(world.getEntityStore().getStore());
    }

    public static Optional<GameEcs> withStore(ComponentAccessor<EntityStore> accessor, String sessionId) {
        return withResource(accessor).get(sessionId);
    }

    public static GameEcs ensureStore(ComponentAccessor<EntityStore> accessor, String sessionId) {
        return withResource(accessor).create(sessionId);
    }

    public static GameEcs ensureStore(World accessor, String sessionId) {
        return withResource(accessor).create(sessionId);
    }

    public static Optional<GameEcs> withStore(World world, String sessionId) {
        return withResource(world).get(sessionId);
    }

    public static Optional<GameEcs> withStore(PlayerRef player) {
        // unavailable = in a game. get the id
        var sessionId = GauntletUtils.sessionFor(player).filter(session -> !session.available()).map(session -> session.getId()).orElse(null);
        if (sessionId == null) return Optional.empty();
        return withStore(player, sessionId);
    }
    public static Optional<GameEcs> withStore(PlayerRef player, String sessionId) {
        var world = PlayerUtils.worldOf(player);
        return withStore(world, sessionId);
    }

    public static ContextComponent getContext(GameEcs store) {
        return store.get(ContextComponent.getComponentType()).orElseGet(() -> {
            var sessionId = store.getSessionId();
            var session = GauntletUtils.sessionFor(sessionId).orElseThrow(() -> new IllegalStateException(
                        "Attempted to access the state for " + sessionId + " but it does not exist"));

            if (session.available())
                throw new IllegalStateException(
                        "Attempted to access the state for " + sessionId + " but it is not active");

            // get the world - technically this codepath is very unlikely, i dont feel like doing a world lookup rn T-T
            var players = GauntletUtils.playersFor(session);

            World world = players.stream()
                    .findFirst()
                    .map(PlayerUtils::worldOf)
                    .orElseThrow(() -> new IllegalStateException(
                            "Attempted to access the world for " + sessionId + " but it has no players"));

            var ctx = new ContextComponent(world, sessionId, session.getCurrentGame());
            store.put(ContextComponent.getComponentType(), ctx);
            return ctx;
        });
    }
}
