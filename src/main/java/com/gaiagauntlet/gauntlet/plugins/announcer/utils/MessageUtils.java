package com.gaiagauntlet.gauntlet.plugins.announcer.utils;

import com.hypixel.hytale.server.core.Message;

import javax.annotation.Nonnull;

import java.util.Objects;

public class MessageUtils {
    private MessageUtils() {}

    @Nonnull
    public static Message msg(String key) {
        var message = Message.translation(key);
        message.getFormattedMessage().markupEnabled = true;
        return message;
    }

    public static Message error(@Nonnull String error) {
        var message = Message.translation("server.gg.commands.error").param("message", error);
        message.getFormattedMessage().markupEnabled = true;
        return message;
    }

    @Nonnull
    public static String reason(@Nonnull Throwable error) {
        return Objects.requireNonNullElse(error.getLocalizedMessage(), error.getClass().getSimpleName());
    }

    @Nonnull
    public static Message markup(@Nonnull Message message) {
        message.getFormattedMessage().markupEnabled = true;
        return message;
    }
}
