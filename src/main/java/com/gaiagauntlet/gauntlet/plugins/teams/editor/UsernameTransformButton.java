package com.gaiagauntlet.gauntlet.plugins.teams.editor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bson.BsonDocument;

import com.gaiagauntlet.gauntlet.plugins.teams.components.TeamComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.assets.TeamListAsset;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.hypixel.hytale.builtin.asseteditor.AssetEditorPlugin;
import com.hypixel.hytale.builtin.asseteditor.AssetPath;
import com.hypixel.hytale.builtin.asseteditor.EditorClient;
import com.hypixel.hytale.builtin.asseteditor.event.AssetEditorActivateButtonEvent;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.protocol.packets.asseteditor.AssetEditorJsonAssetUpdated;
import com.hypixel.hytale.protocol.packets.asseteditor.AssetEditorPopupNotificationType;
import com.hypixel.hytale.protocol.packets.asseteditor.JsonUpdateCommand;
import com.hypixel.hytale.protocol.packets.asseteditor.JsonUpdateType;
import com.hypixel.hytale.server.core.Message;

public final class UsernameTransformButton {

    public static final String BUTTON_ID = "UsernameTransform";
    public static final String BUTTON_TEXT_ID = "server.gg.gauntlet.editor.button";
    public static final String TEAMS_LIST_ID = TeamListAsset.class.getSimpleName();

    private UsernameTransformButton() {
    }


    // all of this grew to be very cursed. I just started copying shit from hytale until it worked - need to clean it up later cuz rn it sucks
    public static void activate(AssetEditorActivateButtonEvent event) {
        var client = event.getEditorClient();
        var plugin = AssetEditorPlugin.get();
        var open = plugin.getOpenAssetPath(client);
        if (open == null || open.path().toString().isEmpty())
            return;

        var asset = TeamListAsset.getAssetMap().get(TeamListAsset.getAssetStore().decodeFilePathKey(open.path()));
        if (asset == null)
            return;

        var lookups = new HashMap<String, CompletableFuture<UUID>>();
        asset.getTeamList().values().forEach(team -> {
            for (var player : team.getRawPlayerNames()) {
                if (!isUuid(player))
                    lookups.computeIfAbsent(player, name -> PlayerUtils.uuidOf(name).exceptionally(error -> null));
            }
        });
        if (lookups.isEmpty())
            return;

        CompletableFuture.allOf(lookups.values().toArray(CompletableFuture[]::new)).thenRun(() -> {
            var unresolved = new TreeSet<String>();
            var commands = new ArrayList<JsonUpdateCommand>();

            asset.getTeamList().forEach((teamId, team) -> {
                var resolved = Arrays.stream(team.getRawPlayerNames()).map(player -> {
                    var lookup = lookups.get(player);
                    if (lookup == null)
                        return player;
                    var uuid = lookup.join();
                    if (uuid == null)
                        unresolved.add(player);
                    return uuid == null ? player : uuid.toString();
                }).toArray(String[]::new);

                if (Arrays.equals(resolved, team.getRawPlayerNames()))
                    return;

                var command = new JsonUpdateCommand();
                command.type = JsonUpdateType.SetProperty;
                command.path = new String[] { "TeamList", teamId,
                        "Players" };
                command.value = new BsonDocument("value",
                        Codec.STRING_ARRAY.encode(resolved, new ExtraInfo())).toJson();
                commands.add(command);
            });

            if (!commands.isEmpty()) {
                var array = commands.toArray(JsonUpdateCommand[]::new);
                plugin.handleJsonAssetUpdate(client, open, TEAMS_LIST_ID, -1, array, 0);
                client.getPacketHandler().write(new AssetEditorJsonAssetUpdated(open.toPacket(), array));
            }

            notify(client,
                    unresolved.isEmpty() ? AssetEditorPopupNotificationType.Success
                            : AssetEditorPopupNotificationType.Warning,
                    unresolved.isEmpty()
                            ? Message.translation("server.gg.gauntlet.editor.usernamesResolved")
                            : Message.translation("server.gg.gauntlet.editor.usernamesUnresolved").param("names",
                                    String.join(", ", unresolved)));
        });
    }

    private static boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static void notify(EditorClient client, AssetEditorPopupNotificationType type, Message message) {
        client.sendPopupNotification(type, message);
    }
}
