package com.gaiagauntlet.gauntlet.plugins.chat.components;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.party.components.PartyComponent;
import com.gaiagauntlet.gauntlet.core.party.utils.PartyUtils;
import com.gaiagauntlet.gauntlet.plugins.chat.config.ChatConfig;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.teams.utils.TeamUtils;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
// Each of these should probably be it's own interface. But like. behhhhhhhhhhhhhhhhhh thats 4 files for smth we could do in one
public sealed interface ChatChannel {
    ChatChannel GLOBAL = new Global();
    ChatChannel PARTY = new Party();
    ChatChannel TEAM = new Team();

    @Nonnull
    String id();

    @Nonnull
    Color color(@Nonnull ChatConfig config);

    @Nonnull
    default Color textColor(@Nonnull ChatConfig config) {
        return config.getMessageColor();
    }

    @Nullable
    List<PlayerRef> recipients(@Nonnull PlayerRef sender);

    record Global() implements ChatChannel {
        @Override
        public String id() {
            return "global";
        }

        @Override
        public Color color(ChatConfig config) {
            return config.getMessageColor();
        }

        @Override
        public List<PlayerRef> recipients(PlayerRef sender) {
            return new ArrayList<>(Universe.get().getPlayers());
        }
    }

    record Party() implements ChatChannel {
        @Override
        public String id() {
            return "party";
        }

        @Override
        public Color color(ChatConfig config) {
            return config.getPartyColor();
        }

        @Override
        public List<PlayerRef> recipients(PlayerRef sender) {
            return PartyUtils.getParty(sender).map(PartyComponent::getAllOnlinePlayers).orElse(null);
        }
    }

    record Team() implements ChatChannel {
        @Override
        public String id() {
            return "team";
        }

        @Override
        public Color color(ChatConfig config) {
            return config.getTeamColor();
        }

        @Override
        public List<PlayerRef> recipients(PlayerRef sender) {
            var uuid = sender.getUuid();
            return GameStore.withStore(sender)
                    .flatMap(game -> TeamUtils.teamOf(game, uuid))
                    .or(() -> TeamUtils.teamOf(GauntletUtils.sessionFor(sender).orElse(null), uuid))
                    .<List<PlayerRef>>map(team -> new ArrayList<>(TeamUtils.getOnlinePlayers(team)))
                    .orElse(null);
        }
    }

    record Whisper(@Nonnull UUID target) implements ChatChannel {
        @Override
        public String id() {
            return "whisper";
        }

        @Override
        public Color color(ChatConfig config) {
            return config.getWhisperColor();
        }

        @Override
        public Color textColor(ChatConfig config) {
            return config.getWhisperColor();
        }

        @Override
        public List<PlayerRef> recipients(PlayerRef sender) {
            var recipient = Universe.get().getPlayer(target);
            if (recipient == null)
                return null;
            return new ArrayList<>(List.of(recipient, sender));
        }
    }
}
