package com.gaiagauntlet.gauntlet.plugins.config.components;

import com.gaiagauntlet.gauntlet.plugins.config.components.assets.GameConfigAsset;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponentType;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import lombok.Getter;
import lombok.Setter;

public class GameConfigComponent implements GameComponent {
    public static final String ID = "GameConfigComponent";

    @Getter @Setter private static GameComponentType<GameConfigComponent> componentType;
    @Setter private String gameConfigAsset;

    public static BuilderCodec<GameConfigComponent> CODEC = BuilderCodec
        .builder(GameConfigComponent.class, GameConfigComponent::new)
        .append(new KeyedCodec<>("GameConfig", Codec.STRING),
            GameConfigComponent::setGameConfigAsset,
            c -> c.gameConfigAsset)
        .addValidator(new AssetKeyValidator<>(GameConfigAsset::getAssetStore))
        .add()
        .build();

    private GameConfigComponent() {}

    public GameConfigComponent(String gameConfigAsset) {
        this.gameConfigAsset = gameConfigAsset;
    }

    public GameConfigAsset getConfig() {
        return GameConfigAsset.getAssetMap().get(gameConfigAsset);
    }
}
