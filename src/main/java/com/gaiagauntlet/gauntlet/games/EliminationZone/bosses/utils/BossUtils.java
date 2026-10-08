package com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.utils;

import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.roles.GaiaBossRoleBuilder;
import com.gaiagauntlet.gauntlet.games.EliminationZone.bosses.components.BossScalingComponent;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.asset.builder.BuilderInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public final class BossUtils {

    private BossUtils() {
    }

    /** Every loaded {@code GaiaBoss} role, by role name. */
    @Nonnull
    public static Map<String, BossScalingComponent> getBosses() {
        var infos = new ArrayList<>(NPCPlugin.get().getBuilderManager().getAllBuilders().values());
        infos.sort(Comparator.comparing(BuilderInfo::getKeyName, Comparator.nullsLast(Comparator.naturalOrder())));
        var result = new LinkedHashMap<String, BossScalingComponent>();
        for (var info : infos) {
            if (info == null || info.isRemoved() || !info.isValid() || !(info.getBuilder() instanceof GaiaBossRoleBuilder builder)) {
                continue;
            }
            var roleId = info.getKeyName();
            if (roleId != null && !roleId.isBlank()) {
                result.put(roleId, builder.definition(roleId));
            }
        }
        return result;
    }

    /** The boss by role name, matched ignoring case, underscores, and spaces. */
    @Nullable
    public static BossScalingComponent getBoss(@Nonnull String roleId) {
        var wanted = BossScalingComponent.normalize(roleId);
        for (var entry : getBosses().entrySet()) {
            if (BossScalingComponent.normalize(entry.getKey()).equals(wanted)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Nonnull
    public static List<String> getBossNames() {
        return new ArrayList<>(getBosses().keySet());
    }
}
