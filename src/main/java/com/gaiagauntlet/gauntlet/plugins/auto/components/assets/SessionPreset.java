package com.gaiagauntlet.gauntlet.plugins.auto.components.assets;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.gaiagauntlet.gauntlet.core.session.components.SessionComponent;
import com.gaiagauntlet.gauntlet.core.session.registry.SessionRegistry;
import com.gaiagauntlet.gauntlet.utils.codec.StringRegistryCodec;
import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.validation.ValidationResults;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.codec.validation.Validator;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;

import lombok.Getter;

public class SessionPreset implements JsonAssetWithMap<String, DefaultAssetMap<String, SessionPreset>> {
    private static final StringRegistryCodec<SessionComponent, Map<String, SessionComponent>> COMPONENTS_CODEC = new StringRegistryCodec<>(
            new SessionRegistry(), HashMap::new, true);

    // copied from the hytale item - this heckin thing is PRIVATE and NOT AVAILABLE
    // - WHY?
    private static final Validator<String> SERVER_LANG_KEY_VALIDATOR = new Validator<>() {
        @Override
        public void accept(@Nullable String key, @Nonnull ValidationResults results) {
            if (key == null || !key.startsWith("server."))
                return;

            var i18n = I18nModule.get();
            if (i18n == null)
                return;

            if (!i18n.getMessages(I18nModule.DEFAULT_LANGUAGE).containsKey(key)) {
                results.warn("[LOC] Key '" + key + "' does not exist in server.lang!");
            }
        }

        @Override
        public void updateSchema(SchemaContext context, Schema target) {
        }
    };

    @Nonnull
    public static final AssetBuilderCodec<String, @NotNull SessionPreset> CODEC = AssetBuilderCodec.builder(
            SessionPreset.class,
            SessionPreset::new,
            Codec.STRING,
            (preset, id) -> preset.id = id,
            preset -> preset.id,
            (preset, data) -> preset.data = data,
            preset -> preset.data)
            .append(new KeyedCodec<>("Title", Codec.STRING),
                    (preset, v) -> preset.title = v,
                    preset -> preset.title)
            .addValidator(Validators.nonNull())
            .addValidator(SERVER_LANG_KEY_VALIDATOR)
            .documentation("Lang key of the name players see when voting or waiting for this preset")
            .add()
            .append(new KeyedCodec<>("Description", Codec.STRING),
                    (preset, v) -> preset.description = v,
                    preset -> preset.description)
            .documentation("Lang key of the line players see under the title")
            .addValidator(SERVER_LANG_KEY_VALIDATOR)
            .add()
            .append(new KeyedCodec<>("Game", Codec.STRING),
                    (preset, v) -> preset.game = v,
                    preset -> preset.game)
            .addValidator(Validators.nonNull())
            .documentation("The game queued when this preset is applied")
            .add()
            .append(new KeyedCodec<>("Components", COMPONENTS_CODEC),
                    (preset, v) -> preset.components = v,
                    preset -> preset.components)
            .documentation("Session components put on the session as-is")
            .add()
            .build();

    private static AssetStore<String, SessionPreset, DefaultAssetMap<String, SessionPreset>> ASSET_STORE;

    /** Validates fields that name presets, and makes the asset editor offer them as a picker */
    @Nonnull
    public static final ValidatorCache<String> VALIDATOR_CACHE = new ValidatorCache<>(
            new AssetKeyValidator<>(SessionPreset::getAssetStore));

    private AssetExtraInfo.Data data;
    @Getter
    private String id;
    @Getter
    private String title;
    @Getter
    @Nullable
    private String description;
    @Getter
    private String game;
    private Map<String, SessionComponent> components = Map.of();

    /**
     * Copies out to the session, so that the session can then mutate them
     */
    @Nonnull
    public Map<String, SessionComponent> components() {
        var extraInfo = new ExtraInfo();
        return COMPONENTS_CODEC.decode(COMPONENTS_CODEC.encode(components, extraInfo), extraInfo);
    }

    @Nullable
    public static AssetStore<String, SessionPreset, DefaultAssetMap<String, SessionPreset>> getAssetStore() {
        if (ASSET_STORE == null)
            ASSET_STORE = AssetRegistry.getAssetStore(SessionPreset.class);
        return ASSET_STORE;
    }

    @Nonnull
    public static Map<String, SessionPreset> getAssetMap() {
        var store = getAssetStore();
        return store == null ? Map.of() : store.getAssetMap().getAssetMap();
    }
}
