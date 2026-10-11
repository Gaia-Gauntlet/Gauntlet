package com.gaiagauntlet.gauntlet.core.hud.huds;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.orchestrator.GauntletOrchestrator;
import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.hud.interfaces.HudDataSnapshot;
import com.gaiagauntlet.gauntlet.core.hud.interfaces.HudElement;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * The player HUD, one layer per player, built on the server's custom HUD
 * pipeline. It holds every HUD
 * element the orchestrator collected, shows the ones that apply to the player
 * and their session, and
 * redraws them when a HUD event reaches it.
 */
public final class GauntletHud extends CustomUIHud {

    public static final String KEY = "Gauntlet";

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String HUD = "Gauntlet/Hud/Hud.ui";

    private final Map<String, HudElement> elements = GauntletOrchestrator.getHudElements();
    private final Map<String, Integer> elementToIndex = buildElementIndex();

    private Map<String, Integer> buildElementIndex() {
        var index = new HashMap<String, Integer>();
        int position = 0;
        for (var id : elements.keySet()) {
            index.put(id, position++);
        }
        return index;
    }

    /** Visibility last sent for each element, so only changes are pushed. */
    private final boolean[] shown = new boolean[elements.size()];

    /**
     * Builds and renders touch the elements' caches, so HUD events from different
     * world threads take turns.
     */
    private final Object lock = new Object();

    @Nullable
    private String sessionId;

    public GauntletHud(@Nonnull PlayerRef playerRef) {
        super(playerRef, KEY);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder cmd) {
        synchronized (lock) {
            var player = getPlayerRef();
            var session = sessionOf(player);
            sessionId = session == null ? null : session.getId();

            cmd.append(HUD);
            for (var element : elements.values()) {
                cmd.append("#Hud", element.getMarkup());
            }
            for (var element : elements.values()) {
                element.buildOnce(cmd, player, session);
            }
            for (var element : elements.values()) {
                apply(cmd, element, player, session, element.gather(player, session), true);
            }
        }
    }

    /** Redraws one element from a snapshot its owner already gathered. A null snapshot hides it. */
    public void onEvent(@Nullable HudDataSnapshot data, @Nonnull String hudId) {
        var element = elements.get(hudId);
        if (element == null) {
            return;
        }
        send(cmd -> apply(cmd, element, getPlayerRef(), syncSession(cmd), data, false));
    }

    /** Gathers and redraws one element, or every element when the id is null. */
    public void refresh(@Nullable String hudId) {
        send(cmd -> {
            var player = getPlayerRef();
            var session = syncSession(cmd);
            for (var element : elements.values()) {
                if (hudId == null || hudId.equals(element.getId())) {
                    apply(cmd, element, player, session, element.gather(player, session), false);
                }
            }
        });
    }

    private void send(@Nonnull Consumer<UICommandBuilder> writer) {
        synchronized (lock) {
            var cmd = new UICommandBuilder();
            try {
                writer.accept(cmd);
            } catch (RuntimeException e) {
                LOGGER.atWarning().withCause(e).log("HUD for %s failed to render; nothing sent",
                        getPlayerRef().getUsername());
                return;
            }
            if (cmd.getCommands().length > 0) {
                update(false, cmd);
            }
        }
    }

    /** Refills every element's session-bound parts when the player has moved to another session. */
    @Nullable
    private GameSession syncSession(@Nonnull UICommandBuilder cmd) {
        var player = getPlayerRef();
        var session = sessionOf(player);
        var id = session == null ? null : session.getId();
        if (!Objects.equals(id, sessionId)) {
            sessionId = id;
            for (var element : elements.values()) {
                element.buildOnce(cmd, player, session);
            }
        }
        return session;
    }

    private void apply(@Nonnull UICommandBuilder cmd, @Nonnull HudElement element, @Nonnull PlayerRef player,
            @Nullable GameSession session, @Nullable HudDataSnapshot data, boolean force) {
        var index = elementToIndex.get(element.getId());
        var visible = data != null && element.render(cmd, player, session, data);
        if (force || visible != shown[index]) {
            shown[index] = visible;
            cmd.set("#Hud[" + index + "].Visible", visible);
        }
    }

    @Nullable
    private static GameSession sessionOf(@Nonnull PlayerRef player) {
        return GauntletUtils.sessionFor(player).orElse(null);
    }
}
