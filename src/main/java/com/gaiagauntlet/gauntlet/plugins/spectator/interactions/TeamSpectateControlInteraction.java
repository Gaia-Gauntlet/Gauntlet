package com.gaiagauntlet.gauntlet.plugins.spectator.interactions;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.plugins.spectator.SpectatorCamera;
import com.gaiagauntlet.gauntlet.plugins.spectator.components.SpectatorComponent;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Bound to the spectator controls: moves the follow camera to the next or previous player the
 * spectating rules allow, or, for a spectator who may, toggles flying freely without following anyone.
 */
public final class TeamSpectateControlInteraction extends SimpleInstantInteraction {

    public static final String ID = "TeamSpectateControl";

    public enum Action { Previous, Next, Freefly }

    public static final BuilderCodec<TeamSpectateControlInteraction> CODEC = BuilderCodec
            .builder(TeamSpectateControlInteraction.class, TeamSpectateControlInteraction::new, SimpleInstantInteraction.CODEC)
            .appendInherited(new KeyedCodec<>("Action", new EnumCodec<>(Action.class)),
                    (interaction, action) -> interaction.action = action,
                    interaction -> interaction.action,
                    (interaction, parent) -> interaction.action = parent.action)
            .add()
            .build();

    @Nullable private Action action;

    private TeamSpectateControlInteraction() {
    }

    @Override
    protected void firstRun(
        @Nonnull InteractionType type,
        @Nonnull InteractionContext context,
        @Nonnull CooldownHandler cooldowns
    ) {
        var commandBuffer = context.getCommandBuffer();
        var spectatorRef = context.getOwningEntity();
        if (action == null || commandBuffer == null || spectatorRef == null) return;
        var store = spectatorRef.getStore();
        var spectatorPlayer = store.getComponent(spectatorRef, PlayerRef.getComponentType());
        if (spectatorPlayer == null) return;
        var session = GauntletUtils.sessionFor(spectatorPlayer).orElse(null);
        if (session == null) return;
        if (spectatorPlayer.getWorldUuid() == null) return;
        var world = Universe.get().getWorld(spectatorPlayer.getWorldUuid());
        if (world == null) return;

        var current = SpectatorCamera.following(spectatorRef);
        if (action == Action.Freefly) {
            if (!SpectatorCamera.canFreefly(world, session.getId(), spectatorRef, commandBuffer)) return;
            if (current != null) {
                SpectatorCamera.release(spectatorRef, commandBuffer);
                return;
            }
        }
        var next = SpectatorCamera.nextTarget(session.getId(), spectatorRef, commandBuffer, current, action != Action.Previous);
        if (next != null) {
            commandBuffer.putComponent(spectatorRef, SpectatorComponent.getComponentType(), new SpectatorComponent(next));
        }
    }
}
