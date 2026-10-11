package com.gaiagauntlet.gauntlet.plugins.teams.systems;

import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.EliminatedComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.utils.TeamUtils;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/** Redraws the party and teams panels of a player's game when they are eliminated or brought back. */
public final class TeamHudEliminatedSystem extends RefChangeSystem<EntityStore, EliminatedComponent> {

    @Override
    public ComponentType<EntityStore, EliminatedComponent> componentType() {
        return EliminatedComponent.getComponentType();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull EliminatedComponent component,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        updateHuds(ref, store);
    }

    @Override
    public void onComponentSet(@Nonnull Ref<EntityStore> ref, @Nullable EliminatedComponent oldComponent,
            @Nonnull EliminatedComponent newComponent, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    }

    @Override
    public void onComponentRemoved(@Nonnull Ref<EntityStore> ref, @Nonnull EliminatedComponent component,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        updateHuds(ref, store);
    }

    private static void updateHuds(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        var player = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null)
            return;
        GameStore.withStore(player).ifPresent(game -> TeamUtils.updateHuds(game, Set.of()));
    }
}
