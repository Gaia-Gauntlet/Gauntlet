package com.gaiagauntlet.gauntlet.core.ui.interfaces;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

/**
 * One tab of the admin page. The markup lives in {@code GG/Admin/Tab<Id>.ui}; the tab binds its
 * controls, fills the parts that only change when the selected game changes, renders its live
 * fields on every refresh, and answers the actions it declared. Action ids are "<tab>.<name>".
 */
public interface AdminTab {

    /** Matches the panel id "#Panel<Id>" and the tab button "#Tab<Id>". */
    @Nonnull
    String getId();

    /** Registers the bindings for the controls the markup declares. Runs once per page. */
    void bind(@Nonnull UIEventBuilder evt);

    /** Fills generated rows and pickers. Runs when the page opens and when the selected game changes. */
    // default void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nonnull Game game) {
    // }

    /** Rewrites the live fields. Runs on the refresh timer and after each action. */
    // void render(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nonnull Game game);

    /** Performs one of this tab's actions. Returns the line to show in the status bar, or null. */
    // @Nullable
    // Message handle(@Nonnull String action, @Nonnull AdminPageEvent event, @Nonnull Game game, @Nonnull AdminPage page);
}
