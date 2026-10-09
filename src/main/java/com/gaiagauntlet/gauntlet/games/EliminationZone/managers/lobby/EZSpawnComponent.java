package com.gaiagauntlet.gauntlet.games.EliminationZone.managers.lobby;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;

import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponentType;
import com.hypixel.hytale.builtin.triggervolumes.TriggerVolumesPlugin;
import com.hypixel.hytale.builtin.triggervolumes.manager.VolumeEntry;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import lombok.Getter;
import lombok.Setter;

public class EZSpawnComponent implements GameComponent {
    public static final String ID = "EZSpawnProvider";
    public static final String PARTICIPANT_TAG = "gg.spawn";
    @Getter
    @Setter
    private static GameComponentType<@NotNull EZSpawnComponent> componentType;

    private World world;

    /**
     * The array of spawn points to select from.
     */
    private Deque<Transform> spawnPoints = new ArrayDeque<>();
    private Map<UUID, Transform> assignedLocation = new HashMap<>();
    // fallback to the center of the world - this should clearly show theres an error to be fixed
    private Transform fallback = new Transform(0, 0, 0);

    public EZSpawnComponent(World world) {
        this.world = world;
    }

    public Transform getNext(PlayerRef player) {
        // get the assigned player's previous location, else find and use one based on trigger volumes
        return assignedLocation.computeIfAbsent(player.getUuid(), uuid -> {
            if (spawnPoints.isEmpty())
                collect();
            return spawnPoints.pop();
        });
    }

    @Nonnull
    private void collect() {
        spawnPoints.clear();
        var manager = world.getEntityStore().getStore()
                .getResource(TriggerVolumesPlugin.get().getManagerResourceType());
        if (manager != null) {
            for (var volume : manager.getVolumes()) {
                for (var tag : volume.getRawTags().keySet()) {
                    if (tag.equals(PARTICIPANT_TAG)) {
                        spawnPoints.add(center(volume));
                    }
                }
            }
        }

        if (spawnPoints.isEmpty())
            spawnPoints.add(fallback); // fallback in case no TVs are found
    }

    @Nonnull
    private static Transform center(@Nonnull VolumeEntry volume) {
        var min = new Vector3d();
        var max = new Vector3d();
        volume.getShape().getWorldAABB(volume.getPosition(), min, max);
        return new Transform((min.x + max.x) / 2.0, (min.y + max.y) / 2.0, (min.z + max.z) / 2.0, 0f, 0f, 0f);
    }
}
