package com.gaiagauntlet.gauntlet.plugins.auto.utils;

import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.msg;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.hud.events.HudEvent;
import com.gaiagauntlet.gauntlet.core.orchestrator.GauntletOrchestrator;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.plugins.auto.components.VoteComponent;
import com.gaiagauntlet.gauntlet.plugins.auto.components.assets.SessionPreset;
import com.gaiagauntlet.gauntlet.utils.WorldUtils;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * Runs a session's vote on its next preset during the intermission. Opening it tells everyone in the
 * session to vote with /vote. Changes are made on the hub thread while the session is between games
 */
public final class VoteUtils {

    public static final String PAGE_ID = "Vote";

    private VoteUtils() {
    }

    @Nullable
    public static VoteComponent get(@Nullable GameSession session) {
        return session == null ? null : session.get(VoteComponent.getComponentType()).orElse(null);
    }

    public static boolean isOpen(@Nullable GameSession session) {
        var vote = get(session);
        return vote != null && !vote.isClosed();
    }

    public static void open(@Nonnull GameSession session, @Nonnull List<String> options) {
        session.put(VoteComponent.getComponentType(), new VoteComponent(options));
        tell(session, msg("server.gg.plugins.auto.vote.open"));
        HudEvent.refresh(session.getId()).dispatch();
    }

    /** Ends the voting early and locks the winner. The intermission keeps counting down */
    public static boolean close(@Nonnull GameSession session) {
        var vote = get(session);
        if (vote == null || vote.isClosed())
            return false;
        var winner = vote.leader(ThreadLocalRandom.current());
        vote.close(winner);
        tell(session, winner == null ? msg("server.gg.plugins.auto.vote.none")
                : msg("server.gg.plugins.auto.vote.won").param("preset", title(winner))
                        .param("votes", vote.tally().get(winner)));
        HudEvent.refresh(session.getId()).dispatch();
        return true;
    }

    /** The translated title of a preset, or its id when the preset is not loaded */
    @Nonnull
    public static Message title(@Nonnull String presetId) {
        var preset = SessionPreset.getAssetMap().get(presetId);
        return preset == null ? Message.raw(presetId) : Message.translation(preset.getTitle());
    }

    public static void tell(@Nonnull GameSession session, @Nonnull Message message) {
        for (var player : GauntletUtils.playersFor(session)) {
            player.sendMessage(message);
        }
    }
}
