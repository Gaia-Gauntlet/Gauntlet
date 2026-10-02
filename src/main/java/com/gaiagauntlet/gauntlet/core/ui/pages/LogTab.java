package com.gaiagauntlet.gauntlet.core.ui.pages;

import java.util.ArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.admin.AdminLog;
import com.gaiagauntlet.gauntlet.core.ui.events.AdminPageEvent;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

/** What happened lately in the selected game, newest first. */
final class LogTab implements AdminTab {

    private static final int LINES = 60;

    @Nonnull
    @Override
    public String getId() {
        return "Log";
    }

    @Override
    public void bind(@Nonnull UIEventBuilder evt) {
    }

    // @Override
    public void render(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nonnull String gameId) {
        var rows = new ArrayList<String>();
        for (var line : AdminLog.recent(gameId, LINES)) {
            rows.add(line.render());
        }
        Widgets.fillList(cmd, "#LogList", rows, "Nothing logged yet");
    }

    // @Nullable
    // @Override
    // public Message handle(@Nonnull String action, @Nonnull AdminPageEvent event, @Nonnull Game game, @Nonnull AdminPage page) {
    //     return null;
    // }
}
