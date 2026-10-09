package com.gaiagauntlet.gauntlet.core.ui;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;

/**
 * A group of admin tabs behind one button of the dashboard's top strip. A plugin section only opens
 * when its plugin is installed in the selected session, and a game section is the controller of the
 * sessions playing that game.
 */
public record AdminSection(@Nonnull String title, @Nullable String pluginId, @Nullable String gameId,
        @Nonnull List<AdminTab> tabs) {
}
