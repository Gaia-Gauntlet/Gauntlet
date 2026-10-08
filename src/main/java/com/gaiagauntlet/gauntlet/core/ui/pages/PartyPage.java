package com.gaiagauntlet.gauntlet.core.ui.pages;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.events.GauntletEventRegistry;
import com.gaiagauntlet.gauntlet.core.events.events.PlayerPartyEvent;
import com.gaiagauntlet.gauntlet.core.party.components.PartyComponent;
import com.gaiagauntlet.gauntlet.core.party.components.PartyInvitesComponent;
import com.gaiagauntlet.gauntlet.core.party.utils.PartyUtils;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.gaiagauntlet.gauntlet.core.ui.events.AdminPageEvent;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

/**
 * The viewer's party: its members and who leads it, inviting players, the invites waiting for the
 * viewer, and leaving. Every action calls the same party code as the party commands, which also
 * tell the players involved in chat.
 */
public final class PartyPage extends GauntletPage {

    public static final String ID = "Party";

    private static final String PAGE = "Gauntlet/Party/PartyPage.ui";
    private static final String MEMBER = "Gauntlet/Party/PartyMemberRow.ui";
    private static final String INVITE = "Gauntlet/Party/PartyInviteRow.ui";

    /** What the member rows were built for: the members in order, then whether the viewer leads. */
    @Nullable private List<String> builtMembers;
    /** The party ids the invite rows were built for. */
    @Nullable private List<String> builtInvites;

    public PartyPage(@Nonnull PlayerRef playerRef, @Nullable GameSession session) {
        super(playerRef, 2000);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        cmd.append(PAGE);
        Widgets.bind(evt, "#CloseButton", "page.close");
        Widgets.bindValues(evt, "#InviteSend", "party.invite", "", Map.of("@Text", "#InviteName.Value"));
        Widgets.bind(evt, "#Leave", "party.leave");
        builtMembers = null;
        builtInvites = null;
    }

    @Override
    protected void render(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        var party = PartyUtils.getParty(playerRef).orElse(null);
        var leader = party == null || party.getOwner().equals(playerRef.getUuid());
        var members = members(party);

        if (party == null) {
            cmd.set("#PartyName.Text", "No party yet");
            cmd.set("#PartyInfo.Text", "Invite someone to start one.");
        } else {
            var session = PartyUtils.sessionFor(party).orElse(null);
            cmd.set("#PartyName.Text", party.getLabel());
            cmd.set("#PartyInfo.Text", (leader ? "You lead this party" : "Led by " + nameOf(party.getOwner()))
                    + ", " + (session == null ? "not in a session" : "in " + session.getId()));
        }
        cmd.set("#InviteCard.Visible", leader);
        cmd.set("#Leave.Disabled", party == null || party.size() <= 1);

        var key = new ArrayList<String>();
        members.forEach(member -> key.add(member.toString()));
        key.add(Boolean.toString(leader));
        if (!key.equals(builtMembers)) {
            builtMembers = key;
            buildMembers(cmd, evt, members, leader);
        }
        for (int i = 0; i < members.size(); i++) {
            var member = members.get(i);
            var row = "#Members[" + i + "]";
            var online = PlayerUtils.isOnline(member);
            var role = party != null && member.equals(party.getOwner()) ? "Leader" : "Member";
            cmd.set(row + " #Role.Text", role + (online ? ", online" : ", offline"));
            cmd.set(row + " #Promote.Visible", leader && online && !member.equals(playerRef.getUuid()));
        }

        renderInvites(cmd, evt);
    }

    private void buildMembers(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt, @Nonnull List<UUID> members, boolean leader) {
        cmd.clear("#Members");
        for (int i = 0; i < members.size(); i++) {
            var member = members.get(i);
            var row = "#Members[" + i + "]";
            cmd.append("#Members", MEMBER);
            cmd.set(row + " #Portrait.PlayerUuid", member.toString());
            cmd.set(row + " #Name.Text", member.equals(playerRef.getUuid()) ? nameOf(member) + " (you)" : nameOf(member));
            if (leader) {
                Widgets.bindArg(evt, row + " #Promote", "party.promote", member.toString());
            }
        }
    }

    private void renderInvites(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        var invites = invites();
        var ids = invites.stream().map(PartyInvitesComponent.Invite::getPartyId).toList();
        if (ids.equals(builtInvites)) return;
        builtInvites = ids;

        cmd.clear("#Invites");
        if (invites.isEmpty()) {
            cmd.append("#Invites", Widgets.ROW);
            cmd.set("#Invites[0].Text", "No invites right now.");
            return;
        }
        for (int i = 0; i < invites.size(); i++) {
            var invite = invites.get(i);
            var row = "#Invites[" + i + "]";
            var party = PartyUtils.getParty(invite.getPartyId()).orElse(null);
            var from = invite.getSender() == null ? "Someone" : nameOf(invite.getSender());
            cmd.append("#Invites", INVITE);
            cmd.set(row + " #Text.Text", from + " invited you to " + (party == null ? "a party" : party.getLabel()));
            Widgets.bindArg(evt, row + " #Accept", "party.accept", invite.getPartyId());
            Widgets.bindArg(evt, row + " #Decline", "party.decline", invite.getPartyId());
        }
    }

    @Nullable
    @Override
    protected Message handle(@Nonnull AdminPageEvent event) {
        return switch (event.action()) {
            case "party.invite" -> invite(event.text());
            case "party.accept", "party.decline" -> {
                var party = PartyUtils.getParty(event.arg()).orElse(null);
                if (party == null) yield Widgets.fail("That party no longer exists");
                if (event.action().equals("party.accept")) {
                    PartyUtils.acceptInvite(party, playerRef);
                    yield Widgets.ok("Joining " + party.getLabel());
                }
                PartyUtils.declineInvite(party, playerRef);
                yield Widgets.ok("Declined the invite to " + party.getLabel());
            }
            case "party.promote" -> {
                var party = PartyUtils.getParty(playerRef).orElse(null);
                if (party == null || !party.getOwner().equals(playerRef.getUuid())) yield Widgets.fail("Only the party leader can hand over the lead");
                var target = Universe.get().getPlayer(UUID.fromString(event.arg()));
                if (target == null || !party.includesPlayer(target)) yield Widgets.fail("That player is no longer here");
                PartyUtils.promote(target);
                yield Widgets.ok(target.getUsername() + " now leads the party");
            }
            case "party.leave" -> {
                GauntletEventRegistry.dispatch(PlayerPartyEvent.Leave(playerRef)
                        .onComplete(log -> pushStatus(log.toMessage())));
                yield Widgets.ok("Leaving the party...");
            }
            default -> null;
        };
    }

    @Nonnull
    private Message invite(@Nonnull String name) {
        if (name.isEmpty()) return Widgets.fail("Type the name of the player to invite");
        var target = Universe.get().getPlayerByUsername(name, NameMatching.EXACT_IGNORE_CASE);
        if (target == null) return Widgets.fail("Nobody named " + name + " is online");
        if (target.getUuid().equals(playerRef.getUuid())) return Widgets.fail("You can't invite yourself");
        var party = PartyUtils.getParty(playerRef).orElse(null);
        if (party != null && !party.getOwner().equals(playerRef.getUuid())) return Widgets.fail("Only the party leader can invite players");
        if (party != null && party.includesPlayer(target)) return Widgets.fail(target.getUsername() + " is already in your party");
        PartyUtils.sendPartyInvite(playerRef, target);
        return Widgets.ok("Invite sent to " + target.getUsername());
    }

    /** Everyone in the party with the leader first, or just the viewer without one. */
    @Nonnull
    private List<UUID> members(@Nullable PartyComponent party) {
        if (party == null) return List.of(playerRef.getUuid());
        var members = new ArrayList<>(party.getAllPlayers());
        members.sort(Comparator.comparing((UUID member) -> !member.equals(party.getOwner()))
                .thenComparing(this::nameOf, String.CASE_INSENSITIVE_ORDER));
        return members;
    }

    /** The viewer's live invites. Invites restored from a save have no expiry and can't be accepted. */
    @Nonnull
    private List<PartyInvitesComponent.Invite> invites() {
        var ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return List.of();
        var component = ref.getStore().getComponent(ref, PartyInvitesComponent.getComponentType());
        if (component == null) return List.of();
        return component.getInvites().stream()
                .filter(invite -> invite.getExpiryFuture() != null)
                .sorted(Comparator.comparing(PartyInvitesComponent.Invite::getPartyId))
                .toList();
    }

    @Nonnull
    private String nameOf(@Nonnull UUID player) {
        var online = PlayerUtils.resolveOnline(player);
        if (online != null) return online;
        return Objects.requireNonNullElse(PlayerUtils.idsToPlayer.get(player), "Unknown player");
    }
}
