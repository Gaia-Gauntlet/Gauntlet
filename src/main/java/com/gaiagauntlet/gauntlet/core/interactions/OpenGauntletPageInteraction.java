package com.gaiagauntlet.gauntlet.core.interactions;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.orchestrator.GauntletOrchestrator;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class OpenGauntletPageInteraction extends SimpleInstantInteraction {

    public static final String ID = "OpenGauntletPage";

    public static final BuilderCodec<OpenGauntletPageInteraction> CODEC = BuilderCodec
            .builder(OpenGauntletPageInteraction.class, OpenGauntletPageInteraction::new, SimpleInstantInteraction.CODEC)
            .appendInherited(new KeyedCodec<>("Page", Codec.STRING),
                    (interaction, action) -> interaction.pageId = action,
                    interaction -> interaction.pageId,
                    (interaction, parent) -> interaction.pageId = parent.pageId)
            .add()
            .build();

    @Nullable private String pageId;

    private OpenGauntletPageInteraction() {
    }

    @Override
    protected void firstRun(
        @Nonnull InteractionType type,
        @Nonnull InteractionContext context,
        @Nonnull CooldownHandler cooldowns
    ) {
        var ref = context.getEntity();
        var store = ref.getStore();
        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;

        var session = GauntletUtils.sessionFor(playerRef).orElse(null);
        GauntletOrchestrator.openPage(store, ref, playerRef, pageId, session);
    }
}
