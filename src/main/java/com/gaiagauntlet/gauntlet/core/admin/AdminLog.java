package com.gaiagauntlet.gauntlet.core.admin;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.hypixel.hytale.logger.HytaleLogger;

/**
 * The last few hundred things worth telling an admin, kept in memory for the
 * dashboard's Log tab.
 */
public final class AdminLog {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    // global logs
    public static final String GLOBAL = "Global";
    private static final int CAPACITY = 300;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    private static final ArrayDeque<Line> LINES = new ArrayDeque<>();

    /** One entry. The game id is empty for server-wide lines. */
    public record Line(long millis, @Nonnull String gameId, @Nonnull String text) {
        @Nonnull
        public String render() {
            return TIME.format(Instant.ofEpochMilli(millis)) + (gameId.isEmpty() ? " " : " [" + gameId + "] ") + text;
        }
    }

    private AdminLog() {
    }

    public static void add(@Nonnull String text) {
        add(GLOBAL, text);
    }

    public static void add(@Nonnull String gameId, @Nonnull String text) {
        LOGGER.atInfo().log(gameId + " " + text);
        synchronized (LINES) {
            LINES.addLast(new Line(System.currentTimeMillis(), gameId, text));
            while (LINES.size() > CAPACITY) {
                LINES.removeFirst();
            }
        }
    }

    /** The newest lines first: the game's own plus server-wide ones. */
    @Nonnull
    public static List<Line> recent(@Nullable GameSession session, int limit) {
        var out = new ArrayList<Line>();
        if (Objects.isNull(session)) return out;

        synchronized (LINES) {
            var it = LINES.descendingIterator();
            while (it.hasNext() && out.size() < limit) {
                var line = it.next();
                if (line.gameId().isEmpty() || line.gameId().equals(session.getCurrentGame())) {
                    out.add(line);
                }
            }
        }
        return out;
    }
}
