package com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.assets;

import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.ZoneDefinition;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.ZonePhase;
import com.gaiagauntlet.gauntlet.plugins.teams.components.assets.TeamListAsset;
import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The on-disk shape of the arena's zones, the closing phases, and how the void repaints blocks.
 */
public final class ZonesAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, ZonesAsset>> {

    public static final AssetBuilderCodec<String, ZonesAsset> CODEC = AssetBuilderCodec.builder(
            ZonesAsset.class,
            ZonesAsset::new,
            Codec.STRING,
            (asset, id) -> asset.id = id,
            asset -> asset.id,
            (asset, data) -> asset.data = data,
            asset -> asset.data)
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

    @Getter private String id;
    private AssetExtraInfo.Data data;
    private static AssetStore<String, ZonesAsset, DefaultAssetMap<String, ZonesAsset>> ASSET_STORE;

    private ZoneDefinition[] zones = new ZoneDefinition[0];
    private ZonePhase[] phases = new ZonePhase[0];
    @Getter private String voidBlock = "";
    @Getter private Map<String, String> voidBlockMap = new LinkedHashMap<>();

    public ZonesAsset() {
    }

    @Nullable
    public static AssetStore<String, ZonesAsset, DefaultAssetMap<String, ZonesAsset>> getAssetStore() {
        if (ASSET_STORE == null) ASSET_STORE = AssetRegistry.getAssetStore(ZonesAsset.class);
        return ASSET_STORE;
    }

    @Nonnull
    public static Map<String, ZonesAsset> getAssetMap() {
        var store = getAssetStore();
        return store == null ? Map.of() : store.getAssetMap().getAssetMap();
    }

    public static ZonesAsset get() {
        // TODO: Replace this with some sort of configuration rather than just getting the first available.
        var optionalAsset = getAssetMap().values().stream().findAny();
        return optionalAsset.orElse(null);
    }

    @Nonnull
    public List<ZoneDefinition> getZones() {
        return List.of(zones);
    }

    @Nonnull
    public List<ZonePhase> getPhases() {
        return List.of(phases);
    }

    public void setPhases(@Nonnull List<ZonePhase> list) {
        phases = list.toArray(ZonePhase[]::new);
    }
}
