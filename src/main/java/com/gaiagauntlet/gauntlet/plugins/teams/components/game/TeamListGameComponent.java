package com.gaiagauntlet.gauntlet.plugins.teams.components.game;

import com.gaiagauntlet.gauntlet.core.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamType;
import com.gaiagauntlet.gauntlet.plugins.teams.components.assets.TeamListAsset;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.EliminatedComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.editor.UsernameTransformButton;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIButton;
import com.hypixel.hytale.codec.schema.metadata.ui.UISidebarButtons;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

public final class TeamListGameComponent implements GameComponent {
    @Nonnull public static final BuilderCodec<TeamListGameComponent> CODEC = AssetBuilderCodec
        .builder(
            TeamListGameComponent.class,
            TeamListGameComponent::new
        )
        .append(new KeyedCodec<>("TeamList", new MapCodec<>(TeamComponent.CODEC, ConcurrentHashMap::new)),
            (team, v) -> team.teamList = v,
            team -> team.teamList)
        .documentation("The full list of teams in this game.")
        .add()
        .build();

    @Nonnull private Map<String, TeamComponent> teamList = new ConcurrentHashMap<>();

    // add more here, since this is not enough

}
