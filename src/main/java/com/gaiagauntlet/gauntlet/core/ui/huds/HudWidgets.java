package com.gaiagauntlet.gauntlet.core.ui.huds;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

/** Small helpers for HUD elements: placing groups and pushing only the values that changed. */
public final class HudWidgets {

    private HudWidgets() {
    }

    /**
     * Replaces a group's anchor. A group anchored at the top without a height stretches to the bottom
     * of the screen, so panels whose size depends on their rows get it set here.
     */
    public static void anchor(@Nonnull UICommandBuilder cmd, @Nonnull String selector, @Nullable Integer top,
            @Nullable Integer right, @Nullable Integer width, @Nullable Integer height) {
        var anchor = new Anchor();
        if (top != null) anchor.setTop(Value.of(top));
        if (right != null) anchor.setRight(Value.of(right));
        if (width != null) anchor.setWidth(Value.of(width));
        if (height != null) anchor.setHeight(Value.of(height));
        cmd.setObject(selector + ".Anchor", anchor);
    }

    /** Replaces the anchor of a group placed from the left edge of the screen. */
    public static void anchorLeft(@Nonnull UICommandBuilder cmd, @Nonnull String selector, int top, int left, int width, int height) {
        var anchor = new Anchor();
        anchor.setTop(Value.of(top));
        anchor.setLeft(Value.of(left));
        anchor.setWidth(Value.of(width));
        anchor.setHeight(Value.of(height));
        cmd.setObject(selector + ".Anchor", anchor);
    }

    /** The last value pushed for each property, so a refresh only sends changes. */
    public static final class Sent {

        private final Map<String, Object> values = new HashMap<>();

        /** Forgets every value, for markup that was just rebuilt with its defaults. */
        public void clear() {
            values.clear();
        }

        public void visible(@Nonnull UICommandBuilder cmd, @Nonnull String selector, boolean visible) {
            if (!Boolean.valueOf(visible).equals(values.put(selector + ".Visible", visible))) {
                cmd.set(selector + ".Visible", visible);
            }
        }

        public void text(@Nonnull UICommandBuilder cmd, @Nonnull String selector, @Nonnull String text) {
            if (!text.equals(values.put(selector + ".Text", text))) {
                cmd.set(selector + ".Text", text);
            }
        }

        public void background(@Nonnull UICommandBuilder cmd, @Nonnull String selector, @Nonnull String texture) {
            if (!texture.equals(values.put(selector + ".Background", texture))) {
                cmd.set(selector + ".Background", texture);
            }
        }
    }
}
