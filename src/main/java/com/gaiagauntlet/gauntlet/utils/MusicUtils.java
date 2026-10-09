package com.gaiagauntlet.gauntlet.utils;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.hypixel.hytale.builtin.audio.components.ForcedMusicTracker;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.musiccontainer.config.MusicContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class MusicUtils {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private MusicUtils() {}

    public static void forcePlayMusicContainerToPlayer(String musicContainer, PlayerRef playerRef) {
        int containerIndex = MusicContainer.getAssetMap().getIndex(musicContainer);
        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        world.execute(() -> {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) return;
            Store<EntityStore> store = ref.getStore();
            ForcedMusicTracker tracker = store.getComponent(ref, ForcedMusicTracker.getComponentType());
            if (tracker != null) tracker.setCurrentContainerIndex(containerIndex);
        });
    }

    public static void forcePlayMusicToAllPlayers(String musicContainer, World world) {
        for (PlayerRef playerRef : world.getPlayerRefs()) {
            forcePlayMusicContainerToPlayer(musicContainer, playerRef);
        }
    }

    public static void forcePlayMusicToAllPlayers(String musicContainer, GameSession session) {
        for (PlayerRef playerRef : GauntletUtils.playersFor(session)) {
            forcePlayMusicContainerToPlayer(musicContainer, playerRef);
        }
    }

    public static void forcePlayMusicToAllPlayers(String musicContainer) {
        for (World world : Universe.get().getWorlds().values()) {
            forcePlayMusicToAllPlayers(musicContainer, world);
        }
    }
}
