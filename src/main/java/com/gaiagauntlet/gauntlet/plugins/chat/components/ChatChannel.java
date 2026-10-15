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
import com.gaiagauntlet.gauntlet.plugins.spectator.components.SpectatorComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.utils.TeamUtils;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import org.jspecify.annotations.NonNull;

// Each of these should probably be it's own interface. But like. behhhhhhhhhhhhhhhhhh thats 4 files for smth we could do in one
public sealed interface ChatChannel {
    ChatChannel GLOBAL = new Global();
    ChatChannel PARTY = new Party();
    ChatChannel TEAM = new Team();
    ChatChannel SESSION = new Session();
    ChatChannel ANNOUNCEMENT = new Announcement();
    ChatChannel SPECTATOR = new Spectator();

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
        public @NonNull String id() {
            return "global";
        }

        @Override
        public @NonNull Color color(@NonNull ChatConfig config) {
            return config.getMessageColor();
        }

        @Override
        public List<PlayerRef> recipients(@NonNull PlayerRef sender) {
            var worldId = sender.getWorldUuid();
            var world = worldId == null ? null : Universe.get().getWorld(worldId);
            return world == null ? null : new ArrayList<>(world.getPlayerRefs());
        }
    }

    record Party()implements ChatChannel {
        @Override
        public @NonNull String id() {
            return "party";
        }

        @Override
        public @NonNull Color color(@NonNull ChatConfig config) {
            return config.getPartyColor();
        }

        @Override
        public List<PlayerRef> recipients(@NonNull PlayerRef sender) {
            return PartyUtils.getParty(sender).map(PartyComponent::getAllOnlinePlayers).orElse(null);
        }
    }

    record Team() implements ChatChannel {
        @Override
        public @NonNull String id() {
            return "team";
        }

        @Override
        public @NonNull Color color(@NonNull ChatConfig config) {
            return config.getTeamColor();
        }

        @Override
        public List<PlayerRef> recipients(@NonNull PlayerRef sender) {
            var uuid = sender.getUuid();
            return GameStore.withStore(sender)
                    .flatMap(game -> TeamUtils.teamOf(game, uuid))
                    .or(() -> TeamUtils.teamOf(GauntletUtils.sessionFor(sender).orElse(null), uuid))
                    .<List<PlayerRef>>map(team -> new ArrayList<>(TeamUtils.getOnlinePlayers(team)))
                    .orElse(null);
        }
    }

    record Session() implements ChatChannel {
        @Override
        public @NonNull String id() {
            return "session";
        }

        @Override
        public @NonNull Color color(@NonNull ChatConfig config) {
            return config.getSessionColor();
        }

        @Override
        public List<PlayerRef> recipients(@NonNull PlayerRef sender) {
            return GauntletUtils.sessionFor(sender)
                    .<List<PlayerRef>>map(session -> new ArrayList<>(GauntletUtils.playersFor(session)))
                    .orElse(null);
        }
    }

    record Announcement() implements ChatChannel {
        public static final String PERMISSION = "gaiagauntlet.chat.announce";

        @Override
        public @NonNull String id() {
            return "announcement";
        }

        @Override
        public @NonNull Color color(@NonNull ChatConfig config) {
            return config.getAnnouncementColor();
        }

        @Override
        public @NonNull Color textColor(@NonNull ChatConfig config) {
            return config.getAnnouncementColor();
        }

        @Override
        public List<PlayerRef> recipients(@NonNull PlayerRef sender) {
            if (!PermissionsModule.get().hasPermission(sender.getUuid(), PERMISSION, false))
                return null;
            return new ArrayList<>(Universe.get().getPlayers());
        }
    }

    record Spectator() implements ChatChannel {
        public static final String BYPASS = "gaiagauntlet.chat.spectator.bypass";

        public static boolean confines(@Nonnull PlayerRef sender) {
            return isSpectating(sender) && !PermissionsModule.get().hasPermission(sender.getUuid(), BYPASS, false);
        }

        private static boolean isSpectating(@Nonnull PlayerRef player) {
            return player.getComponentConcurrent(SpectatorComponent.getComponentType()) != null;
        }

        @Override
        public @NonNull String id() {
            return "spectator";
        }

        @Override
        public @NonNull Color color(@NonNull ChatConfig config) {
            return config.getSpectatorColor();
        }

        @Override
        public List<PlayerRef> recipients(@NonNull PlayerRef sender) {
            var worldId = sender.getWorldUuid();
            var world = worldId == null ? null : Universe.get().getWorld(worldId);
            if (world == null)
                return null;
            var spectators = new ArrayList<PlayerRef>();
            for (var player : world.getPlayerRefs()) {
                if (player.equals(sender) || isSpectating(player))
                    spectators.add(player);
            }
            return spectators;
        }
    }

    record Whisper(@Nonnull UUID target) implements ChatChannel {
        @Override
        public @NonNull String id() {
            return "whisper";
        }

        @Override
        public @NonNull Color color(@NonNull ChatConfig config) {
            return config.getWhisperColor();
        }

        @Override
        public @NonNull Color textColor(@NonNull ChatConfig config) {
            return config.getWhisperColor();
        }

        @Override
        public List<PlayerRef> recipients(@NonNull PlayerRef sender) {
            var recipient = Universe.get().getPlayer(target);
            if (recipient == null)
                return null;
            return new ArrayList<>(List.of(recipient, sender));
        }
    }
}
