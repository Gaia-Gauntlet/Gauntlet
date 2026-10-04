package com.gaiagauntlet.gauntlet.plugins.announcer.utils;

import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.utils.WorldUtils;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.interface_.EventTitleStyle;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import net.lordimass.assets.CameraSequenceAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Objects;

public final class Announcer {

    public static final String COLOR_INFO = "#7EC8FF";
    public static final String COLOR_SUCCESS = "#55FF55";
    public static final String COLOR_WARNING = "#FFAA33";
    public static final String COLOR_DANGER = "#FF5555";

    private Announcer() {}

    /** Chat line to every online player. */
    public static void chat(@Nonnull Message message) {
        for (var player : new ArrayList<>(Universe.get().getPlayers())) {
            player.sendMessage(message);
        }
    }

    /** Chat line to every player in the world. */
    public static void chat(World world, @Nonnull Message message) {
        for (var player : new ArrayList<>(world.getPlayerRefs())) {
            player.sendMessage(message);
        }
    }

    /** Large title and subtitle for everyone in the world, with an optional sound. Safe from any thread. */
    public static void title(@Nonnull World world, @Nonnull Message primary, @Nonnull Message secondary,
                             EventTitleStyle eventTitleStyle, @Nullable String soundEventId) {
        WorldUtils.run(world, () -> {
            for (var player : new ArrayList<>(world.getPlayerRefs())) {
                EventTitleUtil.showEventTitleToPlayer(
                    player,
                    primary, secondary,
                    eventTitleStyle, null,
                    EventTitleUtil.DEFAULT_DURATION,
                    EventTitleUtil.DEFAULT_FADE_DURATION, EventTitleUtil.DEFAULT_FADE_DURATION
                );
            }
            if (soundEventId != null) {
                sound(world, soundEventId);
            }
        });
    }

    /** Plays a 2D sound event to everyone in the world. Run on the world thread. */
    public static void sound(@Nonnull World world, @Nonnull String soundEventId) {
        var index = SoundEvent.getAssetMap().getIndex(soundEventId);
        if (index <= SoundEvent.EMPTY_ID) {
            GaiaLog.atWarning().log(String.format("Sound event %s does not exist", soundEventId));
            return;
        }
        SoundUtil.playSoundEvent2d(index, SoundCategory.UI, world.getEntityStore().getStore());
    }

    /** Plays a camera sequence from the CameraSequenceAssets mod for one player. */
    public static void cinematic(@Nonnull PlayerRef player, @Nonnull String sequence) {
        var seqAsset = CameraSequenceAsset.getAssetMap().getAsset(sequence);
        if (Objects.isNull(seqAsset)) {
            GaiaLog.atWarning().log("Couldn't find camera sequence " + sequence + "!");
            return;
        }
        seqAsset.play(player);
    }

    /** Plays a camera sequence for everyone in the world. */
    public static void cinematic(@Nonnull World world, @Nonnull String sequence) {
        for (var player : new ArrayList<>(world.getPlayerRefs())) {
            cinematic(player, sequence);
        }
    }

}
