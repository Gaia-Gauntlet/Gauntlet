package com.gaiagauntlet.gauntlet.plugins.spectator.systems;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.plugins.spectator.SpectatorCamera;
import com.gaiagauntlet.gauntlet.plugins.spectator.components.SpectatorComponent;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.CameraManager;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.gamemode.GameModeTypes;
import com.hypixel.hytale.server.core.modules.entity.player.CreativeEraserSystems;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/** ECS systems that react to the {@link SpectatorComponent} component and hide spectators from each other. */
public final class SpectatingSystems {

    /** Root interactions under Server/Item/RootInteractions, each running the team spectate control. */
    private static final String PREVIOUS_ROOT = "GG_SpectatePrevious";
    private static final String NEXT_ROOT = "GG_SpectateNext";
    private static final String FREEFLY_ROOT = "GG_SpectateFreefly";

    private SpectatingSystems() {
    }

    /** Takes the spectate controls off a player who is no longer spectating. */
    public static void unbindControls(@Nonnull Ref<EntityStore> ref, @Nonnull ComponentAccessor<EntityStore> accessor) {
        var interactions = accessor.getComponent(ref, Interactions.getComponentType());
        if (interactions != null) {
            stripControls(interactions);
        }
    }

    /**
     * Removes the spectate controls from a set of bindings. The bindings component itself stays, even
     * empty: a client learns of it only while it is on the player when they enter a world, so one added
     * later never reaches them.
     */
    public static void stripControls(@Nonnull Interactions interactions) {
        if (PREVIOUS_ROOT.equals(interactions.getInteractionId(InteractionType.Ability1))) {
            interactions.removeInteractionId(InteractionType.Ability1);
        }
        if (NEXT_ROOT.equals(interactions.getInteractionId(InteractionType.Ability2))) {
            interactions.removeInteractionId(InteractionType.Ability2);
        }
        if (FREEFLY_ROOT.equals(interactions.getInteractionId(InteractionType.Ability3))) {
            interactions.removeInteractionId(InteractionType.Ability3);
        }
    }

    /** Enters and leaves the spectator game mode and keeps the follow camera on a valid target. */
    public static final class OnSpectatorChange extends RefChangeSystem<EntityStore, SpectatorComponent> {

        @Override
        public ComponentType<EntityStore, SpectatorComponent> componentType() {
            return SpectatorComponent.getComponentType();
        }

        @Nonnull
        @Override
        public Query<EntityStore> getQuery() {
            return Player.getComponentType();
        }

        @Override
        public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull SpectatorComponent component,
                @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            GameModeTypes.enter(ref, commandBuffer, SpectatorComponent.GAME_MODE);

            var player = store.getComponent(ref, PlayerRef.getComponentType());
            if (player == null) return;
            GameSession session = GauntletUtils.sessionFor(player).orElse(null);
            if (session == null) return;
            String sessionId = session.getId();

            retarget(sessionId, ref, component.target(), commandBuffer);
            // Anyone who was watching this player needs someone new to watch.
            for (var follower : SpectatorCamera.followersOf(ref)) {
                SpectatorCamera.forget(follower);
                retarget(sessionId, follower, null, commandBuffer);
            }
        }

        @Override
        public void onComponentSet(@Nonnull Ref<EntityStore> ref, @Nullable SpectatorComponent old, @Nonnull SpectatorComponent current,
                                   @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            var player = store.getComponent(ref, PlayerRef.getComponentType());
            if (player == null) return;
            GameSession session = GauntletUtils.sessionFor(player).orElse(null);
            if (session == null) return;
            String sessionId = session.getId();

            // Compared with whom the camera follows now, which freefly clears, not with the old marker's request.
            if (current.target() == null || !Objects.equals(SpectatorCamera.following(ref), current.target())) {
                retarget(sessionId, ref, current.target(), commandBuffer);
            }
        }

        @Override
        public void onComponentRemoved(@Nonnull Ref<EntityStore> ref, @Nonnull SpectatorComponent component,
                @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            CreativeEraserSystems.refresh(ref, commandBuffer);
            SpectatorCamera.stop(ref, SpectatorCamera.following(ref), commandBuffer);
            SpectatorCamera.forget(ref);
            unbindControls(ref, commandBuffer);
            var player = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
            var camera = commandBuffer.getComponent(ref, CameraManager.getComponentType());
            if (player != null && camera != null) {
                camera.resetCamera(player);
            }
        }

        /** Follows the requested target, or the first player the spectating rules allow. */
        private static void retarget(@Nonnull String sessionId, @Nonnull Ref<EntityStore> ref, @Nullable Ref<EntityStore> target, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            if (target != null && target.isValid()) {
                SpectatorCamera.follow(ref, target, commandBuffer);
                return;
            }
            SpectatorCamera.followFirst(sessionId, ref, commandBuffer);
        }
    }

    /**
     * Keeps previous and next on the ability keys, Q and E by default, for every spectator, and freefly
     * on R once a spectator may use it, which can change mid-match when their last teammate falls.
     * Entering spectating through a death changes the same bindings in the same tick, so they are
     * checked every tick rather than set once.
     */
    public static final class SpectatorControls extends EntityTickingSystem<EntityStore> {

        private final Query<EntityStore> query = Query.and(SpectatorComponent.getComponentType(), PlayerRef.getComponentType());

        @Nonnull
        @Override
        public Query<EntityStore> getQuery() {
            return query;
        }

        @Override
        public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
                @Nonnull CommandBuffer<EntityStore> commandBuffer) {

            var ref = chunk.getReferenceTo(index);
            var player = store.getComponent(ref, PlayerRef.getComponentType());
            if (player == null) return;
            GameSession session = GauntletUtils.sessionFor(player).orElse(null);
            if (session == null) return;
            String sessionId = session.getId();
            var world = store.getExternalData().getWorld();

            var existing = commandBuffer.getComponent(ref, Interactions.getComponentType());
            var interactions = existing != null ? existing : new Interactions();
            bind(interactions, InteractionType.Ability1, PREVIOUS_ROOT);
            bind(interactions, InteractionType.Ability2, NEXT_ROOT);
            if (sessionId != null && SpectatorCamera.canFreefly(world, sessionId, ref, commandBuffer)) {
                bind(interactions, InteractionType.Ability3, FREEFLY_ROOT);
            } else if (FREEFLY_ROOT.equals(interactions.getInteractionId(InteractionType.Ability3))) {
                interactions.removeInteractionId(InteractionType.Ability3);
            }
            if (existing == null) {
                commandBuffer.putComponent(ref, Interactions.getComponentType(), interactions);
            }
        }

        private static void bind(@Nonnull Interactions interactions, @Nonnull InteractionType type, @Nonnull String root) {
            if (!root.equals(interactions.getInteractionId(type))) {
                interactions.setInteractionId(type, root);
            }
        }
    }

    /**
     * Keeps a following spectator's own entity near whoever they watch. The camera follows the target by
     * itself, but what the spectator hears and which chunks load go by where their entity stands.
     */
    public static final class FollowTarget extends EntityTickingSystem<EntityStore> {

        private static final double RESYNC_DISTANCE_SQ = 8.0 * 8.0;

        private final Query<EntityStore> query = Query.and(SpectatorComponent.getComponentType(), Player.getComponentType(),
                TransformComponent.getComponentType());

        @Nonnull
        @Override
        public Query<EntityStore> getQuery() {
            return query;
        }

        @Override
        public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
                @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            var ref = chunk.getReferenceTo(index);
            var target = SpectatorCamera.following(ref);
            if (target == null || !target.isValid() || target.getStore() != store) {
                return;
            }
            var targetTransform = commandBuffer.getComponent(target, TransformComponent.getComponentType());
            var transform = chunk.getComponent(index, TransformComponent.getComponentType());
            if (targetTransform == null || transform == null) {
                return;
            }
            if (transform.getPosition().distanceSquared(targetTransform.getPosition()) > RESYNC_DISTANCE_SQ) {
                commandBuffer.putComponent(ref, Teleport.getComponentType(),
                        Teleport.createForPlayer(targetTransform.getPosition(), targetTransform.getRotation()));
            }
        }
    }

    /** Removes spectators from every other arena player's visible set so they never see each other. */
    public static final class HideSpectators extends EntityTickingSystem<EntityStore> {

        private final Query<EntityStore> query = Query.and(EntityTrackerSystems.EntityViewer.getComponentType(),
                PlayerRef.getComponentType());
        private final Set<Dependency<EntityStore>> dependencies = Collections.singleton(
                new SystemDependency<>(Order.AFTER, EntityTrackerSystems.CollectVisible.class));

        @Nullable
        @Override
        public SystemGroup<EntityStore> getGroup() {
            return EntityTrackerSystems.FIND_VISIBLE_ENTITIES_GROUP;
        }

        @Nonnull
        @Override
        public Set<Dependency<EntityStore>> getDependencies() {
            return dependencies;
        }

        @Nonnull
        @Override
        public Query<EntityStore> getQuery() {
            return query;
        }

        @Override
        public boolean isParallel(int archetypeChunkSize, int taskCount) {
            return EntityTickingSystem.maybeUseParallel(archetypeChunkSize, taskCount);
        }

        @Override
        public void tick(float dt, int systemIndex, @Nonnull Store<EntityStore> store) {
            if (store.getEntityCountFor(SpectatorComponent.getComponentType()) == 0) {
                return;
            }
            super.tick(dt, systemIndex, store);
        }

        @Override
        public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
                @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            var viewer = chunk.getComponent(index, EntityTrackerSystems.EntityViewer.getComponentType());
            if (viewer == null) {
                return;
            }
            // A spectator still sees their own entity: updates to it, such as their controls, reach them only through it.
            var self = chunk.getReferenceTo(index);
            for (var iterator = viewer.visible.iterator(); iterator.hasNext();) {
                var ref = iterator.next();
                if (!ref.isValid()) {
                    iterator.remove();
                    continue;
                }
                if (ref.equals(self)) {
                    continue;
                }
                if (commandBuffer.getArchetype(ref).contains(SpectatorComponent.getComponentType())) {
                    viewer.hiddenCount++;
                    iterator.remove();
                }
            }
        }
    }
}
