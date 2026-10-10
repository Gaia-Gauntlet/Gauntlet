package com.gaiagauntlet.gauntlet.plugins.auto.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.hud.huds.HudWidgets;
import com.gaiagauntlet.gauntlet.core.hud.interfaces.HudDataSnapshot;
import com.gaiagauntlet.gauntlet.core.hud.interfaces.HudElement;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.SessionText;
import com.gaiagauntlet.gauntlet.plugins.auto.AutoPlugin;
import com.gaiagauntlet.gauntlet.plugins.auto.components.VoteComponent;
import com.gaiagauntlet.gauntlet.plugins.auto.utils.VoteUtils;
import com.gaiagauntlet.gauntlet.plugins.timer.utils.TimerUtils;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * The open next-game vote at the top of the screen, for players who closed the
 * vote page.
 */
public final class VoteHud implements HudElement {

    private final HudWidgets.Sent sent = new HudWidgets.Sent();

    @Nonnull
    @Override
    public String getId() {
        return "Vote";
    }

    @Nonnull
    @Override
    public String getMarkup() {
        return "Gauntlet/Plugins/" + AutoPlugin.ID + "/VoteHud.ui";
    }

    @Override
    public int getOrder() {
        return 110;
    }

    private record VoteData(
            VoteComponent vote) implements HudDataSnapshot {
    };

    @Override
    public VoteData gather(@Nonnull PlayerRef player, @Nullable GameSession session) {
        if (session == null) return null;
        var voteComp = VoteUtils.get(session);
        if (voteComp == null || voteComp.isClosed())
            return null;
        return new VoteData(voteComp);
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        sent.clear();
    }

    @Override
    public void render(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session,
            HudDataSnapshot data) {
        if (!(data instanceof VoteData voteComp)) {
            GaiaLog.atWarning().withSession(session)
                    .log("Attempted to update the Vote Hud with the incorrect snapshot!");
            return;
        }
        var vote = voteComp.vote;
        if (vote == null)
            return;
        sent.text(cmd, "#VoteTimer",
                TimerUtils.clock(vote.remainingSeconds()));

        var tally = vote.tally();
        String leading = null;
        int best = 0;
        for (var option : vote.getOptions()) {
            if (tally.get(option) > best) {
                best = tally.get(option);
                leading = option;
            }
        }
        var mine = vote.voteOf(player.getUuid());
        var line = leading == null ? "No votes yet." : "Leading: " + SessionText.game(leading) + ".";
        line += mine == null ? " Type /vote to pick a game." : " You voted " + SessionText.game(mine) + ".";
        sent.text(cmd, "#VoteLeader", line);
    }
}
