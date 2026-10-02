package com.gaiagauntlet.gauntlet.core.ui.pages;

import com.gaiagauntlet.gauntlet.core.admin.AdminLog;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

import javax.annotation.Nonnull;
import java.util.ArrayList;

/** Displayed when no other tabs are available to be displayed. */
final class NoTab implements AdminTab {
    @Nonnull
    @Override
    public String getId() {
        return "NoTab";
    }

    @Override
    public void bind(@Nonnull UIEventBuilder evt) {
    }

    // @Override
    public void render(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nonnull String gameId) {
    }
}
