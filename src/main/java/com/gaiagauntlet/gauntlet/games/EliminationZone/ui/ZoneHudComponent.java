package com.gaiagauntlet.gauntlet.games.EliminationZone.ui;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.session.components.SessionComponent;
import com.gaiagauntlet.gauntlet.core.session.components.SessionComponentType;

import lombok.Getter;
import lombok.Setter;

/**
 * What the zone radial shows for a session: one entry per zone with its slot on the radial, its art,
 * and how far it has closed. The zone logic publishes a fresh snapshot whenever a zone changes state,
 * and the HUD only ever reads the latest one. Not saved, since the zone logic republishes on load.
 */
public final class ZoneHudComponent implements SessionComponent {

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

    @Getter @Setter private static SessionComponentType<ZoneHudComponent> sessionComponentType;

    @Getter private volatile List<Zone> zones = List.of();

    /** Replaces the session's snapshot. Safe from any thread. */
    public static void publish(@Nonnull GameSession session, @Nonnull List<Zone> zones) {
        session.ensure(sessionComponentType, new ZoneHudComponent()).zones = List.copyOf(zones);
    }

    /** The session's zones, or an empty list before the zone logic has published any. */
    @Nonnull
    public static List<Zone> of(@Nullable GameSession session) {
        if (session == null || sessionComponentType == null) return List.of();
        return session.get(sessionComponentType).map(ZoneHudComponent::getZones).orElse(List.of());
    }
}
