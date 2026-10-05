package com.gaiagauntlet.gauntlet.plugins.gamestate.ui;

import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.error;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.events.AdminPageEvent;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.core.ui.pages.AdminPage;
import com.gaiagauntlet.gauntlet.core.ui.pages.Widgets;
import com.gaiagauntlet.gauntlet.plugins.gamestate.GameStatePlugin;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

/** Where the match is, its countdown, the life cycle actions, and preflight. */
public final class MatchTab implements AdminTab {

    @Nonnull @Override public String getId() {
        return "Match";
    }

    @Nonnull @Override public String getPanel() {
        return "Gauntlet/Plugins/" + GameStatePlugin.ID + "/MatchPanel.ui";
    }

    @Override public int getOrder() {
        return 10;
    }

    @Override
    public void bind(@Nonnull UIEventBuilder evt) {
        Widgets.bind(evt, "#TimerSkip", "match.skip");
        Widgets.bind(evt, "#TimerPause", "match.pause");
        Widgets.bind(evt, "#TimerResume", "match.resume");
        Widgets.bindValues(evt, "#TimerSet", "match.timer", "", Map.of("@Num", "#TimerSeconds.Value"));
        Widgets.bind(evt, "#MatchStart", "match.start");
        Widgets.bind(evt, "#MatchPortal", "match.portal");
        Widgets.bind(evt, "#MatchTransfer", "match.transfer");
        Widgets.bind(evt, "#MatchSuddenDeath", "match.suddendeath");
        Widgets.bind(evt, "#MatchEnd", "match.end");
        Widgets.bind(evt, "#MatchStop", "match.stop");
        Widgets.bind(evt, "#ResetScores", "match.resetscores");
    }

    @Override
    public void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nullable GameSession session) {
        Widgets.field(cmd, "HubField", "N/A");
        Widgets.field(cmd, "LobbiesField", "N/A");
        Widgets.field(cmd, "ArenaField", "N/A");
        Widgets.field(cmd, "PortalField", "N/A");
        Widgets.field(cmd, "TimerField", "N/A");
        Widgets.field(cmd, "AliveField", "no match");
        Widgets.fillList(cmd, "PreflightList", List.of(), "Nothing to check");
    }

    @Override
    public void render(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nullable GameSession session) {
        Widgets.field(cmd, "MatchGameField", Objects.isNull(session) ? "N/A" : session.getCurrentGame());
        Widgets.field(cmd, "MatchStateField", Objects.isNull(session) ? "N/A" : session.getSessionState().name());
    }

    @Nullable
    @Override
    public Message handle(@Nonnull String action, @Nonnull AdminPageEvent event, @Nullable GameSession session, @Nonnull AdminPage page) {
        return switch (action) {
            case "match.skip" -> notYet("Skipping the countdown");
            case "match.pause" -> notYet("Pausing the countdown");
            case "match.resume" -> notYet("Resuming the countdown");
            case "match.timer" -> notYet("Setting the countdown");
            case "match.start" -> notYet("Starting the lobby countdown");
            case "match.portal" -> notYet("Opening the portal");
            case "match.transfer" -> notYet("Transferring players to the arena");
            case "match.suddendeath" -> notYet("Sudden death");
            case "match.end" -> notYet("Ending the match");
            case "match.stop" -> notYet("Stopping the match");
            case "match.resetscores" -> notYet("Resetting event scores");
            default -> null;
        };
    }

    @Nonnull
    private static Message notYet(@Nonnull String what) {
        return error(what + " is not yet supported!");
    }
}
