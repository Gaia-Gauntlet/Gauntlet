package com.gaiagauntlet.gauntlet.plugins.announcer.utils;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;

/** Consistent colours for command output. */
public final class Chat {

    public static final String OK = "#55FF55";
    public static final String WARN = "#FF8844";
    public static final String ERROR = "#FF5555";
    public static final String HEADER = "#FFD37E";
    public static final String VALUE = "#FFFFFF";
    public static final String MUTED = "#AAAAAA";

    private Chat() {
    }

    public static void ok(@Nonnull CommandContext context, @Nonnull String text) {
        context.sendMessage(Message.raw(text).color(OK));
    }

    public static void warn(@Nonnull CommandContext context, @Nonnull String text) {
        context.sendMessage(Message.raw(text).color(WARN));
    }

    public static void error(@Nonnull CommandContext context, @Nonnull String text) {
        context.sendMessage(Message.raw(text).color(ERROR));
    }

    public static void header(@Nonnull CommandContext context, @Nonnull String text) {
        context.sendMessage(Message.raw(text).color(HEADER).bold(true));
    }

    public static void line(@Nonnull CommandContext context, @Nonnull String label, @Nonnull String value) {
        context.sendMessage(Message.raw(label + " ").color(MUTED).insert(Message.raw(value).color(VALUE)));
    }
}
