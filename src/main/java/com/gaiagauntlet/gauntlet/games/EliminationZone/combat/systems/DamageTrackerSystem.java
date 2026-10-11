package com.gaiagauntlet.gauntlet.games.EliminationZone.combat.systems;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.games.EliminationZone.combat.components.EZCombatComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.components.EZPlayerComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class DamageTrackerSystem extends DamageEventSystem {

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getInspectDamageGroup();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), EZPlayerComponent.getComponentType());
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage event) {
        if (event.isCancelled() || !(event.getSource() instanceof Damage.EntitySource source)) {
            return;
        }
        var attackerRef = source.getRef();
        if (attackerRef == null || !attackerRef.isValid()) {
            return;
        }
        var victim = chunk.getComponent(index, PlayerRef.getComponentType());
        var playerComp = chunk.getComponent(index, EZPlayerComponent.getComponentType());
        var attacker = store.getComponent(attackerRef, PlayerRef.getComponentType());
        if (victim == null || playerComp == null || attacker == null) {
            return;
        }
        GameStore.withStore(store.getExternalData().getWorld(), playerComp.getSessionId())
                .ifPresent(game -> game.ensure(EZCombatComponent.getComponentType(), EZCombatComponent::new)
                        .recordDamage(victim.getUuid(), attacker.getUuid()));
    }
}
