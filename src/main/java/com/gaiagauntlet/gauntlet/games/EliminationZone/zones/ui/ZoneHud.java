package com.gaiagauntlet.gauntlet.games.EliminationZone.zones.ui;

import com.gaiagauntlet.gg.store.Game;
import com.gaiagauntlet.gg.ui.GgHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The radial zone map in the corner of the screen: six fixed slots, each showing its zone as
 * active, closing, or closed. Zones pick their slot and image in the zones file.
 */
public final class ZoneHud extends GgHud {

    public static final String KEY = "GG.Zones";

    private static final String FOLDER = "GG/ZoneRadial/";
    private static final int SLOTS = 6;
    /** Top, right, and left offsets plus size per slot, matching the radial background art. */
    private static final int[][] SLOT_LAYOUT = {
            {11, -1, -1, 155, 147},
            {30, 28, -1, 145, 125},
            {155, 27, -1, 145, 125},
            {151, -1, -1, 155, 149},
            {155, -1, 26, 145, 125},
            {30, -1, 27, 145, 125}};

    private final Game game;
    private final List<ZoneDefinition> zones;
    private final Map<Integer, String> sent = new HashMap<>();

    public ZoneHud(@Nonnull PlayerRef viewer, @Nonnull Game game) {
        super(viewer, KEY);
        this.game = game;
        this.zones = List.copyOf(Zones.get().zones());
    }

    @Nonnull
    @Override
    protected String markup() {
        var out = new StringBuilder();
        out.append("Group {\n  Anchor: (Top: 0, Left: 0, Width: 350, Height: 350);\n  Background: ")
                .append(quote(FOLDER + "ZoneRadialBackground.png")).append(";\n");
        var component = ZoneComponent.TYPE.of(game);
        for (var zone : zones) {
            int slot = zone.hudSlot();
            if (slot < 0 || slot >= SLOTS) {
                continue;
            }
            var layout = SLOT_LAYOUT[slot];
            out.append("  Group #Zone").append(slot).append(" { Anchor: (Top: ").append(layout[0]);
            if (layout[1] >= 0) {
                out.append(", Right: ").append(layout[1]);
            }
            if (layout[2] >= 0) {
                out.append(", Left: ").append(layout[2]);
            }
            var image = imageFor(zone, component);
            sent.put(slot, image);
            out.append(", Width: ").append(layout[3]).append(", Height: ").append(layout[4]).append("); Background: ")
                    .append(quote(image)).append("; }\n");
        }
        out.append("  Group { Anchor: (Top: 5, Width: 300, Height: 330); Background: ").append(quote(FOLDER + "ZoneRadial.png")).append("; }\n}\n");
        return out.toString();
    }

    @Override
    protected void refresh(@Nonnull UICommandBuilder cmd) {
        var component = ZoneComponent.TYPE.of(game);
        for (var zone : zones) {
            int slot = zone.hudSlot();
            if (slot < 0 || slot >= SLOTS) {
                continue;
            }
            var image = imageFor(zone, component);
            if (!image.equals(sent.put(slot, image))) {
                cmd.set("#Zone" + slot + ".Background", image);
            }
        }
    }

    @Nonnull
    private static String imageFor(@Nonnull ZoneDefinition zone, @Nonnull ZoneComponent component) {
        var state = "ACTIVE_";
        if (component.closedZones().stream().anyMatch(z -> z.id().equals(zone.id()))) {
            state = "CLOSED_";
        } else if (component.isClosingStarted() && component.activeZone() != null
                && component.activeZone().id().equals(zone.id())) {
            state = "CLOSING_";
        }
        return FOLDER + state + zone.hudImage() + ".png";
    }
}
