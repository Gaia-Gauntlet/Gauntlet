package com.gaiagauntlet.gauntlet.games.EliminationZone;

import com.gaiagauntlet.gauntlet.games.EliminationZone.components.GGPoi;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components.ZoneDefinition;
import com.gaiagauntlet.gauntlet.plugins.config.components.assets.GameConfigAsset;
import com.hypixel.hytale.assetstore.map.AssetMapWithIndexes;
import com.hypixel.hytale.builtin.instances.InstanceValidator;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.validation.ValidationResults;
import com.hypixel.hytale.codec.validation.Validator;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.codec.validation.validator.MapValueValidator;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class EZGameConfigAsset extends GameConfigAsset {

    public static final Validator<String> VOID_TARGET_VALIDATOR = new Validator<>() {
        @Override
        public void accept(String value, ValidationResults results) {
            if (value == null || value.isBlank() || Fluid.getAssetMap().getIndex(value) != AssetMapWithIndexes.NOT_FOUND) return;
            BlockType.VALIDATOR_CACHE.getValidator().accept(value, results);
        }

        @Override
        public void updateSchema(SchemaContext context, Schema schema) {
            BlockType.VALIDATOR_CACHE.getValidator().updateSchema(context, schema);
        }
    };

    public static final BuilderCodec<@NotNull EZGameConfigAsset> CODEC = BuilderCodec
        .builder(EZGameConfigAsset.class, EZGameConfigAsset::new, GameConfigAsset.ABSTRACT_CODEC)
        .append(new KeyedCodec<>("InstanceTemplateName", Codec.STRING),
            (t, v) -> t.instanceTemplateName = v,
            EZGameConfigAsset::getInstanceTemplateName)
        .addValidator(InstanceValidator.INSTANCE)
        .documentation("The name of the instance to use for this game.")
        .add()
        .append(new KeyedCodec<>("FriendlyFireEnabled", Codec.BOOLEAN),
            (t, v) -> t.friendlyFireEnabled = v,
            EZGameConfigAsset::isFriendlyFireEnabled)
        .documentation("When enabled, teammates can damage each other during team matches.")
        .add()
        .append(new KeyedCodec<>("Zones", new ArrayCodec<>(ZoneDefinition.CODEC, ZoneDefinition[]::new)),
            (t, v) -> t.zones = v,
            EZGameConfigAsset::getZones)
        .documentation("Circle segment arena zones")
        .add()
//        .append(new KeyedCodec<>("WeatherPool",
//                new ArrayCodec<>(WeatherPoolOptionComponent.CODEC,
//                    WeatherPoolOptionComponent[]::new)),
//            (t, v) -> t.weatherPoolOptions = v,
//            EZGameManager::getWeatherPoolOptions)
//        .documentation("Pool of weather options to use during the game.")
//        .add()
        .append(new KeyedCodec<>("ClosingVoidBlockMap",
                new MapCodec<>(Codec.STRING, LinkedHashMap::new)),
            (p, v) -> p.closingVoidBlockMap = v == null ? Map.of() : Map.copyOf(v),
            p -> p.closingVoidBlockMap)
        .documentation("Void replacement map shown in Asset Editor. Values are BlockType assets. "
            + "Keys may be exact source BlockType IDs or the geometry categories Default, Solid, "
            + "Half, ..., LightCeiling, and Fluid. Fluid also accepts exact Fluid asset IDs such "
            + "as Water_Source. Exact source IDs take priority. A value may name a Fluid asset "
            + "instead of a BlockType, which swaps the fluid in place and leaves the volume "
            + "liquid; anything else drains the fluid and writes the block.")
        .addValidator(new MapValueValidator<>(VOID_TARGET_VALIDATOR))
        .add()
        .append(new KeyedCodec<>("ClosingVoidBlock", Codec.STRING),
            (p, v) -> p.closingVoidBlock = v,
            p -> p.closingVoidBlock)
        .documentation("BlockType placed over the ground the closing area has sealed, so the "
            + "boundary is visible in-world instead of only on the HUD. The disposable match "
            + "instance is discarded at match end, so replaced terrain is not restored. "
            + "Use a solid, opaque block; Build_Black_Cube is a placeholder until a purpose-made "
            + "void block exists.")
        .add()
        .append(new KeyedCodec<>("ZoneClosingPhaseSet", Codec.STRING),
            (p, v) -> p.zoneClosingPhaseSet = v,
            p -> p.zoneClosingPhaseSet)
        .documentation("Controls each zone's close, hold, warning, damage, and audio sequence.")
        .add()
//        .append(new KeyedCodec<>("GameEvents", GameEvents.CODEC),
//            (p, v) -> p.gameEvents = v,
//            p -> p.gameEvents)
//        .documentation("Controls each zone's close, hold, warning, damage, and audio sequence.")
//        .add()
        .append(new KeyedCodec<>("SpawnProtectionSeconds", Codec.DOUBLE),
            (p, v) -> p.spawnProtectionSeconds = v,
            p -> p.spawnProtectionSeconds)
        .documentation("Controls each zone's close, hold, warning, damage, and audio sequence.")
        .add()
        .append(new KeyedCodec<>("SuddenDeathAt", Codec.INTEGER),
            (p, v) -> p.suddenDeathAt = v,
            p -> p.suddenDeathAt)
        .addValidator(Validators.greaterThanOrEqual(0))
        .documentation("The number of players remaining where sudden death is activated")
        .add()
        .append(new KeyedCodec<>("CameraSequenceSeconds", Codec.DOUBLE),
            (p, v) -> p.cameraSequenceSeconds = v,
            p -> p.cameraSequenceSeconds)
        .documentation("Controls how long the intro sequence is.")
        .add()
        .append(new KeyedCodec<>("CornucopiaDurationSeconds", Codec.LONG),
            (p, v) -> p.cornucopiaDurationSeconds = v,
            p -> p.cornucopiaDurationSeconds)
        .documentation("How long the cornucopia cutscene is.")
        .add()
        .append(new KeyedCodec<>("StartBufferSeconds", Codec.LONG),
            (p, v) -> p.startBufferSeconds = v,
            p -> p.startBufferSeconds)
        .documentation(
            "Time to wait before starting the arena intro sequence. Used to ensure all players have fully loaded into the game.")
        .add()
        .append(new KeyedCodec<>("TeleportWaveSize", Codec.INTEGER),
            (p, v) -> p.teleportWaveSize = v,
            p -> p.teleportWaveSize)
        .addValidator(Validators.greaterThanOrEqual(1))
        .documentation(
            "How many players are teleported into the game world together in each wave.")
        .add()
        .append(new KeyedCodec<>("TeleportWaveIntervalSeconds", Codec.LONG),
            (p, v) -> p.teleportWaveIntervalSeconds = v,
            p -> p.teleportWaveIntervalSeconds)
        .addValidator(Validators.greaterThanOrEqual(0L))
        .documentation(
            "Delay between each teleport wave, so the world is not loading chunks for every player at once.")
        .add()
        // apologies - there's a better key-val map way of doing this but i'm feeling
        // lazy today and am just copy-pasting this from the legacy code. There's really
        // no diff
        .append(new KeyedCodec<>("ArenaTimerList", new ArrayCodec<>(GGPoi.CODEC, GGPoi[]::new)),
            (m, v) -> m.arenaTimerPois = v,
            m -> m.arenaTimerPois)
        .documentation("North-facing arena countdown display. Coordinates only - the world recorded "
            + "here is ignored, because the arena display is always rendered into the live match "
            + "world, which is a fresh instance every match.")
        .add()
        .append(new KeyedCodec<>("ZoneTickSeconds", Codec.FLOAT),
            (p, v) -> p.zoneTickSeconds = v,
            p -> p.zoneTickSeconds)
        .addValidator(Validators.greaterThanOrEqual(0F))
        .documentation("How often the zone should update, in seconds. This only affects the rate at "
            + "which it updates, it does not affect the speed.")
        .add()
        .append(new KeyedCodec<>("MaxActiveBosses", Codec.INTEGER),
            (p, v) -> p.maxActiveBosses = v,
            p -> p.maxActiveBosses)
        .addValidator(Validators.greaterThanOrEqual(0))
        .documentation("How many bosses can be active in the map at any given time.")
        .add()
        .build();

    @Getter private String instanceTemplateName = "GGEliminationZone";
    @Getter private boolean friendlyFireEnabled = false;
    @Getter private ZoneDefinition[] zones = new ZoneDefinition[0];
//    @Getter private WeatherPoolOptionComponent[] weatherPoolOptions = new WeatherPoolOptionComponent[0];
    @Getter private Map<String, String> closingVoidBlockMap = Map.of();
    @Getter private String closingVoidBlock = "Build_Black_Cube";
    @Getter private String zoneClosingPhaseSet = "";
    @Getter private int suddenDeathAt = 6;
    @Getter private double spawnProtectionSeconds = 30;
    @Getter private double cameraSequenceSeconds = 29;
    @Getter private long startBufferSeconds = 20;
    @Getter private int teleportWaveSize = 5;
    @Getter private long teleportWaveIntervalSeconds = 1;
    @Getter private long cornucopiaDurationSeconds = 27;
//    @Getter private GameEvents gameEvents;
    @Getter private GGPoi[] arenaTimerPois;
    @Getter private float zoneTickSeconds = 2;
    @Getter private int maxActiveBosses = 3;
}
