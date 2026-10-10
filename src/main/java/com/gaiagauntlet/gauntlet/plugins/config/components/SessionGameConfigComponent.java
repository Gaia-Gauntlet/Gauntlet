package com.gaiagauntlet.gauntlet.plugins.config.components;

import com.gaiagauntlet.gauntlet.core.session.components.SessionComponent;
import com.gaiagauntlet.gauntlet.core.session.components.SessionComponentType;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponentType;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

public class SessionGameConfigComponent implements SessionComponent, GameComponent {
    public static final String ID = "SessionGameConfigComponent";

    public static BuilderCodec<@NotNull SessionGameConfigComponent> CODEC = BuilderCodec
        .builder(SessionGameConfigComponent.class, SessionGameConfigComponent::new)
        .append(new KeyedCodec<>("GameConfig", new MapCodec<>(Codec.STRING, HashMap::new, false)),
            (c, v) -> c.gameToConfigMap = v,
            c -> c.gameToConfigMap)
        .add()
        .build();

    @Getter @Setter private static SessionComponentType<SessionGameConfigComponent> sessionComponentType;
    @Getter @Setter private static GameComponentType<@NotNull SessionGameConfigComponent> gameComponentType;

    /** Map from gameId to gameConfig */
    @Getter private Map<String, String> gameToConfigMap = new HashMap<>();

    public String getConfigId(String game) {
        return gameToConfigMap.get(game);
    }
}
