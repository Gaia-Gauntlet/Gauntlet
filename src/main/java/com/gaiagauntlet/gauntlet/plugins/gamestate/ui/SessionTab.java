package com.gaiagauntlet.gauntlet.plugins.gamestate.ui;

import com.gaiagauntlet.gauntlet.core.GauntletOrchestrator;
import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.events.AdminPageEvent;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.core.ui.pages.AdminPage;
import com.gaiagauntlet.gauntlet.core.ui.pages.Widgets;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.error;
import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.msg;

public final class SessionTab implements AdminTab {
    public SessionTab() {}

    @Override
    public @NonNull String getId() {
        return "Session";
    }

    @Override
    public void bind(@NonNull UIEventBuilder evt) {
        Widgets.bind(evt, "#DestroySession", "session.destroy");

        Widgets.bind(evt, "#SetupGame", "session.game.setup");
        Widgets.bind(evt, "#StartGame", "session.game.start");
        Widgets.bind(evt, "#StopGame", "session.game.stop");
    }

    @Override
    public void buildOnce(@NonNull UICommandBuilder cmd, @NonNull UIEventBuilder evt, @Nullable GameSession session) {
        AdminTab.super.buildOnce(cmd, evt, session);
    }

    @Override
    public void render(@NonNull UICommandBuilder cmd, @NonNull UIEventBuilder evt, GameSession session) {
        AdminTab.super.render(cmd, evt, session);

        Widgets.field(cmd, "SessionField", Objects.isNull(session) ? "N/A" : session.getId());
        Widgets.field(cmd, "GameField", Objects.isNull(session) ? "N/A" : session.getCurrentGame());
        Widgets.field(cmd, "StateField", Objects.isNull(session) ? "N/A" : session.getSessionState().name());

        Widgets.fillList(cmd, "GameList",
            Objects.isNull(session) ? List.of() : Arrays.stream(session.getGameSequence()).toList(),
            "No games added yet..."
        );
    }

    @Override
    public @Nullable Message handle(@NonNull String action, @NonNull AdminPageEvent event, @Nullable GameSession session, @NonNull AdminPage page) {
        return switch (action) {
            case "session.destroy" -> sessionDestroy(session);
            case "session.game.setup" -> gameSetup(session, page);
            case "session.game.start" -> gameStart(session);
            case "session.game.stop" -> gameStop(session);
            default -> Message.raw("Session tab received unknown action "+ action).color(Color.RED);
        };
    }

    private Message sessionDestroy(@Nullable GameSession session) {
        if (Objects.nonNull(session)) {
            var oldSession = GauntletUtils.withResource().getSessions().remove(session.getId());
            if (Objects.nonNull(oldSession)) {
                return msg("server.gg.commands.session.destroy.success").param("sessionId", oldSession.getId());
            }
        }
        return error("Unable to remove session because it isn't registered!");
    }

    private Message gameSetup(@Nullable GameSession session, AdminPage page) {
        if (Objects.isNull(session)) {
            return error("No session to setup a game for!");
        }
        var sessionId = session.getId();
        var playerRef = page.getPlayer();
        var ref = playerRef.getReference();
        assert ref != null;
        var future = GauntletOrchestrator.setupGame(ref.getStore(), sessionId);
        page.pushStatus(msg("server.gg.commands.session.setup.pending").param("sessionId", sessionId));
        future.whenComplete((ctrl, error) -> {
            if (error != null) {
                page.pushStatus(error("Unable to setup! " + error.getLocalizedMessage()));
                return;
            }
            page.pushStatus(msg("server.gg.commands.session.setup.success")
                .param("sessionId", sessionId)
                .param("gameId", ctrl.getId()));
        });

        return null;
    }

    private Message gameStart(@Nullable GameSession session) {
        return error("Starting games not yet supported!");
    }

    private Message gameStop(@Nullable GameSession session) {
        return error("Stopping games not yet supported!");
    }
}
