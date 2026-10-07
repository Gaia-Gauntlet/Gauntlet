package com.gaiagauntlet.gauntlet.plugins.config.components.assets;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetCodecMapCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Map;

public class GameConfigAsset implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, GameConfigAsset>> {

    protected AssetExtraInfo.Data data;
    @Getter private String id;

    public static final AssetCodecMapCodec<String, @NotNull GameConfigAsset> CODEC = new AssetCodecMapCodec<>(
        Codec.STRING,
        (t, k) -> t.id = k,
        t -> t.id,
        (t, data) -> t.data = data,
        t -> t.data);

    public static final BuilderCodec<@NotNull GameConfigAsset> ABSTRACT_CODEC = BuilderCodec
        .abstractBuilder(GameConfigAsset.class)
        // NOTE: Add more config here ONLY if it is truly universal behaviour across all games
        .build();

    /**
     * The asset store for {@link GameConfigAsset} assets.
     */
    private static AssetStore<String, GameConfigAsset, IndexedLookupTableAssetMap<String, GameConfigAsset>> ASSET_STORE;

    /**
     * The validator cache for {@link GameConfigAsset} assets.
     */
    @Nonnull
    public static final ValidatorCache<String> VALIDATOR_CACHE = new ValidatorCache<>(
        new AssetKeyValidator<>(GameConfigAsset::getAssetStore));

    @Nonnull
    public static AssetStore<String, GameConfigAsset, IndexedLookupTableAssetMap<String, GameConfigAsset>> getAssetStore() {
        if (ASSET_STORE == null)
            ASSET_STORE = AssetRegistry.getAssetStore(GameConfigAsset.class);
        return ASSET_STORE;
    }

    public static Map<String, GameConfigAsset> getAssetMap() {
        return getAssetStore().getAssetMap().getAssetMap();
    }
}
