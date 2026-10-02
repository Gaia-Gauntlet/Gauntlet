package com.gaiagauntlet.gauntlet.core.party.components;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.concurrent.TimeUnit;


public class PartyInviteComponent {
    public static final long EXPIRY_SECONDS = 180;

    PlayerRef recipient;
    PlayerRef sender;
    boolean expired = false; // TODO: Find a way to handle stale invites after server shutdown

    public PartyInviteComponent(PlayerRef recipient, PlayerRef sender) {
        this.recipient = recipient;
        this.sender = sender;

        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            expired = true;
        }, EXPIRY_SECONDS, TimeUnit.SECONDS);
    }
}
