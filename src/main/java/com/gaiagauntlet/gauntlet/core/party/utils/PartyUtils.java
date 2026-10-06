package com.gaiagauntlet.gauntlet.core.party.utils;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.events.GauntletEventRegistry;
import com.gaiagauntlet.gauntlet.core.events.events.PlayerPartyEvent;
import com.gaiagauntlet.gauntlet.core.party.components.PartyComponent;
import com.gaiagauntlet.gauntlet.core.party.components.PartyInvitesComponent;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.awt.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.msg;

public class PartyUtils {
    public static final long EXPIRY_SECONDS = 180;

    private PartyUtils() {
    }

    public static Collection<PartyComponent> getParties() {
        var resource = GauntletUtils.withResource();
        return resource.getParties().values();
    }

    public static Optional<PartyComponent> getParty(String partyId) {
        var resource = GauntletUtils.withResource();
        return resource.getParty(partyId);
    }

    @Nonnull
    public static PartyComponent createParty(String label, UUID owner) {
        var players = new HashSet<UUID>();
        players.add(owner);
        var party = new PartyComponent(UUID.randomUUID().toString(), label, players, owner);
        return party;
    }

    /**
     * Ensures that the player is always in a party, even if it's a singleton.
     * 
     * Auto-reconnects disconnected players
     * 
     * Creates a party for the player if it doesn't exist
     */
    @Nonnull
    public static PartyComponent getParty(PlayerRef player) {
        var resource = GauntletUtils.withResource();
        return resource.getParty(player).orElseGet(() -> {
            // No pre-existing party exists, create a new one with this player as captain.
            var newParty = createParty(PlayerUtils.normalize(player.getUsername()) + "'s Party", player.getUuid());
            resource.addParty(newParty);
            return newParty;
        });

    }

    /**
     * Gets the party for the player without making a new one if they aren't in one
     * already.
     */
    @Nullable
    public static Optional<PartyComponent> getPartyNullable(PlayerRef player) {
        var resource = GauntletUtils.withResource();
        for (PartyComponent party : resource.getParties().values()) {
            if (!party.includesPlayer(player.getUuid()))
                continue;
            return Optional.of(party);
        }
        return Optional.empty();
    }

    public static Optional<GameSession> sessionFor(PartyComponent party) {
        return sessionFor(party.getId());
    }

    public static Optional<GameSession> sessionFor(String partyId) {
        var resource = GauntletUtils.withResource();
        var session = resource.sessionFor(partyId);
        return session;
    }

    // Invites

    public static void sendPartyInvite(PlayerRef sender, PlayerRef recipient) {
        var party = getParty(sender);
        var invites = getInvitesComp(recipient);
        if (sender.getUuid().equals(recipient.getUuid())) {
            sender.sendMessage(msg("You can't invite yourself to a party!").color(Color.RED));
            return;
        } else if (!party.getOwner().equals(sender.getUuid())) {
            sender.sendMessage(msg("server.gg.commands.party.invite.leaderonly"));
            return;
        } else if (hasActiveInvite(party.getId(), recipient)) {
            sender.sendMessage(msg(recipient.getUsername() + " already has an active invite from you."));
            return;
        } else if (party.includesPlayer(recipient.getUuid())) {
            sender.sendMessage(msg(recipient.getUsername() + " is already in this party!"));
            return;
        }
        UUID uuid = sender.getWorldUuid();
        assert uuid != null;
        World world = Universe.get().getWorld(uuid);
        assert world != null;
        var expiry = world.scheduleAfter(
                () -> expireInvite(party.getId(), sender, recipient),
                EXPIRY_SECONDS, TimeUnit.SECONDS);
        invites.putInvite(party.getId(), sender.getUuid(), expiry);
        recipient.sendMessage(msg("server.gg.commands.party.invite.received")
                .param("sender", sender.getUsername())
                .param("party", party.getId())
                .param("expiry", EXPIRY_SECONDS));
        sender.sendMessage(msg("server.gg.commands.party.invite.sent")
                .param("recipient", recipient.getUsername())
                .param("party", party.getId())
                .param("expiry", EXPIRY_SECONDS));
    }

    public static void expireInvite(String partyId, PlayerRef sender, PlayerRef recipient) {
        var invites = getInvitesComp(recipient);
        var invite = invites.removeInvite(partyId);
        if (Objects.isNull(invite))
            return;

        recipient.sendMessage(Message.raw("Your invite from " + sender.getUsername()
                + " has expired."));
        sender.sendMessage(Message.raw("Your invite to " + recipient.getUsername()
                + " has expired."));

        invite.cancel();
    }

    public static void acceptInvite(PartyComponent party, PlayerRef recipient) {
        if (!hasActiveInvite(party.getId(), recipient)) {
            recipient.sendMessage(msg("You don't have an invite from this party!").color(Color.RED));
            return;
        }

        var invites = getInvitesComp(recipient);
        var invite = invites.getInvite(party.getId());
        invite.cancel();
        invites.removeInvite(party.getId());

        // emit the join party event
        GauntletEventRegistry.dispatch(PlayerPartyEvent.Join(recipient, party.getId()));
    }

    public static void declineInvite(PartyComponent party, PlayerRef recipient) {
        var invites = getInvitesComp(recipient);
        var invite = invites.removeInvite(party.getId());
        if (Objects.isNull(invite)) {
            recipient.sendMessage(msg("You don't have an invite to that party to decline!").color(Color.RED));
            return;
        }
        recipient.sendMessage(msg("party.invite.declined.you").param("party", party.getId()));
        PlayerRef sender = PlayerUtils.get(invite.getSender());
        if (Objects.nonNull(sender)) {
            sender.sendMessage(msg("party.invite.declined")
                    .param("recipient", recipient.getUsername())
                    .param("party", party.getId()));
        }
    }

    static boolean hasActiveInvite(String partyId, PlayerRef recipient) {
        var invites = getInvitesComp(recipient);
        var invite = invites.getInvite(partyId);
        if (Objects.isNull(invite))
            return false;
        if (Objects.isNull(invite.getExpiryFuture())) {
            // This is a stale invite, discount it
            return false;
        }
        return true;
    }

    private static PartyInvitesComponent getInvitesComp(PlayerRef recipient) {
        var recipRef = recipient.getReference();
        assert recipRef != null;
        var recipStore = recipRef.getStore();
        return recipStore.ensureAndGetComponent(recipRef, PartyInvitesComponent.getComponentType());
    }

    public static void promote(PlayerRef player) {
        var party = getParty(player);
        if (party == null)
            return;
        var existingId = party.getOwner();
        var existing = PlayerUtils.get(existingId);
        party.setOwner(player.getUuid());
        var members = party.getAllOnlinePlayers();
        for (var member : members) {

            // demoted message
            if (member.equals(existing)) {
                player.sendMessage(msg("party.ownership.demote.self"));
            } else if (existing != null) {
                member.sendMessage(msg("party.ownership.demote").param("player", existing.getUsername()));
            }

            // promoted message
            if (member.equals(player)) {
                player.sendMessage(msg("party.ownership.promote.self"));
            } else {
                member.sendMessage(msg("party.ownership.promote").param("player", player.getUsername()));
            }

        }
    }
}
