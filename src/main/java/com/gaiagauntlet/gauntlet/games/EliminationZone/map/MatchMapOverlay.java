package com.gaiagauntlet.gauntlet.games.EliminationZone.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.ZoneComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.ZoneDefinition;
import com.gaiagauntlet.gauntlet.plugins.events.events.MatchEventRegistry;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.events.ArenaLoadedEvent;
import com.gaiagauntlet.gauntlet.plugins.spectator.components.SpectatorComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.EliminatedComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.utils.TeamUtils;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.packets.worldmap.PlayerMarkerComponent;
import com.hypixel.hytale.protocol.packets.worldmap.TintComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.MapMarkerBuilder;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.MarkersCollector;

public final class MatchMapOverlay implements WorldMapManager.MarkerProvider {

    private static final String PROVIDER_KEY = "gaiaGauntlet";
    private static final String OUTLINE_IMAGE = "Coordinate.png";
    private static final String BLANK_COMPASS_IMAGE = "GG_Blank.png";
    private static final String TEAMMATE_IMAGE = "Player.png";
    private static final double OUTLINE_SPACING = 8.0;
    // the client draws a moved marker where it last was, so a teammate is resent under a new id once they move this far
    private static final double TEAMMATE_RESEND_DISTANCE_SQ = 1.5 * 1.5;
    private static final int OUTLINE_MAX_POINTS = 32;
    private static final TintComponent RED = new TintComponent(new Color((byte) 220, (byte) 55, (byte) 55));

    private final String sessionId;
    private final Map<UUID, Map<UUID, SentMarker>> sentTeammates = new ConcurrentHashMap<>();

    private MatchMapOverlay(@Nonnull String sessionId) {
        this.sessionId = sessionId;
    }

    public static void setup() {
        MatchEventRegistry.register(ArenaLoadedEvent.class, EZController.ID,
                evt -> evt.getWorld().getWorldMapManager().addMarkerProvider(PROVIDER_KEY,
                        new MatchMapOverlay(evt.getSessionId())));
    }

    @Override
    public void update(@Nonnull World world, @Nonnull Player player, @Nonnull MarkersCollector collector) {
        var game = GameStore.withStore(world, sessionId).orElse(null);
        if (game == null)
            return;
        game.get(ZoneComponent.TYPE).ifPresent(zones -> {
            for (var zone : zones.closedZones()) {
                outline(collector, "gg_sealed_" + zone.id(), zone, zone.innerRadius(), "Sealed: " + zone.id(), false);
            }
            var active = zones.activeZone();
            if (active != null && zones.isClosingStarted() && !zones.isHolding() && zones.progress() > 0.0) {
                outline(collector, "gg_closing_" + active.id(), active, zones.closeRadius(),
                        "Closing: " + active.id(), true);
            }
        });
        teammates(world, player, game, collector);
    }

    private void teammates(@Nonnull World world, @Nonnull Player player, @Nonnull GameEcs game,
            @Nonnull MarkersCollector collector) {
        var ref = player.getReference();
        if (ref == null || !ref.isValid())
            return;
        var store = ref.getStore();
        var viewer = store.getComponent(ref, PlayerRef.getComponentType());
        if (viewer == null)
            return;
        var team = TeamUtils.teamOf(game, viewer.getUuid()).orElse(null);
        var standing = team != null && team.isParticipant()
                && store.getComponent(ref, EliminatedComponent.getComponentType()) == null
                && store.getComponent(ref, SpectatorComponent.getComponentType()) == null;
        if (!standing) {
            sentTeammates.remove(viewer.getUuid());
            player.getWorldMapTracker().setPlayerMapFilter(null);
            return;
        }
        player.getWorldMapTracker().setPlayerMapFilter(hideStanding(game));

        var sent = sentTeammates.computeIfAbsent(viewer.getUuid(), id -> new ConcurrentHashMap<>());
        for (var other : new ArrayList<>(world.getPlayerRefs())) {
            var mate = other.getUuid();
            if (mate.equals(viewer.getUuid()) || !team.contains(mate) || !isStanding(other))
                continue;
            var transform = other.getTransform();
            var position = transform.getPosition();
            var last = sent.get(mate);
            if (last == null || last.distanceSq(position.x(), position.y(), position.z()) > TEAMMATE_RESEND_DISTANCE_SQ) {
                last = new SentMarker(last == null ? 0 : last.generation() + 1, position.x(), position.y(), position.z());
                sent.put(mate, last);
            }
            collector.addIgnoreViewDistance(
                    new MapMarkerBuilder("gg_mate_" + mate + "_" + last.generation(), TEAMMATE_IMAGE, transform)
                            .withCustomName(other.getUsername())
                            .withComponent(new PlayerMarkerComponent(mate))
                            .build());
        }
    }

    // the filter hides a player when it returns true, so competitors only see their teammates through the markers above
    @Nonnull
    private static Predicate<PlayerRef> hideStanding(@Nonnull GameEcs game) {
        return other -> TeamUtils.teamOf(game, other.getUuid()).isPresent() && isStanding(other);
    }

    private static boolean isStanding(@Nonnull PlayerRef player) {
        return player.getComponentConcurrent(EliminatedComponent.getComponentType()) == null
                && player.getComponentConcurrent(SpectatorComponent.getComponentType()) == null;
    }

    private record SentMarker(long generation, double x, double y, double z) {

        double distanceSq(double px, double py, double pz) {
            double dx = px - x;
            double dy = py - y;
            double dz = pz - z;
            return dx * dx + dy * dy + dz * dz;
        }
    }

    private static void outline(@Nonnull MarkersCollector collector, @Nonnull String idPrefix,
            @Nonnull ZoneDefinition zone, double innerRadius, @Nullable String name, boolean onCompass) {
        int index = 0;
        for (var point : outlinePoints(zone, innerRadius)) {
            var builder = new MapMarkerBuilder(idPrefix + "_" + index, OUTLINE_IMAGE,
                    new Transform(point[0], 0, point[1], 0f, 0f, 0f))
                    .withCompassImage(onCompass ? null : BLANK_COMPASS_IMAGE)
                    .withComponent(RED);
            if (index == 0 && name != null) {
                builder.withCustomName(name);
            }
            collector.addIgnoreViewDistance(builder.build());
            index++;
        }
    }

    @Nonnull
    private static List<double[]> outlinePoints(@Nonnull ZoneDefinition zone, double innerRadius) {
        var points = new ArrayList<double[]>();
        double outer = zone.outerRadius();
        double inner = Math.max(0.0, Math.min(innerRadius, outer));
        if (outer <= 0.0) {
            return points;
        }
        double sweep = zone.endAngle() - zone.startAngle();
        boolean full = Math.abs(sweep) >= 360.0 - 1e-6;
        double start = Math.toRadians(zone.startAngle());
        double sweepRad = Math.toRadians(full ? 360.0 : sweep);
        arc(points, zone, outer, start, sweepRad);
        if (inner > 1e-6) {
            arc(points, zone, inner, start, sweepRad);
        }
        if (!full) {
            edge(points, zone, inner, outer, start);
            edge(points, zone, inner, outer, start + sweepRad);
        }
        return points;
    }

    private static void arc(@Nonnull List<double[]> out, @Nonnull ZoneDefinition zone, double radius, double start,
            double sweep) {
        int count = Math.min(OUTLINE_MAX_POINTS, Math.max(4, (int) Math.ceil(radius * Math.abs(sweep) / OUTLINE_SPACING) + 1));
        for (int i = 0; i < count; i++) {
            double angle = start + sweep * i / (count - 1);
            out.add(new double[] { zone.centerX() + Math.cos(angle) * radius, zone.centerZ() + Math.sin(angle) * radius });
        }
    }

    private static void edge(@Nonnull List<double[]> out, @Nonnull ZoneDefinition zone, double inner, double outer,
            double angle) {
        int count = Math.min(OUTLINE_MAX_POINTS, Math.max(2, (int) Math.ceil((outer - inner) / OUTLINE_SPACING) + 1));
        for (int i = 0; i < count; i++) {
            double radius = inner + (outer - inner) * i / (count - 1);
            out.add(new double[] { zone.centerX() + Math.cos(angle) * radius, zone.centerZ() + Math.sin(angle) * radius });
        }
    }
}
