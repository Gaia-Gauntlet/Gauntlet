package com.gaiagauntlet.gauntlet.plugins.auto.components.assets;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.plugins.auto.components.rotation.PresetRotation;
import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.validation.Validators;

import lombok.Getter;

public class AutoSession implements JsonAssetWithMap<String, DefaultAssetMap<String, AutoSession>> {
    @Nonnull
    public static final AssetBuilderCodec<String, @NotNull AutoSession> CODEC = AssetBuilderCodec.builder(
            AutoSession.class,
            AutoSession::new,
            Codec.STRING,
            (auto, id) -> auto.id = id,
            auto -> auto.id,
            (auto, data) -> auto.data = data,
            auto -> auto.data)
            .append(new KeyedCodec<>("SpinUpPlayers", Codec.INTEGER),
                    (auto, v) -> auto.spinUpPlayers = v,
                    auto -> auto.spinUpPlayers)
            .addValidator(Validators.greaterThanOrEqual(1))
            .documentation("How many players the session needs before Auto starts its next game")
            .add()
            .append(new KeyedCodec<>("GatherSeconds", Codec.INTEGER),
                    (auto, v) -> auto.gatherSeconds = v,
                    auto -> auto.gatherSeconds)
            .addValidator(Validators.greaterThanOrEqual(0))
            .documentation("The wait before starting from an idle session")
            .add()
            .append(new KeyedCodec<>("IntermissionSeconds", Codec.INTEGER),
                    (auto, v) -> auto.intermissionSeconds = v,
                    auto -> auto.intermissionSeconds)
            .addValidator(Validators.greaterThanOrEqual(0))
            .documentation(
                    "The break in the hub between one game finishing and the next starting. It's an intermission, what more do you want?     ")
            .add()
            .append(new KeyedCodec<>("IdleGraceSeconds", Codec.INTEGER),
                    (auto, v) -> auto.idleGraceSeconds = v,
                    auto -> auto.idleGraceSeconds)
            .addValidator(Validators.greaterThanOrEqual(0))
            .documentation("How long a running game may have nobody in the session before Auto stops it")
            .add()
            .append(new KeyedCodec<>("SetupTimeoutSeconds", Codec.INTEGER),
                    (auto, v) -> auto.setupTimeoutSeconds = v,
                    auto -> auto.setupTimeoutSeconds)
            .addValidator(Validators.greaterThanOrEqual(1))
            .documentation("How long a game may take to set up before Auto abandons it")
            .add()
            .append(new KeyedCodec<>("CleanTimeoutSeconds", Codec.INTEGER),
                    (auto, v) -> auto.cleanTimeoutSeconds = v,
                    auto -> auto.cleanTimeoutSeconds)
            .addValidator(Validators.greaterThanOrEqual(1))
            .documentation("How long a game may take to end and clean up before Auto abandons it")
            .add()
            .append(new KeyedCodec<>("RetrySeconds", Codec.INT_ARRAY),
                    (auto, v) -> auto.retrySeconds = v,
                    auto -> auto.retrySeconds)
            .documentation(
                    "The wait before each retry after a failure, in order. Auto pauses the session once these run out. Resets on an event")
            .add()
            .append(new KeyedCodec<>("Rotation", PresetRotation.CODEC),
                    (auto, v) -> auto.rotation = v,
                    auto -> auto.rotation)
            .documentation(
                    "How the next SessionPreset is chosen once the queue is empty. Without one, Auto only plays what admins queue")
            .add()
            .build();

    private static AssetStore<String, AutoSession, DefaultAssetMap<String, AutoSession>> ASSET_STORE;

    private AssetExtraInfo.Data data;
    @Getter
    private String id;
    @Getter
    @Nullable
    private PresetRotation rotation;
    @Getter
    private int spinUpPlayers = 1;
    @Getter
    private int gatherSeconds = 5;
    @Getter
    private int intermissionSeconds = 15;
    @Getter
    private int idleGraceSeconds = 60;
    @Getter
    private int setupTimeoutSeconds = 60;
    @Getter
    private int cleanTimeoutSeconds = 30;
    @Getter
    private int[] retrySeconds = { 5, 15, 60 };

    @Nullable
    public static AssetStore<String, AutoSession, DefaultAssetMap<String, AutoSession>> getAssetStore() {
        if (ASSET_STORE == null)
            ASSET_STORE = AssetRegistry.getAssetStore(AutoSession.class);
        return ASSET_STORE;
    }

    @Nonnull
    public static Map<String, AutoSession> getAssetMap() {
        var store = getAssetStore();
        return store == null ? Map.of() : store.getAssetMap().getAssetMap();
    }
}
