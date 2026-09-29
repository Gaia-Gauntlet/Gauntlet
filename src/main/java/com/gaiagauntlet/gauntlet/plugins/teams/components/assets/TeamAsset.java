package com.gaiagauntlet.gauntlet.plugins.teams.components.assets;

import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamType;
import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.server.core.asset.common.CommonAssetValidator;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class TeamAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, TeamAsset>> {
    @Nonnull
    public static final AssetBuilderCodec<String, @NotNull TeamAsset> CODEC = AssetBuilderCodec.builder(
            TeamAsset.class,
            TeamAsset::new,
            Codec.STRING,
            (team, id) -> team.id = id,
            team -> team.id,
            (team, data) -> team.data = data,
            team -> team.data)
        .append(new KeyedCodec<>("Name", Codec.STRING),
            (team, v) -> team.name = v == null ? "" : v,
            team -> team.name)
        .documentation("The display name shown to players. Defaults to the asset id when blank.")
        .add()
        .append(new KeyedCodec<>("TeamType", new EnumCodec<>(TeamType.class)),
            (team, v) -> team.teamType = v,
            team -> team.teamType)
        .documentation("The display name shown to players. Defaults to the asset id when blank.")
        .add()
        .append(new KeyedCodec<>("Icon", Codec.STRING),
            (team, v) -> team.icon = v == null ? "" : v,
            team -> team.icon)
        .addValidator(new CommonAssetValidator("png", "UI/Custom/"))
        .documentation("UI asset team icon, for example GG/TeamIcons/Tricky_Trorks.png. Optional.")
        .add()
        .append(new KeyedCodec<>("Players", Codec.STRING_ARRAY),
            (team, v) -> team.players = v,
            team -> team.players)
        .documentation("The team roster")
        .add()
        .build();

    private static AssetStore<String, TeamAsset, DefaultAssetMap<String, TeamAsset>> ASSET_STORE;

    private AssetExtraInfo.Data data;
    @Getter private String id;
    @Getter @Nonnull private String name = "";
    @Getter @Nonnull private TeamType teamType = TeamType.Participant;
    @Getter @Nonnull private String[] players = new String[0];
    private String icon;

    public TeamAsset() {}

    @Nullable
    public static AssetStore<String, TeamAsset, DefaultAssetMap<String, TeamAsset>> getAssetStore() {
        if (ASSET_STORE == null)
            ASSET_STORE = AssetRegistry.getAssetStore(TeamAsset.class);
        return ASSET_STORE;
    }

    @Nonnull
    public static Map<String, TeamAsset> getAssetMap() {
        AssetStore<String, TeamAsset, DefaultAssetMap<String, TeamAsset>> store = getAssetStore();
        return store == null ? Map.of() : store.getAssetMap().getAssetMap();
    }
}
