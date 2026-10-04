package com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

/**
 * Boss identity and eligibility, authored under {@code BossScalingComponent} in a {@code GaiaBoss} NPC role
 * and attached to every NPC built from it.
 */
public final class BossScalingComponent implements Component<EntityStore> {

    public static final BuilderCodec<BossScalingComponent> CODEC = BuilderCodec.builder(BossScalingComponent.class, BossScalingComponent::new)
            .append(new KeyedCodec<>("DisplayNameKey", Codec.STRING), (c, v) -> c.displayNameKey = v == null ? "" : v, c -> c.displayNameKey)
            .documentation("Translation key of the boss's name.")
            .add()
            .append(new KeyedCodec<>("ZoneTags", Codec.STRING_ARRAY), (c, v) -> c.zoneTags = v == null ? new String[0] : v.clone(), c -> c.zoneTags.clone())
            .documentation("Zones the boss belongs to. Empty means any zone.")
            .add()
            .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN), (c, v) -> c.enabled = v == null || v, c -> c.enabled).add()
            .append(new KeyedCodec<>("PhysicalScale", Codec.FLOAT), (c, v) -> c.physicalScale = v == null ? 1f : v, c -> c.physicalScale)
            .documentation("Model scale applied on spawn.")
            .add()
            .build();

    private static ComponentType<EntityStore, BossScalingComponent> type;

    private String roleId = "";
    private String displayNameKey = "";
    private String[] zoneTags = new String[0];
    private boolean enabled = true;
    private float physicalScale = 1f;

    public BossScalingComponent() {
    }

    public static void setType(@Nonnull ComponentType<EntityStore, BossScalingComponent> componentType) {
        type = componentType;
    }

    @Nonnull
    public static ComponentType<EntityStore, BossScalingComponent> getComponentType() {
        return type;
    }

    @Nonnull
    public String roleId() {
        return roleId;
    }

    /**
     * The boss's name as plain text in the server's default language, for text that cannot carry a
     * translation such as an event title. Falls back to the role id.
     */
    @Nonnull
    public String displayText() {
        if (!displayNameKey.isBlank()) {
            var translated = I18nModule.get().getMessages(I18nModule.DEFAULT_LANGUAGE).get(displayNameKey);
            if (translated != null && !translated.isBlank()) {
                return translated;
            }
        }
        return roleId.replace('_', ' ');
    }

    @Nonnull
    public List<String> zoneTags() {
        return List.of(zoneTags);
    }

    /** True when the boss may spawn in the zone. Ids are compared without spaces, underscores, or case. */
    public boolean allowsZone(@Nullable String zoneId) {
        if (zoneTags.length == 0 || zoneId == null) {
            return true;
        }
        for (var tag : zoneTags) {
            if (normalize(tag).equals(normalize(zoneId))) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    public static String normalize(@Nonnull String id) {
        return id.replace("_", "").replace(" ", "").replace("-", "").toLowerCase(Locale.ROOT);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public float physicalScale() {
        return physicalScale;
    }

    /** A copy carrying the role file name that identifies the boss. */
    @Nonnull
    public BossScalingComponent forRole(@Nonnull String roleId) {
        var copy = clone();
        copy.roleId = roleId;
        return copy;
    }

    @Override
    @Nonnull
    public BossScalingComponent clone() {
        var copy = new BossScalingComponent();
        copy.roleId = roleId;
        copy.displayNameKey = displayNameKey;
        copy.zoneTags = zoneTags.clone();
        copy.enabled = enabled;
        copy.physicalScale = physicalScale;
        return copy;
    }
}
