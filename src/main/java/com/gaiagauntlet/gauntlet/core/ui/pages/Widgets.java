package com.gaiagauntlet.gauntlet.core.ui.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

/** Small helpers for binding buttons and filling lists, pickers, and fields on the admin page. */
public final class Widgets {

    static final String ROW = "GG/Admin/Row.ui";

    /** One picker entry: what the admin reads and what the page receives. */
    record Option(@Nonnull String label, @Nonnull String value) {
    }

    private Widgets() {
    }

    static void bind(@Nonnull UIEventBuilder evt, @Nonnull String selector, @Nonnull String action) {
        evt.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of("Action", action), false);
    }

    static void bindArg(@Nonnull UIEventBuilder evt, @Nonnull String selector, @Nonnull String action, @Nonnull String arg) {
        evt.addEventBinding(CustomUIEventBindingType.Activating, selector, new EventData().append("Action", action).append("Arg", arg), false);
    }

    /**
     * Binds a click that also carries live control values. Keys starting with @ are resolved by the
     * client at click time from the selector they name, so "@Text" to "#Field.Value" sends the text.
     */
    static void bindValues(@Nonnull UIEventBuilder evt, @Nonnull String selector, @Nonnull String action,
            @Nonnull String arg, @Nonnull Map<String, String> values) {
        var data = new EventData().append("Action", action).append("Arg", arg);
        values.forEach(data::append);
        evt.addEventBinding(CustomUIEventBindingType.Activating, selector, data, false);
    }

    static void bindChange(@Nonnull UIEventBuilder evt, @Nonnull String selector, @Nonnull String action) {
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, selector,
                new EventData().append("Action", action).append("@Pick", selector + ".Value"), false);
    }

    static void field(@Nonnull UICommandBuilder cmd, @Nonnull String id, @Nonnull String text) {
        cmd.set("#" + id + " #Value.Text", text);
    }

    static void text(@Nonnull UICommandBuilder cmd, @Nonnull String selector, @Nonnull String text) {
        cmd.set(selector + ".Text", text);
    }

    /** Replaces the container's rows with one text row each, or one placeholder row when empty. */
    static void fillList(@Nonnull UICommandBuilder cmd, @Nonnull String container, @Nonnull List<String> rows, @Nonnull String empty) {
        cmd.clear(container);
        if (rows.isEmpty()) {
            cmd.append(container, ROW);
            cmd.set(container + "[0].Text", empty);
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            cmd.append(container, ROW);
            cmd.set(container + "[" + i + "].Text", rows.get(i));
        }
    }

    static void fillPicker(@Nonnull UICommandBuilder cmd, @Nonnull String picker, @Nonnull List<Option> options) {
        var entries = new ArrayList<DropdownEntryInfo>(options.size());
        for (var option : options) {
            entries.add(new DropdownEntryInfo(LocalizableString.fromString(option.label()), option.value()));
        }
        cmd.set(picker + ".Entries", entries);
        if (!options.isEmpty()) {
            cmd.set(picker + ".Value", options.get(0).value());
        }
    }

    @Nonnull
    static List<Option> options(@Nonnull List<String> values) {
        var options = new ArrayList<Option>(values.size());
        for (var value : values) {
            options.add(new Option(value, value));
        }
        return options;
    }

    @Nonnull
    static Message ok(@Nonnull String text) {
        return Message.raw(text).color("#55FF55");
    }

    @Nonnull
    static Message warn(@Nonnull String text) {
        return Message.raw(text).color("#FF8844");
    }

    @Nonnull
    static Message fail(@Nonnull String text) {
        return Message.raw(text).color("#FF5555");
    }

    static int parseInt(@Nonnull String text, @Nonnull String what) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + text.trim() + "' is not a number of " + what);
        }
    }
}
