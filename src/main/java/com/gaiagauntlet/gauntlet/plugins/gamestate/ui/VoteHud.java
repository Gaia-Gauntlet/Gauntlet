package com.gaiagauntlet.gauntlet.plugins.gamestate.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.SessionText;
import com.gaiagauntlet.gauntlet.core.ui.huds.HudWidgets;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.HudElement;
import com.gaiagauntlet.gauntlet.plugins.gamestate.GameStatePlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestate.utils.MatchUtils;
import com.gaiagauntlet.gauntlet.plugins.gamestate.utils.VoteUtils;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/** The open next-game vote at the top of the screen, for players who closed the vote page. */
public final class VoteHud implements HudElement {

    private final HudWidgets.Sent sent = new HudWidgets.Sent();

    @Nonnull @Override public String getId() {
        return "Vote";
    }

    @Nonnull @Override public String getMarkup() {
        return "Gauntlet/Plugins/" + GameStatePlugin.ID + "/VoteHud.ui";
    }

    @Override public int getOrder() {
        return 110;
    }

    @Override
    public boolean isVisible(@Nonnull PlayerRef player, @Nullable GameSession session) {
        return VoteUtils.isOpen(session);
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        sent.clear();
    }

    @Override
    public void render(@Nonnull UICommandBuilder cmd, @Nonnull PlayerRef player, @Nullable GameSession session) {
        var vote = VoteUtils.get(session);
        if (vote == null) return;
        sent.text(cmd, "#VoteTimer", MatchUtils.clock(vote.remainingSeconds(System.currentTimeMillis())));

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
