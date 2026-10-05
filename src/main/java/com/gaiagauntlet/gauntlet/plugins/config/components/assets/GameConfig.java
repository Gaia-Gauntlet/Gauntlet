package com.gaiagauntlet.gauntlet.plugins.config.components.assets;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetCodecMapCodec;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

import static com.gaiagauntlet.gauntlet.plugins.teams.components.assets.TeamListAsset.ASSET_STORE;

public class GameConfig implements JsonAssetWithMap<String, IndexedLookupTableAssetMap<String, GameConfig>> {

    protected AssetExtraInfo.Data data;
    @Getter private String id;

    public static final AssetCodecMapCodec<String, @NotNull GameConfig> CODEC = new AssetCodecMapCodec<>(
        Codec.STRING,
        (t, k) -> t.id = k,
        t -> t.id,
        (t, data) -> t.data = data,
        t -> t.data);

    public static final BuilderCodec<@NotNull GameConfig> ABSTRACT_CODEC = BuilderCodec
        .abstractBuilder(GameConfig.class)
        // NOTE: Add more config here ONLY if it is truly universal behaviour across all
        // games
        .build();

    /**
     * The asset store for {@link GameConfig} assets.
     */
    private static AssetStore<String, GameConfig, IndexedLookupTableAssetMap<String, GameConfig>> ASSET_STORE;

    /**
     * The validator cache for {@link GameConfig} assets.
     */
    @Nonnull
    public static final ValidatorCache<String> VALIDATOR_CACHE = new ValidatorCache<>(
        new AssetKeyValidator<>(GameConfig::getAssetStore));

    @Nonnull
    public static AssetStore<String, GameConfig, IndexedLookupTableAssetMap<String, GameConfig>> getAssetStore() {
        if (ASSET_STORE == null)
            ASSET_STORE = AssetRegistry.getAssetStore(GameConfig.class);
        return ASSET_STORE;
    }

    public static IndexedLookupTableAssetMap<String, GameConfig> getAssetMap() {
        return getAssetStore().getAssetMap();
    }
}
