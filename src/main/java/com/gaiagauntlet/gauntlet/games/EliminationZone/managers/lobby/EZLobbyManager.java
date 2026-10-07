package com.gaiagauntlet.gauntlet.games.EliminationZone.managers.lobby;

import java.util.concurrent.CompletableFuture;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.interfaces.LobbyManager;
import com.gaiagauntlet.gauntlet.utils.WorldUtils;
import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class EZLobbyManager implements LobbyManager {

    @Override
    public CompletableFuture<World> setupWorld(ComponentAccessor<EntityStore> hubAccessor, String sessionId) {
        var hubWorld = hubAccessor.getExternalData().getWorld();
        var worldFuture = new CompletableFuture<World>();
        var session = GauntletUtils.sessionFor(sessionId);
        /* GET CONFIG - Lordi's branch currently has the config logic. Hardcoding for now */
        var templateId = "EZGameWorld";
        WorldUtils.spawnInstance(templateId, hubWorld, WorldUtils.spawnOf(hubWorld)).whenComplete((world, err) -> {
            worldFuture.complete(hubWorld);
        });

        return worldFuture;
    }

    @Override 
    public CompletableFuture<Void> cleanWorld(World arenaWorld) {
        InstancesPlugin.safeRemoveInstance(arenaWorld);
        return CompletableFuture.completedFuture(null);
    };

    @Override
    public void onJoin(Ref<EntityStore> ref, ComponentAccessor<EntityStore> accessor, String sessionId) {
        // stuffs
    }
}
