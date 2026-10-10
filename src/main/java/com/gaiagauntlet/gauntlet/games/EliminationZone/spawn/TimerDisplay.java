package com.gaiagauntlet.gauntlet.games.EliminationZone.spawn;

import com.gaiagauntlet.gauntlet.utils.PrefabUtils;
import com.hypixel.hytale.math.Axis;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.world.World;
import org.joml.Vector3i;

import javax.annotation.Nonnull;

/** The in-world block countdown display: four lightblock digits on both faces of a base prefab. Run on the world thread. */
public final class TimerDisplay {

    private static final String BASE = "Timer/Timer_Base.prefab.json";
    private static final String CLEAR = "Timer/ClearTimer.prefab.json";
    private static final Vector3i BASE_OFFSET = new Vector3i(-4, -2, -2);

    private TimerDisplay() {
    }

    public static void showBase(@Nonnull World world, @Nonnull Transform point) {
        PrefabUtils.place(world, point, BASE, new Vector3i(BASE_OFFSET));
    }

    public static void clear(@Nonnull World world, @Nonnull Transform point) {
        PrefabUtils.place(world, point, CLEAR, new Vector3i(BASE_OFFSET));
    }

    public static void showTime(@Nonnull World world, @Nonnull Transform point, int totalSeconds) {
        int minutes = Math.max(0, totalSeconds) / 60;
        int seconds = Math.max(0, totalSeconds) % 60;
        var m = String.format("%02d", Math.min(minutes, 99));
        var s = String.format("%02d", seconds);
        if (point.getAxis() == Axis.X) {
            digit(world, point, s.charAt(1), -14, 0);
            digit(world, point, s.charAt(0), -8, 0);
            digit(world, point, s.charAt(1), 6, -4);
            digit(world, point, s.charAt(0), 0, -4);
            digit(world, point, m.charAt(1), 0, 0);
            digit(world, point, m.charAt(0), 6, 0);
            digit(world, point, m.charAt(1), -8, -4);
            digit(world, point, m.charAt(0), -14, -4);
        } else {
            digit(world, point, s.charAt(1), 6, 0);
            digit(world, point, s.charAt(0), 0, 0);
            digit(world, point, s.charAt(1), -14, -4);
            digit(world, point, s.charAt(0), -8, -4);
            digit(world, point, m.charAt(1), -8, 0);
            digit(world, point, m.charAt(0), -14, 0);
            digit(world, point, m.charAt(1), 0, -4);
            digit(world, point, m.charAt(0), 6, -4);
        }
    }

    private static void digit(@Nonnull World world, @Nonnull Transform point, char digit, int along, int depth) {
        PrefabUtils.place(world, point, "Timer/Lightblock_Char_" + digit + ".prefab.json", new Vector3i(along, 0, depth));
    }
}
