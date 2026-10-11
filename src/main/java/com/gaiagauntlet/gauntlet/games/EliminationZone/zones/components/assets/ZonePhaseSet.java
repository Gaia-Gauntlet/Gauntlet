package com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.assets;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.ZonePhase;
import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import lombok.Getter;

public final class ZonePhaseSet implements JsonAssetWithMap<String, DefaultAssetMap<String, ZonePhaseSet>> {

    @Nonnull
    public static final AssetBuilderCodec<String, @NotNull ZonePhaseSet> CODEC = AssetBuilderCodec.builder(
            ZonePhaseSet.class,
            ZonePhaseSet::new,
            Codec.STRING,
            (set, id) -> set.id = id,
            set -> set.id,
            (set, data) -> set.data = data,
            set -> set.data)
            .append(new KeyedCodec<>("Phases", new ArrayCodec<>(ZonePhase.CODEC, ZonePhase[]::new)),
                    (set, v) -> set.phases = v == null ? List.of() : List.of(v),
                    set -> set.phases.toArray(ZonePhase[]::new))
            .documentation("One closing round per zone, in closing order. The last phase repeats when there are more zones than phases.")
            .add()
            .build();

    @Nullable
    private static AssetStore<String, ZonePhaseSet, DefaultAssetMap<String, ZonePhaseSet>> ASSET_STORE;

    private AssetExtraInfo.Data data;
    @Getter private String id;
    @Nonnull @Getter private List<ZonePhase> phases = List.of();

    public ZonePhaseSet() {
    }

    @Nullable
    public static AssetStore<String, ZonePhaseSet, DefaultAssetMap<String, ZonePhaseSet>> getAssetStore() {
        if (ASSET_STORE == null)
            ASSET_STORE = AssetRegistry.getAssetStore(ZonePhaseSet.class);
        return ASSET_STORE;
    }

    @Nonnull
    public static Map<String, ZonePhaseSet> getAssetMap() {
        var store = getAssetStore();
        return store == null ? Map.of() : store.getAssetMap().getAssetMap();
    }
}
