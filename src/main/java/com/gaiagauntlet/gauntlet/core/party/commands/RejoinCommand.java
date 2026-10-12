package com.gaiagauntlet.gauntlet.core.party.commands;

import com.gaiagauntlet.gauntlet.core.orchestrator.handlers.PlayerHandlers;
import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.msg;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.Nullable;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.hypixel.hytale.builtin.adventure.reputation.command.ReputationAddCommand;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * All of these are, currently, debug and a stop-gap until the full eventing
 * pipeline is finished
 * 
 * 
 * Once the eventing pipeline is done, the orchestrator will be managing all of
 * this stuff and these commands will just
 * emit intents
 */
public class RejoinCommand extends AbstractTargetPlayerCommand {

    /**
     * Default Constructor for a {@link ReputationAddCommand} instance.
     */
    public RejoinCommand() {
        super("rejoin", "server.commands.rejoin.desc");
        requireNoPermission();
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nullable final Ref<EntityStore> sourceRef,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world,
            @Nonnull Store<EntityStore> store) {

        GauntletUtils.run(GauntletUtils.withHubWorld(), () -> {
            if (!PlayerHandlers.rejoin(playerRef, ctx::sendMessage))
                playerRef.sendMessage(msg("server.gg.commands.party.notoffline"));
        });
    }
}
