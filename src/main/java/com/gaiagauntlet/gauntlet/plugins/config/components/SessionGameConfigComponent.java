package com.gaiagauntlet.gauntlet.plugins.config.components;

import com.gaiagauntlet.gauntlet.core.session.components.SessionComponent;
import com.gaiagauntlet.gauntlet.core.session.components.SessionComponentType;
import com.gaiagauntlet.gauntlet.plugins.config.components.assets.GameConfigAsset;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class SessionGameConfigComponent implements SessionComponent {
    public static final String ID = "SessionGameConfigComponent";

    public static BuilderCodec<SessionGameConfigComponent> CODEC = BuilderCodec
        .builder(SessionGameConfigComponent.class, SessionGameConfigComponent::new)
        .append(new KeyedCodec<>("GameConfig", new MapCodec<>(Codec.STRING, HashMap::new, false)),
            (c, v) -> c.gameToConfigMap = v,
            c -> c.gameToConfigMap)
        .add()
        .build();

    @Getter @Setter private static SessionComponentType<SessionGameConfigComponent> componentType;

    /** Map from gameId to gameConfig */
    @Getter private Map<String, String> gameToConfigMap = new HashMap<>();

    public String getConfig(String game) {
        return gameToConfigMap.get(game);
    }
}
