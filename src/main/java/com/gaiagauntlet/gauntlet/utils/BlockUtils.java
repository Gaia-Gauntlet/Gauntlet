package com.gaiagauntlet.gauntlet.utils;


import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import org.joml.Vector3i;

public class BlockUtils {

    private BlockUtils() {

    }

    /** Gets the block ID for the given position in a world */
    public static int getBlockId(World world, int x, int y, int z) {
        final var chunkStore = world.getChunkStore();
        final var sectionReference = chunkStore.getChunkSectionReferenceAtBlock(x, y, z);
        if (sectionReference == null || !sectionReference.isValid()) return BlockType.EMPTY_ID;

        final var section = chunkStore.getStore().getComponent(sectionReference, BlockSection.getComponentType());
        if (section == null) return BlockType.EMPTY_ID;

        return section.get(x, y, z);
    }

    public static int getBlockId(World world, Vector3i pos) {
        return getBlockId(world, pos.x, pos.y, pos.z);
    }

    public static BlockSection getBlockSection(World world, int x, int y, int z) {
        // TODO: Chunk loading

        var chunkStore = world.getChunkStore();
        var sectionRef = chunkStore.getChunkSectionReferenceAtBlock(x, y, z);
        if (sectionRef == null || !sectionRef.isValid()) return null; // Continues if section is not loaded.
        var blockSection = chunkStore.getStore().getComponent(sectionRef, BlockSection.getComponentType());
        if (blockSection == null) return null;
        return blockSection;
    }

    public static BlockSection getBlockSection(World world, Vector3i pos) {
        return getBlockSection(world, pos.x, pos.y, pos.z);
    }
}
