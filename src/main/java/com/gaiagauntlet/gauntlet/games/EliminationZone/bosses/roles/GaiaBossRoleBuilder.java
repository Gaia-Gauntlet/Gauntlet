package com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.roles;

import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.components.BossScalingComponent;
import com.google.gson.JsonElement;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.builders.BuilderRoleVariant;
import org.jspecify.annotations.NonNull;

import javax.annotation.Nonnull;

/**
 * The {@code GaiaBoss} NPC role type: behaves exactly like the vanilla role it references and
 * carries a {@code BossScalingComponent} block that makes it a boss the match can spawn.
 */
public final class GaiaBossRoleBuilder extends BuilderRoleVariant {

    public static final String TYPE = "GaiaBoss";
    private static final String SCALING_KEY = "BossScalingComponent";
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private BossScalingComponent scaling = new BossScalingComponent();

    public static void register() {
        NPCPlugin.get().<Role>registerCoreComponentType(TYPE, GaiaBossRoleBuilder::new);
    }

    @Override
    public @NonNull Builder<Role> readConfig(@Nonnull JsonElement config) {
        if (config.isJsonObject() && config.getAsJsonObject().has(SCALING_KEY)) {
            var json = config.getAsJsonObject().get(SCALING_KEY).toString();
            try (var reader = RawJsonReader.fromBuffer(json.toCharArray())) {
                reader.consumeWhiteSpace();
                scaling = BossScalingComponent.CODEC.decodeJson(reader, new ExtraInfo());
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Could not read %s of a GaiaBoss role", SCALING_KEY);
            }
        }
        return super.readConfig(config);
    }

    @Override
    public Role build(@Nonnull BuilderSupport support) {
        var role = super.build(support);
        support.getHolder().putComponent(BossScalingComponent.getComponentType(), scaling.forRole(getIdentifier()));
        return role;
    }

    /** The boss definition this role describes, labelled with the role's name. */
    @Nonnull
    public BossScalingComponent definition(@Nonnull String roleId) {
        return scaling.forRole(roleId);
    }
}
