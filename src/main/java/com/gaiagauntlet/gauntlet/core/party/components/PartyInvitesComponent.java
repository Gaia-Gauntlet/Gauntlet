package com.gaiagauntlet.gauntlet.core.party.components;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;


public class PartyInvitesComponent implements Component<EntityStore> {
    public static final BuilderCodec<PartyInvitesComponent> CODEC = BuilderCodec
        .builder(PartyInvitesComponent.class, PartyInvitesComponent::new)
        .build();

    @Override
    public @Nullable Component<EntityStore> clone() {
        return new PartyInvitesComponent();
    }
}
