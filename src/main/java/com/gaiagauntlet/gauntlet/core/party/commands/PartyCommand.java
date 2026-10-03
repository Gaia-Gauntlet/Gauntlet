package com.gaiagauntlet.gauntlet.core.party.commands;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.party.utils.PartyUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.SingleArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.suggestion.SuggestionResult;
import com.hypixel.hytale.server.core.command.system.suggestion.SuggestionUtil;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Objects;

import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.msg;

public class PartyCommand extends AbstractPlayerCommand {
    private static final SingleArgumentType<String> PARTY_ID = new SingleArgumentType<>(
        "server.commands.parsing.argtype.string.name", "server.commands.parsing.argtype.string.usage") {
        @Override
        public String parse(String input, ParseResult parseResult) {
            return input;
        }

        @Override
        public void suggest(@Nonnull CommandSender sender, @Nonnull String textAlreadyEntered,
                            int numParametersTyped, @Nonnull SuggestionResult result) {
            SuggestionUtil.suggestFiltered(GauntletUtils.withResource().getParties().keySet(), textAlreadyEntered,
                result);
        }

        @Override
        public int getSuggestionValueCount() {
            return 1;
        }
    };

    public PartyCommand() {
        super("party", "Commands related to player parties.");
        addAliases("p");
        requireNoPermission();
        addSubCommand(new AddCommand());
        addSubCommand(new AcceptCommand());
        addSubCommand(new DeclineCommand());
        addSubCommand(new LeaveCommand());
    }

    @Override
    protected void execute(@NonNull CommandContext context, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {
        var party = PartyUtils.getPartyForPlayer(playerRef);
        context.sendMessage(msg("server.gg.commands.party.header").param("name", party.getId()));
        var players = party.getAllOnlinePlayers();
        for (int i = 0; i < players.size(); i++) {
            var player = players.get(i);
            if (party.getOwner().equals(player.getUuid())) {
                context.sendMessage(msg("server.gg.commands.party.owner")
                    .param("i", (i+1))
                    .param("name", player.getUsername())
                );
            } else {
                context.sendMessage(msg("server.gg.commands.party.member")
                    .param("i", (i+1))
                    .param("name", player.getUsername())
                );
            }
        }
    }

    public static class AddCommand extends AbstractPlayerCommand {

        private final RequiredArg<PlayerRef> recipientArg = withRequiredArg(
            "player", "Player to invite to your party", ArgTypes.PLAYER_REF
        );

        public AddCommand() {
            super("invite", "Invite a player to your party");

            addAliases("add");
        }

        @Override
        protected void execute(@NonNull CommandContext context, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {
            PlayerRef recipient = recipientArg.get(context);
            PartyUtils.sendPartyInvite(playerRef, recipient);
        }
    }

    public static class AcceptCommand extends AbstractPlayerCommand {

        private final RequiredArg<String> partyArg = withRequiredArg(
            "party", "Party you'd like to join", PARTY_ID
        );

        public AcceptCommand() {
            super("accept", "Accept an invite to join a party");
        }

        @Override
        protected void execute(@NonNull CommandContext context, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {
            var partyId = partyArg.get(context);
            var party = PartyUtils.getParty(partyId.substring(1, partyId.length()-1)); // Remove speech marks
            if (Objects.isNull(party)) {
                context.sendMessage(msg("server.gg.commands.party.invalidname").param("party", partyId));
                return;
            }
            PartyUtils.acceptInvite(party, playerRef);
        }
    }

    public static class DeclineCommand extends AbstractPlayerCommand {

        private final RequiredArg<String> partyArg = withRequiredArg(
            "party", "Party you'd like to join", PARTY_ID
        );

        public DeclineCommand() {
            super("decline", "Decline an invite to join a party");
        }

        @Override
        protected void execute(@NonNull CommandContext context, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {
            var partyId = partyArg.get(context);
            var party = PartyUtils.getParty(partyId.substring(1, partyId.length()-1)); // Remove speech marks
            if (Objects.isNull(party)) {
                context.sendMessage(msg("server.gg.commands.party.invalidname").param("party", partyId));
                return;
            }
            PartyUtils.declineInvite(party, playerRef);
        }
    }

    public static class LeaveCommand extends AbstractPlayerCommand {
        public LeaveCommand() {
            super("leave", "Leave the party you're currently in.");
        }

        @Override
        protected void execute(@NonNull CommandContext context, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {
            PartyUtils.leaveParty(playerRef);
        }
    }

    // TODO: Decline invite command
    // TODO: Leave command
    // TODO: Transfer owner command
}
