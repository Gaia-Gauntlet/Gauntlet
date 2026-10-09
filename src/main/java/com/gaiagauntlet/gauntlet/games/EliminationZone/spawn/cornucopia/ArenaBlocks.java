package com.gaiagauntlet.gauntlet.games.EliminationZone.spawn.cornucopia;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;
import org.joml.Vector3i;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.BlockEntity;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.PropComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.hitboxcollision.HitboxCollision;
import com.hypixel.hytale.server.core.modules.entity.hitboxcollision.HitboxCollisionConfig;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PreventItemMerging;
import com.hypixel.hytale.server.core.modules.entity.item.PreventPickup;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.SetBlockSettings;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;

/**
 * Turns world blocks into movable block entities and back. Run on the world
 * thread.
 */
public final class ArenaBlocks {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private ArenaBlocks() {
    }

    /**
     * The block a position is in. Negative coordinates round toward negative
     * infinity.
     */
    @Nonnull
    public static Vector3i blockAt(@Nonnull Vector3d position) {
        return new Vector3i((int) Math.floor(position.x), (int) Math.floor(position.y), (int) Math.floor(position.z));
    }

    /**
     * Replaces the block with an entity carrying the extra components. Null when
     * there is no block or no matching item.
     */
    // @Nullable
    // public static <T extends Component<EntityStore>> Ref<EntityStore> blockToEntity(@Nonnull World world,
            // @Nonnull Vector3i position, @Nonnull Store<EntityStore> store,
            // @Nonnull Map<ComponentType<EntityStore, T>, T> extra) {
        // var blockType = world.getBlockType(position);
        // BlockType.getAssetMap().getAsset(blockSection.get(targetBlock.x, targetBlock.y, targetBlock.z));
        // if (blockType == null) {
            // return null;
        // }
        // var itemId = blockType.getId();
        // world.setBlock(position.x, position.y, position.z, "Empty");
        // return spawnBlockEntity(itemId, position, store, new HashMap<>(extra));
    // }

    /**
     * Puts the entity's block back where the entity now is and removes the entity.
     */
    public static void entityToBlock(@Nonnull World world, @Nonnull Ref<EntityStore> ref) {
        var store = ref.getStore();
        var blockEntity = store.getComponent(ref, BlockEntity.getComponentType());
        var transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (blockEntity == null || transform == null || !world.isAlive() || !world.isTicking()) {
            return;
        }
        final var chunkStore = world.getChunkStore();
        var position = blockAt(transform.getPosition());

        final var assetMap = BlockType.getAssetMap();
        final var blockId = assetMap.getIndex(blockEntity.getBlockTypeKey());
        final var blockType = assetMap.getAsset(blockId);

        chunkStore.getChunkSectionReferenceAtBlockAsync(position.x, position.y, position.z)
                .thenApplyAsync(sectionRef -> {
                    BlockOperations.setBlock(chunkStore, sectionRef, position.x, position.y, position.z,
                            blockId, blockType, RotationTuple.NONE_INDEX, FillerBlockUtil.NO_FILLER,
                            SetBlockSettings.NONE);
                    return world;
                }, world);
        store.removeEntity(ref, RemoveReason.REMOVE);
    }

    @Nullable
    private static <T extends Component<EntityStore>> Ref<EntityStore> spawnBlockEntity(@Nonnull String itemId,
            @Nonnull Vector3i position, @Nonnull Store<EntityStore> store,
            @Nonnull Map<ComponentType<EntityStore, T>, T> extra) {
        var item = Item.getAssetMap().getAsset(itemId);
        if (item == null) {
            LOGGER.atWarning().log("No item for block %s; cannot animate it", itemId);
            return null;
        }
        var center = new Vector3d(position.x + 0.5, position.y + 0.5, position.z + 0.5);
        var rotation = new Rotation3f();
        var holder = store.getRegistry().newHolder();
        var stack = new ItemStack(itemId, 1);
        stack.setOverrideDroppedItemAnimation(true);

        var model = itemModel(item);
        if (model != null) {
            holder.addComponent(NetworkId.getComponentType(),
                    new NetworkId(store.getExternalData().takeNextNetworkId()));
            holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
            holder.addComponent(PersistentModel.getComponentType(),
                    new PersistentModel(new Model.ModelReference(itemModelId(item), 1f, null, true)));
            holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(rotation));
            holder.ensureComponent(UUIDComponent.getComponentType());
        } else if (item.hasBlockType()) {
            holder.addComponent(BlockEntity.getComponentType(), new BlockEntity(itemId));
            holder.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(1f));
            holder.ensureComponent(UUIDComponent.getComponentType());
        } else {
            holder.addComponent(NetworkId.getComponentType(),
                    new NetworkId(store.getExternalData().takeNextNetworkId()));
            holder.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(1f));
            holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(rotation));
        }
        var hitbox = HitboxCollisionConfig.getAssetMap().getAsset("HardRotated");
        holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(center, rotation));
        holder.addComponent(ItemComponent.getComponentType(), new ItemComponent(stack));
        holder.addComponent(PreventPickup.getComponentType(), PreventPickup.INSTANCE);
        holder.addComponent(PreventItemMerging.getComponentType(), PreventItemMerging.INSTANCE);
        holder.addComponent(PropComponent.getComponentType(), PropComponent.get());
        if (hitbox != null) {
            holder.addComponent(HitboxCollision.getComponentType(), new HitboxCollision(hitbox));
        }
        for (var entry : extra.entrySet()) {
            holder.addComponent(entry.getKey(), entry.getValue());
        }
        return store.addEntity(holder, AddReason.SPAWN);
    }

    @Nullable
    private static String itemModelId(@Nonnull Item item) {
        var modelId = item.getModel();
        if (modelId == null && item.hasBlockType()) {
            var blockType = BlockType.getAssetMap().getAsset(item.getId());
            if (blockType != null && blockType.getCustomModel() != null) {
                modelId = blockType.getCustomModel();
            }
        }
        return modelId;
    }

    @Nullable
    private static Model itemModel(@Nonnull Item item) {
        var modelId = itemModelId(item);
        if (modelId == null) {
            return null;
        }
        var asset = ModelAsset.getAssetMap().getAsset(modelId);
        return asset != null ? Model.createStaticScaledModel(asset, 1f) : null;
    }
}
