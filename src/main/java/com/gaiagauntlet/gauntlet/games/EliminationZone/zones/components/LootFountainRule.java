package com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import javax.annotation.Nonnull;
import java.util.List;

/** How many authored loot fountains of one tier a zone keeps in a fresh arena. */
public final class LootFountainRule {

    public static final BuilderCodec<LootFountainRule> CODEC = BuilderCodec.builder(LootFountainRule.class, LootFountainRule::new)
            .append(new KeyedCodec<>("Tier", Codec.INTEGER), (r, v) -> r.tier = v == null ? 1 : v, r -> r.tier)
            .documentation("Loot fountain tier, 1 through 5.")
            .add()
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN), (r, v) -> r.enabled = v == null || v, r -> r.enabled)
            .documentation("When false every authored fountain of this tier stays.")
            .add()
            .append(new KeyedCodec<>("Min", Codec.INTEGER), (r, v) -> r.min = v == null ? 0 : v, r -> r.min).add()
            .append(new KeyedCodec<>("Max", Codec.INTEGER), (r, v) -> r.max = v == null ? 0 : v, r -> r.max)
            .documentation("The number kept is rolled between Min and Max; the rest are removed.")
            .add()
            .append(new KeyedCodec<>("AlwaysSpawn", new ArrayCodec<>(Codec.INT_ARRAY, int[][]::new)),
                    (r, v) -> r.alwaysSpawn = v == null ? new int[0][] : v, r -> r.alwaysSpawn)
            .documentation("Block positions as [x, y, z] that are never removed and do not count toward Min and Max.")
            .add()
            .build();

    private int tier = 1;
    private boolean enabled = true;
    private int min;
    private int max;
    private int[][] alwaysSpawn = new int[0][];

    public LootFountainRule() {
    }

    public int tier() {
        return tier;
    }

    public boolean enabled() {
        return enabled;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    @Nonnull
    public List<int[]> alwaysSpawn() {
        return List.of(alwaysSpawn);
    }
}
