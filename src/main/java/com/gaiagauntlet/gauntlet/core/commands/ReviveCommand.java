package com.gaiagauntlet.gauntlet.core.commands;

import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.msg;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.annotation.Nonnull;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.events.GauntletEventRegistry;
import com.gaiagauntlet.gauntlet.core.events.events.PlayerReviveEvent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetPlayersCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ReviveCommand extends AbstractTargetPlayersCommand {

    public ReviveCommand() {
        super("revive", "Revive eliminated players in their game");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull World world, @Nonnull Store<EntityStore> store,
            @Nonnull List<Ref<EntityStore>> targets) {
        var sessions = new LinkedHashMap<String, List<PlayerRef>>();
        for (var ref : targets) {
            var player = store.getComponent(ref, PlayerRef.getComponentType());
            if (player == null)
                continue;
            GauntletUtils.sessionFor(player).ifPresent(
                    session -> sessions.computeIfAbsent(session.getId(), _ -> new ArrayList<>()).add(player));
        }
        if (sessions.isEmpty()) {
            context.sendMessage(msg("server.gg.commands.revive.nosession"));
            return;
        }
        for (var entry : sessions.entrySet()) {
            GauntletEventRegistry.dispatch(new PlayerReviveEvent(entry.getKey(), entry.getValue())
                    .onComplete(log -> context.sendMessage(log.toMessage())));
        }
    }
}
