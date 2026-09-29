package com.gaiagauntlet.gauntlet.utils;

import com.hypixel.hytale.server.core.universe.Universe;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class PlayerUtils {
    private PlayerUtils() {}

    @Nonnull
    public static String normalizeUsername(@Nonnull String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isOnline(@Nonnull UUID uuid) {
        return !Objects.isNull(Universe.get().getPlayer(uuid));
    }
}
