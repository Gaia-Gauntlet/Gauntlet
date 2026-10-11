package com.gaiagauntlet.gauntlet.games.EliminationZone.ui;

import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.pages.AdminPage;
import com.gaiagauntlet.gauntlet.core.ui.pages.Widgets;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.LobbyController;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.World;

public final class EZAdmin {

    private EZAdmin() {
    }

    @Nonnull
    public static Optional<GameEcs> game(@Nullable GameSession session) {
        return arena(session).flatMap(arena -> GameStore.withStore(arena, session.getId()));
    }

    @Nullable
    public static Message onGame(@Nullable GameSession session, @Nonnull AdminPage page, @Nonnull String action,
            @Nonnull Function<GameEcs, Message> task) {
        if (session == null) {
            throw new IllegalStateException("Pick a session first");
        }
        var arena = arena(session).orElseThrow(() -> new IllegalStateException(session.getId() + " has no live arena"));
        page.onWorld(arena, action, () -> task.apply(GameStore.withStore(arena, session.getId())
                .orElseThrow(() -> new IllegalStateException(session.getId() + " has no game in its arena"))));
        return null;
    }

    @Nullable
    public static Double optionalSeconds(@Nonnull String text) {
        return text.isBlank() ? null : (double) Widgets.parseInt(text, "seconds");
    }

    @Nonnull
    private static Optional<World> arena(@Nullable GameSession session) {
        if (session == null) {
            return Optional.empty();
        }
        return LobbyController.withArenaWorld(GauntletUtils.withHubWorld(), session.getId());
    }
}
