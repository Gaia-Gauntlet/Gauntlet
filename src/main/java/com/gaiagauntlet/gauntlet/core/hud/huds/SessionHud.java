package com.gaiagauntlet.gauntlet.core.hud.huds;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.hud.interfaces.HudDataSnapshot;
import com.gaiagauntlet.gauntlet.core.hud.interfaces.HudElement;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.SessionText;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * The viewer's session in the top left corner: the game it is playing, its
 * status, and the games
 * queued after it. Steps aside while a match is played out in the arena, where
 * games draw their own
 * HUD in that corner.
 */
public final class SessionHud implements HudElement {

    private static final int TOP = 12;
    private static final int LEFT = 12;
    private static final int WIDTH = 260;
    private static final int HEIGHT = 102;
    private static final int HINT_HEIGHT = 58;
    private static final int UP_NEXT_SHOWN = 3;

    private final HudWidgets.Sent sent = new HudWidgets.Sent();

    /**
     * Whether the panel is laid out for a session, or null before the first render.
     */
    @Nullable
    private Boolean builtForSession;

    @Nonnull
    @Override
    public String getId() {
        return "Session";
    }

    @Nonnull
    @Override
    public String getMarkup() {
        return "Gauntlet/Hud/SessionHud.ui";
    }

    @Override
    public int getOrder() {
        return 5;
    }

    public record SessionHudSnapshot(boolean inSession) implements HudDataSnapshot {
    }

    @Override
    public HudDataSnapshot gather(@Nonnull PlayerRef player, @Nullable GameSession session) {
        // return !MatchUtils.inArena(session);
        var inSession = session != null && session.available();
        if (inSession)
            return new SessionHudSnapshot(inSession);
        return null;
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        sent.clear();
        builtForSession = null;
    }

    @Override
    public boolean render(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session,
            @Nonnull HudDataSnapshot data) {
        var inSession = session != null;
        if (!Boolean.valueOf(inSession).equals(builtForSession)) {
            builtForSession = inSession;
            HudWidgets.anchorLeft(cmd, "#SessionPanel", TOP, LEFT, WIDTH, inSession ? HEIGHT : HINT_HEIGHT);
            cmd.set("#SessionRows.Visible", inSession);
            cmd.set("#SessionHint.Visible", !inSession);
        }

        if (!inSession) {
            sent.text(cmd, "#SessionTitle", "Not in a session");
            return true;
        }
        sent.text(cmd, "#SessionTitle", session.getId());
        sent.text(cmd, "#SessionNow", SessionText.game(session.getCurrentGame()));
        sent.text(cmd, "#SessionState", SessionText.state(session));
        var next = SessionText.upNext(session, UP_NEXT_SHOWN);
        sent.text(cmd, "#SessionNext", next.isEmpty() ? "Nothing queued" : String.join(", ", next));
        return true;
    }
}
