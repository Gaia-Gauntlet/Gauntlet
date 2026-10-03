package com.gaiagauntlet.gauntlet.core.party.commands;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.party.components.PartyComponent;
import com.gaiagauntlet.gauntlet.core.party.utils.PartyUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.msg;

public class PartyCommand extends AbstractPlayerCommand {
    public PartyCommand() {
        super("party", "Commands related to player parties.");
        addAliases("p");
        requireNoPermission();
        addSubCommand(new AddCommand());
    }

    @Override
    protected void execute(@NonNull CommandContext context, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {
        var party = PartyUtils.getPartyForPlayer(playerRef);
        context.sendMessage(msg("server.gg.commands.party.header").param("name", party.getId()));
        var players = party.getAllOnlinePlayers();
        for (int i = 0; i < players.size(); i++) {
            var player = players.get(i);
            context.sendMessage(msg("server.gg.commands.party.member")
                .param("i", (i+1))
                .param("name", player.getUsername())
            );
        }
    }

    public static class AddCommand extends AbstractPlayerCommand {

        private final RequiredArg<PlayerRef> recipientArg = withRequiredArg(
            "player", "Player to invite to your party", ArgTypes.PLAYER_REF
        );

        public AddCommand() {
            super("add", "Invite a player to your party");

            addAliases("invite");
        }

        @Override
        protected void execute(@NonNull CommandContext context, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {
            PlayerRef recipient = recipientArg.get(context);

            PartyUtils.sendPartyInvite(playerRef, recipient);
        }
    }

    // TODO: Decline invite command
    // TODO: Accept invite command
    // TODO: Leave command
    // TODO: Base command to show team and people in it
}
