package com.gaiagauntlet.gauntlet.core.party.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ScheduledFuture;


@NoArgsConstructor
public class PartyInvitesComponent implements Component<EntityStore> {
    @Getter @Setter private static ComponentType<EntityStore, PartyInvitesComponent> componentType;
    public static final String ID = "PartyInvitesComponent";

    public static final BuilderCodec<PartyInvitesComponent> CODEC = BuilderCodec
        .builder(PartyInvitesComponent.class, PartyInvitesComponent::new)
        .append(new KeyedCodec<>("Invites",
            new MapCodec<>(Invite.CODEC, HashMap::new, false)),
            (c, v) -> c.invites = v,
            c -> c.invites
        )
        .add()
        .build();

    /** Map from partyId to a party invite */
    private Map<String, Invite> invites = new HashMap<>();

    public Invite getInvite(String partyId) {
        return invites.get(partyId);
    }

    public Invite removeInvite(String partyId) {
        return invites.remove(partyId);
    }

    public void putInvite(String partyId, UUID sender, ScheduledFuture<?> expiry) {
        invites.put(
            partyId,
            new Invite(partyId, sender, expiry)
        );
    }

    @Override
    public @Nullable Component<EntityStore> clone() {
        return new PartyInvitesComponent();
    }

    public static class Invite {
        public static final BuilderCodec<Invite> CODEC = BuilderCodec
            .builder(Invite.class, Invite::new)
            .append(new KeyedCodec<>("Party", Codec.STRING),
                (c, v) -> c.partyId = v,
                c -> c.partyId
            )
            .add()
            .append(new KeyedCodec<>("Sender", Codec.UUID_STRING),
                (c, v) -> c.sender = v,
                c -> c.sender
            )
            .add()
            .build();

        @Getter String partyId;
        @Getter UUID sender;
        // Don't keep this in the codec so that invites go stale (null) after server restart
        @Nullable @Getter private ScheduledFuture<?> expiryFuture;

        private Invite() {}

        public Invite(String partyId, UUID sender, ScheduledFuture<?> expiryFuture) {
            this.partyId = partyId;
            this.expiryFuture = expiryFuture;
        }

        public void cancel() {
            if (Objects.nonNull(expiryFuture)) expiryFuture.cancel(false);
        }
    }
}
