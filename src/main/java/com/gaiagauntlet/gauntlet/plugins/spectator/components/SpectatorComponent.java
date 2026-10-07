package com.gaiagauntlet.gauntlet.plugins.spectator.components;

import com.gaiagauntlet.gauntlet.plugins.spectator.SpectatorCamera;
import com.gaiagauntlet.gauntlet.plugins.spectator.systems.SpectatingSystems;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentGameModeType;
import com.hypixel.hytale.server.core.modules.entity.component.Spectating;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Marks a player as spectating. Adding or replacing it retargets the follow camera. */
public final class SpectatorComponent implements Component<EntityStore> {

    /** The game mode asset with the spectate controls, at Server/Entity/GameMode/GGSpectator.json. */
    public static final String GAME_MODE = "GGSpectator";

    private static ComponentType<EntityStore, SpectatorComponent> type;

    @Nullable private final Ref<EntityStore> target;

    public SpectatorComponent() {
        this(null);
    }

    public SpectatorComponent(@Nullable Ref<EntityStore> target) {
        this.target = target;
    }

    public static void setType(@Nonnull ComponentType<EntityStore, SpectatorComponent> componentType) {
        type = componentType;
    }

    @Nonnull
    public static ComponentType<EntityStore, SpectatorComponent> getComponentType() {
        return type;
    }

    /** Who to follow, or null for whoever the spectating rules pick first. */
    @Nullable
    public Ref<EntityStore> target() {
        return target;
    }

    /**
     * Takes a player out of spectating, whatever left them there: this marker, the camera the server
     * tracks, or the spectator gamemode alone, which outlives a disconnect when the marker does not.
     * Returns true when the player was spectating. Runs on the player's world thread.
     */
    public static boolean clear(@Nonnull Ref<EntityStore> ref, @Nonnull ComponentAccessor<EntityStore> accessor) {
        if (accessor.getComponent(ref, type) != null) {
            // Removing the marker exits the gamemode and resets the camera through the change system.
            accessor.tryRemoveComponent(ref, type);
            return true;
        }
        var mode = accessor.getComponent(ref, PersistentGameModeType.getComponentType());
        if ((mode != null && GAME_MODE.equals(mode.getGameModeTypeId())) || Spectating.isSpectating(ref, accessor)) {
            SpectatorCamera.stop(ref, null, accessor);
            SpectatingSystems.unbindControls(ref, accessor);
            return true;
        }
        return false;
    }

    @Override
    public SpectatorComponent clone() {
        return new SpectatorComponent(target);
    }
}
