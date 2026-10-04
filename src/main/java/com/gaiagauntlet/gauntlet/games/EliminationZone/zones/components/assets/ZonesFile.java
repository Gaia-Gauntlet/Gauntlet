package com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.assets;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The on-disk shape of the arena's zones, the closing phases, and how the void repaints blocks. */
public final class ZonesFile {

    public static final BuilderCodec<ZonesFile> CODEC = BuilderCodec.builder(ZonesFile.class, ZonesFile::new)
            .append(new KeyedCodec<>("Zones", new ArrayCodec<>(ZoneDefinition.CODEC, ZoneDefinition[]::new)),
                    (f, v) -> f.zones = v == null ? new ZoneDefinition[0] : v, f -> f.zones)
            .documentation("The arena's combat zones. They close one at a time in a random order.")
            .add()
            .append(new KeyedCodec<>("Phases", new ArrayCodec<>(ZonePhase.CODEC, ZonePhase[]::new)),
                    (f, v) -> f.phases = v == null ? new ZonePhase[0] : v, f -> f.phases)
            .documentation("Pacing per closing round, in order. The last one repeats.")
            .add()
            .append(new KeyedCodec<>("VoidBlock", Codec.STRING), (f, v) -> f.voidBlock = v == null ? "" : v, f -> f.voidBlock)
            .documentation("Block the void paints when no mapping matches.")
            .add()
            .append(new KeyedCodec<>("VoidBlockMap", new MapCodec<>(Codec.STRING, LinkedHashMap::new)),
                    (f, v) -> f.voidBlockMap = v == null ? new LinkedHashMap<>() : new LinkedHashMap<>(v), f -> f.voidBlockMap)
            .documentation("Replacement per source block id, or per shape category: Solid, Half, Stairs, Fence, Rubble, Branch, "
                    + "BranchCorner, Pipe, PipeCorner, Roof, RoofFlat, RoofShallow, RoofSteep, Plant, PlantGround, PlantWall, "
                    + "PlantCeiling, Trapdoor, Light, LightGround, LightWall, LightCeiling, Fluid, Default.")
            .add()
            .build();

    private ZoneDefinition[] zones = new ZoneDefinition[0];
    private ZonePhase[] phases = new ZonePhase[0];
    private String voidBlock = "";
    private Map<String, String> voidBlockMap = new LinkedHashMap<>();

    public ZonesFile() {
    }

    @Nonnull
    public List<ZoneDefinition> zones() {
        return List.of(zones);
    }

    @Nonnull
    public List<ZonePhase> phases() {
        return List.of(phases);
    }

    public void setPhases(@Nonnull List<ZonePhase> list) {
        phases = list.toArray(ZonePhase[]::new);
    }

    @Nonnull
    public String voidBlock() {
        return voidBlock;
    }

    @Nonnull
    public Map<String, String> voidBlockMap() {
        return voidBlockMap;
    }
}
