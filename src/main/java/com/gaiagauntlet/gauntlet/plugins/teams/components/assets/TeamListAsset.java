package com.gaiagauntlet.gauntlet.plugins.teams.components.assets;

import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamType;
import com.gaiagauntlet.gauntlet.plugins.teams.editor.UsernameTransformButton;
import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIButton;
import com.hypixel.hytale.codec.schema.metadata.ui.UISidebarButtons;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TeamListAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, TeamListAsset>> {
    @Nonnull
    public static final AssetBuilderCodec<String, @NotNull TeamListAsset> CODEC = AssetBuilderCodec.builder(
            TeamListAsset.class,
            TeamListAsset::new,
            Codec.STRING,
            (team, id) -> team.id = id,
            team -> team.id,
            (team, data) -> team.data = data,
            team -> team.data)
            .metadata(new UISidebarButtons(
                    new UIButton(UsernameTransformButton.BUTTON_TEXT_ID,
                            UsernameTransformButton.BUTTON_ID)))
            .append(new KeyedCodec<>("TeamList", new MapCodec<>(TeamComponent.CODEC, HashMap::new)),
                    (team, v) -> team.teamList = v,
                    team -> team.teamList)
            .documentation("The full list of teams in this preset.")
            .add()
            .build();

    private static AssetStore<String, TeamListAsset, DefaultAssetMap<String, TeamListAsset>> ASSET_STORE;

    private AssetExtraInfo.Data data;
    @Getter private String id;
    @Nonnull @Getter private Map<String, TeamComponent> teamList;

    public TeamListAsset() {
    }
    public TeamListAsset(TeamListAsset other) {
        data = other.data;
        id = other.id;
        teamList = new ConcurrentHashMap<>(other.teamList.size());
        for (var team : other.teamList.entrySet()) {
            teamList.put(team.getKey(), team.getValue().clone());
        }
    }

    @Nullable
    public static AssetStore<String, TeamListAsset, DefaultAssetMap<String, TeamListAsset>> getAssetStore() {
        if (ASSET_STORE == null)
            ASSET_STORE = AssetRegistry.getAssetStore(TeamListAsset.class);
        return ASSET_STORE;
    }

    @Nonnull
    public static Map<String, TeamListAsset> getAssetMap() {
        AssetStore<String, TeamListAsset, DefaultAssetMap<String, TeamListAsset>> store = getAssetStore();
        return store == null ? Map.of() : store.getAssetMap().getAssetMap();
    }

    public TeamListAsset clone() {
        return new TeamListAsset(this);
    }
}
