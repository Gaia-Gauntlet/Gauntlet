package com.gaiagauntlet.gauntlet.plugins.spectator.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class SpectatorCommand extends AbstractTargetPlayerCommand {
    public SpectatorCommand() {
        super("spectator", "Put the target player into Gauntlet Spectator mode");
    }

    @Override
    protected void execute(
        @NonNull CommandContext context,
        @Nullable Ref<EntityStore> storeRef,
        @NonNull Ref<EntityStore> targetRef,
        @NonNull PlayerRef playerRef,
        @NonNull World targetWorld,
        @NonNull Store<EntityStore> targetStore
    ) {

    }
}
