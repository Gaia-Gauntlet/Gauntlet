package com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.cornucopia;

import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;
import org.joml.Vector3i;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.BlockEntity;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.hitboxcollision.HitboxCollision;
import com.hypixel.hytale.server.core.modules.entity.hitboxcollision.HitboxCollisionConfig;
import com.hypixel.hytale.server.core.universe.world.SetBlockSettings;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;

public final class ArenaBlocks {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String COLLISION_CONFIG = "RotatedCollision";

    private ArenaBlocks() {
    }

    @Nonnull
    public static Vector3i blockAt(@Nonnull Vector3d position) {
        return new Vector3i(MathUtil.floor(position.x), MathUtil.floor(position.y), MathUtil.floor(position.z));
    }

    @Nullable
    public static Ref<EntityStore> blockToEntity(@Nonnull World world, @Nonnull Vector3i position,
            @Nonnull Consumer<Holder<EntityStore>> decorate) {
        var chunkStore = world.getChunkStore();
        var sectionRef = chunkStore.getChunkSectionReferenceAtBlock(position.x, position.y, position.z);
        if (sectionRef == null || !sectionRef.isValid()) {
            return null;
        }
        var blockSection = chunkStore.getStore().getComponent(sectionRef, BlockSection.getComponentType());
        var chunkSection = chunkStore.getStore().getComponent(sectionRef, ChunkSection.getComponentType());
        if (blockSection == null || chunkSection == null
                || blockSection.getFiller(position.x, position.y, position.z) != FillerBlockUtil.NO_FILLER) {
            return null;
        }

        var blockType = BlockType.getAssetMap().getAsset(blockSection.get(position.x, position.y, position.z));
        if (blockType == null || blockType.getMaterial() == BlockMaterial.Empty) {
            return null;
        }
        var rotationIndex = blockSection.getRotationIndex(position.x, position.y, position.z);

        var cleared = BlockOperations.setBlock(chunkStore, sectionRef, position.x, position.y, position.z,
                BlockType.EMPTY_ID, BlockType.EMPTY, RotationTuple.NONE_INDEX, FillerBlockUtil.NO_FILLER,
                SetBlockSettings.NONE);
        if (!cleared) {
            return null;
        }
        BlockOperations.updateBlockArea(chunkStore, chunkSection, blockSection, blockType, rotationIndex,
                position.x, position.y, position.z);

        var holder = createBlockEntity(blockType, position, RotationTuple.get(rotationIndex));
        decorate.accept(holder);
        return world.getEntityStore().getStore().addEntity(holder, AddReason.SPAWN);
    }

    public static void entityToBlock(@Nonnull World world, @Nonnull Ref<EntityStore> ref) {
        if (!ref.isValid()) {
            return;
        }
        var store = ref.getStore();
        var blockEntity = store.getComponent(ref, BlockEntity.getComponentType());
        var transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (blockEntity == null || transform == null) {
            return;
        }
        var position = blockAt(transform.getPosition());
        var rotation = toBlockRotation(transform.getRotation());
        if (!placeBlock(world, position, blockEntity.getBlockTypeKey(), rotation.index())) {
            LOGGER.atWarning().log("Could not place %s at %s; the cell is not loaded or is occupied",
                    blockEntity.getBlockTypeKey(), position);
        }
        store.removeEntity(ref, RemoveReason.REMOVE);
    }

    public static boolean placeBlock(@Nonnull World world, @Nonnull Vector3i position, @Nonnull String blockTypeKey,
            int rotationIndex) {
        var assetMap = BlockType.getAssetMap();
        var blockType = assetMap.getAsset(blockTypeKey);
        if (blockType == null) {
            return false;
        }
        var chunkStore = world.getChunkStore();
        var sectionRef = chunkStore.getChunkSectionReferenceAtBlock(position.x, position.y, position.z);
        if (sectionRef == null || !sectionRef.isValid()) {
            return false;
        }
        var blockSection = chunkStore.getStore().getComponent(sectionRef, BlockSection.getComponentType());
        if (blockSection == null || !BlockOperations.testPlaceBlock(chunkStore.getStore(), blockSection,
                position.x, position.y, position.z, blockType, rotationIndex)) {
            return false;
        }
        return BlockOperations.setBlock(chunkStore, sectionRef, position.x, position.y, position.z,
                assetMap.getIndex(blockTypeKey), blockType, rotationIndex, FillerBlockUtil.NO_FILLER,
                SetBlockSettings.PERFORM_BLOCK_UPDATE);
    }

    @Nonnull
    private static Holder<EntityStore> createBlockEntity(@Nonnull BlockType blockType, @Nonnull Vector3i position,
            @Nonnull RotationTuple rotation) {
        var holder = EntityStore.REGISTRY.newHolder();
        var center = new Vector3d(position.x + 0.5, position.y + 0.5, position.z + 0.5);
        holder.addComponent(BlockEntity.getComponentType(), new BlockEntity(blockType.getId()));
        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(center, toEntityRotation(rotation)));
        holder.ensureComponent(UUIDComponent.getComponentType());

        var collision = HitboxCollisionConfig.getAssetMap().getAsset(COLLISION_CONFIG);
        if (collision != null) {
            holder.addComponent(HitboxCollision.getComponentType(), new HitboxCollision(collision));
        } else {
            LOGGER.atWarning().log("Hitbox collision config %s is missing; players will fall through %s",
                    COLLISION_CONFIG, blockType.getId());
        }
        return holder;
    }

    @Nonnull
    private static Rotation3f toEntityRotation(@Nonnull RotationTuple rotation) {
        return new Rotation3f(
                (float) rotation.pitch().getRadians(),
                (float) rotation.yaw().getRadians() + (float) Math.PI,
                (float) rotation.roll().getRadians());
    }

    @Nonnull
    private static RotationTuple toBlockRotation(@Nonnull Rotation3f rotation) {
        return RotationTuple.of(
                Rotation.closestOfDegrees((float) Math.toDegrees(rotation.yaw() - (float) Math.PI)),
                Rotation.closestOfDegrees((float) Math.toDegrees(rotation.pitch())),
                Rotation.closestOfDegrees((float) Math.toDegrees(rotation.roll())));
    }
}
