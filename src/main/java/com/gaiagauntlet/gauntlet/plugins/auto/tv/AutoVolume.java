package com.gaiagauntlet.gauntlet.plugins.auto.tv;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.events.GauntletEventRegistry;
import com.gaiagauntlet.gauntlet.core.events.events.PlayerGameEvent;
import com.gaiagauntlet.gauntlet.plugins.auto.utils.AutoUtils;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/** Sends the player who set the volume off into the Default session */
public class AutoVolume extends TriggerEffect {
    public static final String ID = "GauntletAutoJoin";

    @Nonnull
    public static final BuilderCodec<@NotNull AutoVolume> CODEC = BuilderCodec
            .builder(AutoVolume.class, AutoVolume::new, BASE_CODEC)
            .build();

    @Override
    public void execute(@Nonnull TriggerContext context) {
        var ref = context.getEntityRef();
        if (ref == null || !ref.isValid())
            return;
        var player = context.getStore().getComponent(ref, PlayerRef.getComponentType());
        if (player == null)
            return;
        GauntletUtils.withHubWorld().execute(() -> {
            var current = GauntletUtils.sessionFor(player).orElse(null);
            if (current != null && current.getId().equals(AutoUtils.DEFAULT_SESSION))
                return;
            AutoUtils.ensureDefaultSession();
            GauntletEventRegistry.dispatch(PlayerGameEvent.Add(player, AutoUtils.DEFAULT_SESSION)
                    .onComplete(result -> player.sendMessage(result.toMessage())));
        });
    }
}
