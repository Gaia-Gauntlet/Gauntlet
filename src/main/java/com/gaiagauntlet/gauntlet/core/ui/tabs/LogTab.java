package com.gaiagauntlet.gauntlet.core.ui.tabs;

import java.util.ArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.admin.AdminLog;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.events.AdminPageEvent;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.core.ui.pages.AdminPage;
import com.gaiagauntlet.gauntlet.core.ui.pages.Widgets;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

/** What happened lately in the selected game, newest first. */
public final class LogTab implements AdminTab {

    private static final int LINES = 60;

    public LogTab() {}

    @Nonnull @Override public String getId() {
        return "Log";
    }

    @Nonnull @Override public String getPanel() {
        return "Gauntlet/Admin/Panels/PanelLog.ui";
    }

    @Override public int getOrder() {
        return 100;
    }
    @Override
    public void bind(@Nonnull UIEventBuilder evt) {
    }

    @Override
    public void render(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nullable GameSession session) {
        var rows = new ArrayList<String>();
        for (var line : AdminLog.recent(session, LINES)) {
            rows.add(line.toString());
        }
        Widgets.fillList(cmd, "LogList", rows, "Nothing logged yet");
    }
}
