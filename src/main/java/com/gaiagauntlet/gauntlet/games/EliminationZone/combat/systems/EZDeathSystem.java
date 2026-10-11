package com.gaiagauntlet.gauntlet.games.EliminationZone.combat.systems;

import com.gaiagauntlet.gauntlet.games.EliminationZone.components.EZPlayerComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.spectator.components.SpectatorComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public final class EZDeathSystem extends DeathSystems.OnDeathSystem {

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return EZPlayerComponent.getComponentType();
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent death, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        var playerComp = store.getComponent(ref, EZPlayerComponent.getComponentType());
        var world = store.getExternalData().getWorld();

        var sessionId = playerComp.getSessionId();
        var gameStore = GameStore.withStore(world, sessionId);
        
        // IDK WHAT TO DO WHEN PEOPLE DIE AND IT'S MAKING ME CRASH TF OUT :)

        // uhhh spectator i guess?
        store.addComponent(ref, SpectatorComponent.getComponentType(), new SpectatorComponent());
    }
}