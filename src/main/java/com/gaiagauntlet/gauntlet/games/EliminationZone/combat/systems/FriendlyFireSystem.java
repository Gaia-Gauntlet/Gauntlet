package com.gaiagauntlet.gauntlet.games.EliminationZone.combat.systems;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.games.EliminationZone.components.EZPlayerComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.teams.utils.TeamUtils;
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

/** Blocks damage between teammates unless the game allows friendly fire. */
public final class FriendlyFireSystem extends DamageEventSystem {

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
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
        var playerComp = chunk.getComponent(index, EZPlayerComponent.getComponentType());
        var playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        var world = store.getExternalData().getWorld();

        var sessionId = playerComp.getSessionId();
        var gameStore = GameStore.withStore(world, sessionId).orElse(null);
        if (gameStore == null || EZGameConfig.get(gameStore).isFriendlyFireEnabled())
            return;

        var teamsList = TeamUtils.teamsOf(gameStore).orElse(null);
        var attackerRef = source.getRef();
        if (playerRef == null || teamsList == null || attackerRef == null || !attackerRef.isValid()) {
            return;
        }
        var attacker = store.getComponent(attackerRef, PlayerRef.getComponentType());
        if (attacker == null || attacker.getUuid().equals(playerRef.getUuid())) {
            return;
        }
        var victimTeam = teamsList.get(playerRef.getUuid());
        if (victimTeam == null || !victimTeam.contains(attacker.getUuid())) {
            return;
        }
        event.setCancelled(true);
    }
}
