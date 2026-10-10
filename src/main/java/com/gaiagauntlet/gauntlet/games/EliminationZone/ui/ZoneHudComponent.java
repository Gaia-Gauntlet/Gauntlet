package com.gaiagauntlet.gauntlet.games.EliminationZone.ui;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponentType;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;

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

    /** Replaces the session's snapshot. Safe from any thread. */
    public static void publish(@Nonnull GameSession session, @Nonnull List<Zone> zones) {
        var snapshot = List.copyOf(zones);
        GauntletUtils.run(GauntletUtils.withHubWorld(), () -> GameStore.ensureHubStore(session.getId())
                .ensure(componentType, ZoneHudComponent::new).zones = snapshot);
    }

    /** The session's zones, or an empty list before the zone logic has published any. */
    @Nonnull
    public static List<Zone> of(@Nullable GameSession session) {
        if (session == null || componentType == null) return List.of();
        return GameStore.withHubStore(session.getId())
                .flatMap(store -> store.get(componentType))
                .map(ZoneHudComponent::getZones)
                .orElse(List.of());
    }
}
