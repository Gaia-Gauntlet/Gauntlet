package com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.events;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.components.BossScalingComponent;
import com.gaiagauntlet.gauntlet.plugins.events.assets.GameEventConfig;
import com.gaiagauntlet.gauntlet.plugins.events.events.SequenceEvent;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.logger.HytaleLogger;

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

    private String bossId;
    private String zoneId;

    public BossSpawnEventConfig() {
    }

    public BossSpawnEventConfig(String bossId) {
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
        var gameEventConfig = gameEvent.getGameConfig();
        if (!(gameEventConfig instanceof BossSpawnEventConfig bossEvent)) return;
        GameRef gameRef = gameEvent.getGameRef();

        String zoneId = bossEvent.getZoneId();
        String bossId = bossEvent.getBossId();

        // Spawn selected boss
        if (bossId != null && !bossId.isBlank()) {
            BossScalingComponent definition = BossRoleResolver.resolve(bossId);
            if (definition == null || !definition.isEnabled() || !definition.allowsZone(zoneId)) {
                HytaleLogger.getLogger().atWarning().log("boss: explicit BossId '" + bossId
                        + "' is not an enabled, zone-eligible boss; skipped", "event");
                return;
            }
            BossService.spawn(gameRef, bossId, zoneId);
            return;
        }

        // Spawn random boss
        var candidates = BossRoleResolver.generateRolesToBossScalingComponents().values();
        var eligible = new ArrayList<BossScalingComponent>();
        var zone = GameUtils.getGameStore().getComponent(gameRef, ZoneClosingComponent.getComponentType());
        var bossesGameComponent = GameUtils.getGameStore().getComponent(gameRef, BossesGameComponent.getComponentType());

        for (BossScalingComponent candidate : candidates) {
            for (SectorZone pendingZone : zone.getPendingZones()) {
                if (candidate.isEnabled()
                    && candidate.getZoneTags().contains(pendingZone.getId())
                    && !BossService.hasActiveBoss(gameRef, candidate.getRoleId())
                    && !zone.getActiveZone().getId().equals(pendingZone.getId())
                    && !zone.getClosedZones().stream().anyMatch(z -> z.getId().equals(pendingZone.getId()))
                    && !bossesGameComponent.getPendingSpawns().containsKey(candidate.getRoleId())
                ) {
                    eligible.add(candidate);
                }
            }
        }

        if (eligible.isEmpty()) {
            GGLogger.log(gameRef, "boss: no eligible boss configured"
                    + (zoneId == null ? "" : " for zone '" + zoneId + "'") + "; skipped", "event");
            return;
        }

        BossScalingComponent chosen = eligible.get(new Random().nextInt(eligible.size()));
        BossService.spawn(gameRef, chosen.getRoleId(), chosen.getZoneTags().getFirst());
    }
}
