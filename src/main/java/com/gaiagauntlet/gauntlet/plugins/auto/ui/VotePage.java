package com.gaiagauntlet.gauntlet.plugins.auto.ui;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.SessionText;
import com.gaiagauntlet.gauntlet.core.ui.events.AdminPageEvent;
import com.gaiagauntlet.gauntlet.core.ui.pages.GauntletPage;
import com.gaiagauntlet.gauntlet.core.ui.pages.Widgets;
import com.gaiagauntlet.gauntlet.plugins.auto.GameStatePlugin;
import com.gaiagauntlet.gauntlet.plugins.auto.components.VoteComponent;
import com.gaiagauntlet.gauntlet.plugins.auto.utils.VoteUtils;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * The next-game vote for the viewer's session: the time left, each game's share of the votes, and a
 * button per game. Once the vote closes it shows the winner and the buttons stop working.
 */
public final class VotePage extends GauntletPage {

    private static final String PAGE = "Gauntlet/Plugins/" + GameStatePlugin.ID + "/VotePage.ui";
    private static final String OPTION = "Gauntlet/Plugins/" + GameStatePlugin.ID + "/VoteOption.ui";

    @Nullable private final GameSession opened;

    /** The vote the rows were built for, or null when they need building. */
    @Nullable private VoteComponent built;
    /** Whether the header was last laid out for a closed vote, or null before it was laid out. */
    @Nullable private Boolean builtClosed;

    public VotePage(@Nonnull PlayerRef playerRef, @Nullable GameSession session) {
        super(playerRef, 2000);
        this.opened = session;
    }

    @Override
    protected void build(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        cmd.append(PAGE);
        Widgets.bind(evt, "#CloseButton", "page.close");
        built = null;
    }

    @Override
    protected void render(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        var session = session();
        var vote = VoteUtils.get(session);
        var closed = vote == null || vote.isClosed();
        if (vote != built || !Boolean.valueOf(closed).equals(builtClosed)) {
            built = vote;
            builtClosed = closed;
            buildRows(cmd, evt, vote);
            cmd.set("#TimerText.Visible", !closed);
            cmd.set("#TimerClock.Visible", !closed);
            if (!closed) {
                cmd.set("#TimerClock.Seconds", vote.remainingSeconds(System.currentTimeMillis()));
            }
            cmd.set("#Result.Text", vote == null ? "No vote right now"
                    : !closed ? ""
                    : vote.getWinner() == null ? "Vote closed" : "Up next: " + SessionText.game(vote.getWinner()));
        }
        if (vote == null) {
            cmd.set("#Summary.Text", "");
            return;
        }

        var players = Math.max(GauntletUtils.playersFor(session).size(), vote.totalVotes());
        cmd.set("#Summary.Text", vote.totalVotes() + " of " + players + " players voted");

        var tally = vote.tally();
        var mine = vote.voteOf(playerRef.getUuid());
        var options = vote.getOptions();
        for (int i = 0; i < options.size(); i++) {
            var option = options.get(i);
            var row = "#Options[" + i + "]";
            int count = tally.get(option);
            cmd.set(row + " #Bar.Value", vote.totalVotes() == 0 ? 0f : (float) count / vote.totalVotes());
            cmd.set(row + " #Count.Text", count + (count == 1 ? " vote" : " votes"));
            var picked = option.equals(mine);
            cmd.set(row + " #Pick.Text", picked ? "Voted" : "Vote");
            cmd.set(row + " #Pick.Disabled", vote.isClosed() || picked);
        }
    }

    private void buildRows(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nullable VoteComponent vote) {
        cmd.clear("#Options");
        var options = vote == null ? List.<String>of() : vote.getOptions();
        for (int i = 0; i < options.size(); i++) {
            var row = "#Options[" + i + "]";
            cmd.append("#Options", OPTION);
            cmd.set(row + " #Name.Text", SessionText.game(options.get(i)));
            Widgets.bindArg(evt, row + " #Pick", "vote.cast", options.get(i));
        }
    }

    @Nullable
    @Override
    protected Message handle(@Nonnull AdminPageEvent event) {
        if (!event.action().equals("vote.cast")) return null;
        var vote = VoteUtils.get(session());
        if (vote == null || vote.isClosed()) return Widgets.fail("The vote has closed");
        if (!vote.cast(playerRef.getUuid(), event.arg())) return Widgets.fail("That game is not on the vote");
        return Widgets.ok("You voted for " + SessionText.game(event.arg()));
    }

    /** The viewer's session now, or the one the page opened for if they have since left it. */
    @Nullable
    private GameSession session() {
        var current = GauntletUtils.sessionFor(playerRef).orElse(null);
        return current != null ? current : opened;
    }
}
