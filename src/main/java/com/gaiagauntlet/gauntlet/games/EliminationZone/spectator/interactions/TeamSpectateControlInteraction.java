package com.gaiagauntlet.gauntlet.games.EliminationZone.spectator.interactions;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Bound to the spectator controls: moves the follow camera to the next or previous player the
 * spectating rules allow, or, for a spectator who may, toggles flying freely without following anyone.
 */
public final class TeamSpectateControlInteraction extends SimpleInstantInteraction {

    public static final String ID = "GG_TeamSpectateControl";

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
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldowns) {
        var commandBuffer = context.getCommandBuffer();
        var spectator = context.getOwningEntity();
        if (action == null || commandBuffer == null || spectator == null) {
            return;
        }
//        var marker = commandBuffer.getComponent(spectator, ArenaMarker.getComponentType());
//        var store = GlobalStore.find();
//        if (marker == null || store == null) {
//            return;
//        }
//        var game = store.game(marker.gameId()).orElse(null);
//        if (game == null) {
//            return;
//        }
//        var current = SpectatorCamera.following(spectator);
//        if (action == Action.Freefly) {
//            if (!SpectatorCamera.canFreefly(game, spectator, commandBuffer)) {
//                return;
//            }
//            if (current != null) {
//                SpectatorCamera.release(spectator, commandBuffer);
//                return;
//            }
//        }
//        var next = SpectatorCamera.nextTarget(game, spectator, commandBuffer, current, action != Action.Previous);
//        if (next != null) {
//            commandBuffer.putComponent(spectator, Spectator.getComponentType(), new Spectator(next));
//        }
    }
}
