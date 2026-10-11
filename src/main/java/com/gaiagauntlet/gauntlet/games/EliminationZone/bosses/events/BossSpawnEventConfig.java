package com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.events;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.BossSpawner;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.BossSpawner.SpawnPoint;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.components.BossScalingComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.components.BossesComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.utils.BossUtils;
import com.gaiagauntlet.gauntlet.plugins.events.assets.GameEventConfig;
import com.gaiagauntlet.gauntlet.plugins.events.events.SequenceEvent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;

import lombok.Getter;

public class BossSpawnEventConfig extends GameEventConfig {
    public static final String Id = "BossSpawnEvent";

    public static final BuilderCodec<@NotNull BossSpawnEventConfig> CODEC = BuilderCodec
            .builder(BossSpawnEventConfig.class, BossSpawnEventConfig::new, BASE_CODEC)
            .append(new KeyedCodec<String>("BossId", Codec.STRING),
                    (event, value) -> event.bossId = value == null ? "" : value,
                    event -> event.bossId)
            .add()
            .append(new KeyedCodec<String>("ZoneId", Codec.STRING),
                    (event, value) -> event.zoneId = value == null ? "" : value,
                    event -> event.zoneId)
            .add()
            .build();

    @Getter
    private String bossId;
    @Getter
    private String zoneId;

    public BossSpawnEventConfig() {
    }

    public BossSpawnEventConfig(String bossId) {
        this.bossId = bossId;
    }

    @Override
    public String getId() {
        return Id;
    }

    private static void put(Map<String, String> params, String key, String value) {
        if (value != null && !value.isBlank()) {
            params.put(key, value);
        }
    }

    public static void execute(SequenceEvent gameEvent) {
        var gameEventConfig = gameEvent.getConfig();
        if (!(gameEventConfig instanceof BossSpawnEventConfig bossEvent))
            return;
        var bossId = bossEvent.getBossId();
        var game = gameEvent.getStore();
        var sessionId = gameEvent.getSessionId();
        if (bossId == null || bossId.isBlank()) {
            BossSpawner.spawnRandom(sessionId, GameStore.getContext(game).getWorld());
            return;
        }
        BossSpawner.spawn(sessionId, game, bossId);
    }

    @Override
    public String describe() {
        return bossId == null || bossId.isBlank() ? "Random boss" : "Boss " + bossId;
    }
}
