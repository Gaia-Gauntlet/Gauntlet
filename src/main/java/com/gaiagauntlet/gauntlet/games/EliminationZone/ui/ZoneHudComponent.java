package com.gaiagauntlet.gauntlet.games.EliminationZone.ui;

import java.util.List;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponentType;
import lombok.Getter;
import lombok.Setter;

/**
 * What the zone radial shows for a session: one entry per zone with its slot on the radial, its art,
 * and how far it has closed. The zone logic publishes a fresh snapshot whenever a zone changes state,
 * and the HUD only ever reads the latest one. Kept in the session's game store on the hub world, and
 * not saved, since the zone logic republishes on load.
 */
public final class ZoneHudComponent implements GameComponent {

    public static final String ID = "EZZoneHudComponent";

    /** Slots on the radial, clockwise from the top. */
    public static final int SLOTS = 6;

    public enum ZoneState {
        ACTIVE,
        CLOSING,
        CLOSED
    }

    /**
     * One zone on the radial. The image is the art suffix under GG/ZoneRadial, for example
     * "Gaia_City" for GG/ZoneRadial/ACTIVE_Gaia_City.png.
     */
    public record Zone(int slot, @Nonnull String image, @Nonnull ZoneState state) {
    }

    @Getter @Setter private static GameComponentType<@NotNull ZoneHudComponent> componentType;

    @Getter private volatile List<Zone> zones = List.of();
}
