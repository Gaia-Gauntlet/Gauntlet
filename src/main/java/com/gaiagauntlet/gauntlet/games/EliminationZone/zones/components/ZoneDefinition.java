package com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components;

import com.gaiagauntlet.gg.loot.LootFountainRule;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;

import javax.annotation.Nonnull;

/**
 * One combat zone of the arena: an annular sector (a slice of a ring) placed at a world-space
 * center. Angles are degrees where 0 points along +X and 90 along +Z. A zone closes by sweeping its
 * edge from the outer radius to the inner radius; the band behind the edge is the void.
 */
public final class ZoneDefinition {

    public static final BuilderCodec<ZoneDefinition> CODEC = BuilderCodec.builder(ZoneDefinition.class, ZoneDefinition::new)
            .append(new KeyedCodec<>("Id", Codec.STRING), (z, v) -> z.id = v, z -> z.id)
            .addValidator(Validators.nonEmptyString())
            .documentation("Zone name shown to players.")
            .add()
            .append(new KeyedCodec<>("CenterX", Codec.DOUBLE), (z, v) -> z.centerX = v == null ? 0 : v, z -> z.centerX).add()
            .append(new KeyedCodec<>("CenterY", Codec.DOUBLE), (z, v) -> z.centerY = v == null ? 0 : v, z -> z.centerY)
            .documentation("Bottom of the zone. Blocks below it are never voided.")
            .add()
            .append(new KeyedCodec<>("CenterZ", Codec.DOUBLE), (z, v) -> z.centerZ = v == null ? 0 : v, z -> z.centerZ).add()
            .append(new KeyedCodec<>("InnerRadius", Codec.DOUBLE), (z, v) -> z.innerRadius = v == null ? 0 : v, z -> z.innerRadius)
            .documentation("Radius the void stops at; the center stays safe.")
            .add()
            .append(new KeyedCodec<>("OuterRadius", Codec.DOUBLE), (z, v) -> z.outerRadius = v == null ? 0 : v, z -> z.outerRadius).add()
            .append(new KeyedCodec<>("StartAngle", Codec.DOUBLE), (z, v) -> z.startAngle = v == null ? 0 : v, z -> z.startAngle).add()
            .append(new KeyedCodec<>("EndAngle", Codec.DOUBLE), (z, v) -> z.endAngle = v == null ? 360 : v, z -> z.endAngle).add()
            .append(new KeyedCodec<>("Height", Codec.DOUBLE), (z, v) -> z.height = v == null ? 0 : v, z -> z.height)
            .documentation("Vertical extent above CenterY that the void paints.")
            .add()
            .append(new KeyedCodec<>("HudSlot", Codec.INTEGER), (z, v) -> z.hudSlot = v == null ? -1 : v, z -> z.hudSlot)
            .documentation("Position on the radial zone HUD, 0 to 5, or -1 to leave it off the HUD.")
            .add()
            .append(new KeyedCodec<>("HudImage", Codec.STRING), (z, v) -> z.hudImage = v == null ? "" : v, z -> z.hudImage)
            .documentation("Image name under UI/Custom/GG/ZoneRadial, without the ACTIVE_, CLOSING_, CLOSED_ prefix.")
            .add()
            .append(new KeyedCodec<>("LootFountains", new ArrayCodec<>(LootFountainRule.CODEC, LootFountainRule[]::new)),
                    (z, v) -> z.lootFountains = v == null ? new LootFountainRule[0] : v, z -> z.lootFountains)
            .documentation("How many authored loot fountains of each tier a fresh arena keeps in this zone.")
            .add()
            .build();

    private static final double FULL_CIRCLE = 360.0;
    private static final double EPSILON = 1.0e-9;

    private String id = "";
    private double centerX;
    private double centerY;
    private double centerZ;
    private double innerRadius;
    private double outerRadius;
    private double startAngle;
    private double endAngle = FULL_CIRCLE;
    private double height;
    private int hudSlot = -1;
    private String hudImage = "";
    private LootFountainRule[] lootFountains = new LootFountainRule[0];

    public ZoneDefinition() {
    }

    @Nonnull
    public String id() {
        return id;
    }

    public double centerX() {
        return centerX;
    }

    public double centerY() {
        return centerY;
    }

    public double centerZ() {
        return centerZ;
    }

    public double innerRadius() {
        return innerRadius;
    }

    public double outerRadius() {
        return outerRadius;
    }

    public double startAngle() {
        return startAngle;
    }

    public double endAngle() {
        return endAngle;
    }

    public double height() {
        return height;
    }

    public double minY() {
        return centerY;
    }

    public double maxY() {
        return centerY + height;
    }

    public int hudSlot() {
        return hudSlot;
    }

    @Nonnull
    public String hudImage() {
        return hudImage;
    }

    @Nonnull
    public java.util.List<LootFountainRule> lootFountains() {
        return java.util.List.of(lootFountains);
    }

    /** True when the horizontal point lies in this sector's sweep. */
    public boolean isInSweep(double dx, double dz) {
        if (Math.abs(endAngle - startAngle) >= FULL_CIRCLE - EPSILON) {
            return true;
        }
        double angle = normalize(Math.toDegrees(Math.atan2(dz, dx)));
        double start = normalize(startAngle);
        double end = normalize(endAngle);
        if (start <= end) {
            return angle >= start - EPSILON && angle <= end + EPSILON;
        }
        return angle >= start - EPSILON || angle <= end + EPSILON;
    }

    /**
     * True when the horizontal point is in the voided band: between the closing edge and the outer
     * radius, inside the sweep. Nothing is void while the edge still sits on the outer radius.
     */
    public boolean isInVoid(double closeRadius, double px, double pz) {
        if (!Double.isFinite(closeRadius) || outerRadius < innerRadius) {
            return false;
        }
        double edge = Math.clamp(closeRadius, innerRadius, outerRadius);
        if (edge >= outerRadius - EPSILON) {
            return false;
        }
        double dx = px - centerX;
        double dz = pz - centerZ;
        double distanceSquared = dx * dx + dz * dz;
        if (distanceSquared < edge * edge - EPSILON || distanceSquared > outerRadius * outerRadius + EPSILON) {
            return false;
        }
        return isInSweep(dx, dz);
    }

    public boolean isInVoid(double closeRadius, double px, double py, double pz) {
        return py >= minY() && py <= maxY() && isInVoid(closeRadius, px, pz);
    }

    private static double normalize(double degrees) {
        double normalized = degrees % FULL_CIRCLE;
        return normalized < 0.0 ? normalized + FULL_CIRCLE : normalized;
    }

    @Override
    public String toString() {
        return id;
    }
}
