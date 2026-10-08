package com.gaiagauntlet.gauntlet.utils;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.Axis;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.universe.world.World;
import lombok.Getter;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import java.util.List;

/** Places bundled prefabs at a point of interest, rotated to match the point's facing axis. Run on the world thread. */
public final class PrefabUtils {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Every prefab the plugin places. Loaded once at startup so no world thread ever pays the parse cost. */
    public static final List<String> ALL = List.of(
            "Portal/Portal.prefab.json", "Portal/ClearPortal.prefab.json",
            "Timer/Timer_Base.prefab.json", "Timer/ClearTimer.prefab.json",
            "Timer/Lightblock_Char_0.prefab.json", "Timer/Lightblock_Char_1.prefab.json",
            "Timer/Lightblock_Char_2.prefab.json", "Timer/Lightblock_Char_3.prefab.json",
            "Timer/Lightblock_Char_4.prefab.json", "Timer/Lightblock_Char_5.prefab.json",
            "Timer/Lightblock_Char_6.prefab.json", "Timer/Lightblock_Char_7.prefab.json",
            "Timer/Lightblock_Char_8.prefab.json", "Timer/Lightblock_Char_9.prefab.json");

    @Getter
    private static volatile boolean warm;
    private static final List<String> missing = new java.util.concurrent.CopyOnWriteArrayList<>();

    private PrefabUtils() {
    }

    /** Places a prefab authored facing the X axis, such as the countdown timer. */
    public static boolean place(@Nonnull World world, @Nonnull Transform point, @Nonnull String prefab) {
        return place(world, point, prefab, new Vector3i());
    }

    /**
     * Places a prefab authored facing the X axis at the point plus an offset expressed in the
     * point's local frame (x along the facing axis, z depth).
     */
    public static boolean place(@Nonnull World world, @Nonnull Transform point, @Nonnull String prefab,
            @Nonnull Vector3i localOffset) {
        return place(world, point, prefab, localOffset, Axis.X);
    }

    /** Places a prefab authored facing the Z axis, such as the portal. */
    public static boolean placeFacingZ(@Nonnull World world, @Nonnull Transform point, @Nonnull String prefab) {
        return place(world, point, prefab, new Vector3i(), Axis.Z);
    }

    /** Turns the prefab a quarter turn when the point faces across the axis its blocks were authored along. */
    private static boolean place(@Nonnull World world, @Nonnull Transform point, @Nonnull String prefab,
            @Nonnull Vector3i localOffset, @Nonnull Axis authored) {
        var path = PrefabStore.get().findAssetPrefabPath(prefab);
        if (path == null) {
            LOGGER.atWarning().log("Prefab %s is not in any asset pack", prefab);
            return false;
        }
        var selection = PrefabStore.get().getPrefab(path);
        var position = point.getPosition();
        var origin = new Vector3i((int) position.x, (int) position.y, (int) position.z);
        if (point.getAxis() != authored) {
            selection = selection.rotate(Axis.Y, -90);
            origin.add(localOffset);
        } else {
            origin.add(new Vector3i(localOffset.z, localOffset.y, localOffset.x));
        }
        if (localOffset.z < 0) {
            selection = selection.flip(point.getAxis() == Axis.X ? Axis.Z : Axis.X);
        }
        selection.place(ConsoleSender.INSTANCE, world, origin, null);
        return true;
    }
}
