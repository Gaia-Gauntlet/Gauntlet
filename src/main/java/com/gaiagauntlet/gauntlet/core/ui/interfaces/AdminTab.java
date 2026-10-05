package com.gaiagauntlet.gauntlet.core.ui.interfaces;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.events.AdminPageEvent;
import com.gaiagauntlet.gauntlet.core.ui.pages.AdminPage;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

/**
 * One tab of the admin page. The plugin that owns the tab also ships its panel markup; the tab binds
 * its controls, fills the parts that only change when the selected game changes, renders its live
 * fields on every refresh, and answers the actions it declared. Action ids are "<tab>.<name>".
 */
public interface AdminTab {

    /** Prefix of this tab's action ids. */
    @Nonnull
    String getId();

    /** Text on the tab button. */
    @Nonnull
    default String getTitle() {
        return getId();
    }

    /** Path of the panel markup, relative to Common/UI/Custom. */
    @Nonnull
    String getPanel();

    /** Position in the tab strip. Lower comes first; ties are ordered by id. */
    default int getOrder() {
        return 0;
    }

    /** Registers the bindings for the controls the markup declares. Runs once per page. */
    void bind(@Nonnull UIEventBuilder evt);

    /**
     * Fills generated rows and pickers. Runs when the page opens and when the selected game changes.
     */
    default void buildOnce(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nullable GameSession session) {
    }

    /** Rewrites the live fields. Runs on the refresh timer and after each action. */
     default void render(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nullable GameSession session) {
     }

    /** Performs one of this tab's actions. Returns the line to show in the status bar, or null. */
     @Nullable
     default Message handle(@Nonnull String action, @Nonnull AdminPageEvent event, @Nullable GameSession session, @Nonnull AdminPage page) {
         return null;
     };
}
