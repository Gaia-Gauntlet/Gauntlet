package com.gaiagauntlet.gauntlet.games.EliminationZone.zones.services;

import com.gaiagauntlet.gauntlet.core.components.PlayerComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfigAsset;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.ZoneComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.ZoneDefinition;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.ZoneVisualisationComponent;
import com.gaiagauntlet.gauntlet.plugins.config.components.GameConfigComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.math.matrix.Matrix4dUtil;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.protocol.packets.player.DisplayDebug;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Matrix4d;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Draws every zone's outline as debug cylinders for players who toggled it on: cyan while open,
 * red once sealed. Shapes are resent a little before they expire so they never flicker.
 */
public final class ZoneWireframeSystem extends DelayedEntitySystem<EntityStore> {

    private static final float LIFETIME_SECONDS = 2.5f;
    private static final int ARC_SEGMENTS = 16;
    private static final double THICKNESS = 0.15;
    private static final float OPACITY = 0.85f;
    private static final Vector3f OPEN = new Vector3f(0.0f, 1.0f, 1.0f);
    private static final Vector3f SEALED = new Vector3f(1.0f, 0.0f, 0.0f);

    public ZoneWireframeSystem() {
        super(LIFETIME_SECONDS - 0.4f);
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        var playerComp = chunk.getComponent(index, PlayerComponent.getComponentType());
        var playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        assert playerComp != null;
        assert playerRef != null;
        var world = store.getExternalData().getWorld();
        String game = playerComp.getCurrentGame();
        var gameEcs = GameStore.withStore(world, game).orElse(null);
        if (gameEcs == null) return;
        var configComp = gameEcs.get(GameConfigComponent.getComponentType()).orElse(null);
        if (configComp == null) return;
        var config = configComp.getConfig();
        if (!(config instanceof EZGameConfigAsset gameConfig)) return;
        List<ZoneDefinition> zones = Arrays.asList(gameConfig.getZones());

        if (zones.isEmpty()) {return;}

        var component = GameStore.ensureStore(world, game).ensure(ZoneComponent.TYPE, ZoneComponent::new);
        for (var zone : zones) {
            var sealed = component != null && component.closedZones().stream().anyMatch(z -> z.id().equals(zone.id()));
            for (var segment : segments(zone)) {
                var packet = line(segment, sealed ? SEALED : OPEN);
                if (packet != null) {
                    playerRef.getPacketHandler().write(packet);
                }
            }
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            ZoneVisualisationComponent.getComponentType(),
            PlayerComponent.getComponentType()
        );
    }

    private record Segment(double x1, double y1, double z1, double x2, double y2, double z2) {
    }

    /** The base loop, the same loop at the top, and vertical edges at the corners. */
    @Nonnull
    private static List<Segment> segments(@Nonnull ZoneDefinition zone) {
        var out = new ArrayList<Segment>();
        double baseY = zone.minY();
        double topY = zone.maxY();
        boolean fullCircle = Math.abs(zone.endAngle() - zone.startAngle()) >= 360.0 - 1e-6;
        boolean hasInner = zone.innerRadius() > 1e-6;
        if (fullCircle) {
            addRing(out, zone, zone.outerRadius(), baseY, topY);
            if (hasInner) {
                addRing(out, zone, zone.innerRadius(), baseY, topY);
            }
            return out;
        }
        var perimeter = new ArrayList<double[]>();
        appendArc(perimeter, zone, zone.outerRadius(), zone.startAngle(), zone.endAngle());
        if (hasInner) {
            appendArc(perimeter, zone, zone.innerRadius(), zone.endAngle(), zone.startAngle());
        } else {
            perimeter.add(new double[] {zone.centerX(), zone.centerZ()});
        }
        addLoop(out, perimeter, baseY);
        addLoop(out, perimeter, topY);
        for (double angle : new double[] {zone.startAngle(), zone.endAngle()}) {
            addVertical(out, point(zone, zone.outerRadius(), angle), baseY, topY);
            if (hasInner) {
                addVertical(out, point(zone, zone.innerRadius(), angle), baseY, topY);
            }
        }
        if (!hasInner) {
            addVertical(out, new double[] {zone.centerX(), zone.centerZ()}, baseY, topY);
        }
        return out;
    }

    private static void addRing(@Nonnull List<Segment> out, @Nonnull ZoneDefinition zone, double radius, double baseY, double topY) {
        var loop = new ArrayList<double[]>();
        for (int i = 0; i < ARC_SEGMENTS; i++) {
            loop.add(point(zone, radius, 360.0 * i / ARC_SEGMENTS));
        }
        addLoop(out, loop, baseY);
        addLoop(out, loop, topY);
    }

    private static void appendArc(@Nonnull List<double[]> out, @Nonnull ZoneDefinition zone, double radius, double from, double to) {
        for (int i = 0; i <= ARC_SEGMENTS; i++) {
            out.add(point(zone, radius, from + (to - from) * i / ARC_SEGMENTS));
        }
    }

    private static void addLoop(@Nonnull List<Segment> out, @Nonnull List<double[]> loop, double y) {
        for (int i = 0; i < loop.size(); i++) {
            var a = loop.get(i);
            var b = loop.get((i + 1) % loop.size());
            out.add(new Segment(a[0], y, a[1], b[0], y, b[1]));
        }
    }

    private static void addVertical(@Nonnull List<Segment> out, @Nonnull double[] xz, double baseY, double topY) {
        if (topY - baseY > 1e-6) {
            out.add(new Segment(xz[0], baseY, xz[1], xz[0], topY, xz[1]));
        }
    }

    @Nonnull
    private static double[] point(@Nonnull ZoneDefinition zone, double radius, double angleDegrees) {
        double rad = Math.toRadians(angleDegrees);
        return new double[] {zone.centerX() + Math.cos(rad) * radius, zone.centerZ() + Math.sin(rad) * radius};
    }

    /** A unit cylinder rotated onto the segment, shifted to its midpoint, and scaled to its length. */
    @Nullable
    private static DisplayDebug line(@Nonnull Segment segment, @Nonnull Vector3f color) {
        double dirX = segment.x2() - segment.x1();
        double dirY = segment.y2() - segment.y1();
        double dirZ = segment.z2() - segment.z1();
        double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (length < 0.001) {
            return null;
        }
        var matrix = new Matrix4d().identity().translate(segment.x1(), segment.y1(), segment.z1());
        matrix.rotate(-(Math.atan2(dirZ, dirX) + Math.PI / 2), 0, 1, 0);
        matrix.rotate(-Math.atan2(Math.sqrt(dirX * dirX + dirZ * dirZ), dirY), 1, 0, 0);
        matrix.translate(0, length / 2, 0).scale(THICKNESS, length, THICKNESS);
        return new DisplayDebug(DebugShape.Cylinder, Matrix4dUtil.asFloatData(matrix), color, LIFETIME_SECONDS, (byte) 0, null, OPACITY);
    }
}
