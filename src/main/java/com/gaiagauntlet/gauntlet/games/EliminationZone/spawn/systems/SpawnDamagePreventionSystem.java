package com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.systems;

import com.gaiagauntlet.gauntlet.games.EliminationZone.components.EZPlayerComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.components.SpawnProtectionComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SpawnDamagePreventionSystem extends DamageEventSystem {

    public SpawnDamagePreventionSystem() {
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType(), EZPlayerComponent.getComponentType());
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Damage event) {
        var playerComp = archetypeChunk.getComponent(index, EZPlayerComponent.getComponentType());
        if (playerComp == null) return;
        var playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        assert playerRef != null;
        assert playerRef.getWorldUuid() != null;
        var world = Universe.get().getWorld(playerRef.getWorldUuid());

        var gameStore = GameStore.withStore(world, playerComp.getSessionId()).orElse(null);
        if (gameStore == null) return; // not in the game
        var spawnProt = gameStore.get(SpawnProtectionComponent.getComponentType());
        if (spawnProt.isEmpty()) return; // not in a spawn protection
        event.setCancelled(true);
    }
}
