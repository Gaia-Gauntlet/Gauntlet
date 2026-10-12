package com.gaiagauntlet.gauntlet.plugins.auto.events;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.events.events.PlayerGameEvent;
import com.gaiagauntlet.gauntlet.core.events.events.PlayerPartyEvent;
import com.gaiagauntlet.gauntlet.core.events.events.SessionStateEvent;
import com.gaiagauntlet.gauntlet.core.session.constants.SessionState;
import com.gaiagauntlet.gauntlet.plugins.auto.components.AutoComponent;
import com.gaiagauntlet.gauntlet.plugins.auto.utils.AutoUtils;
import com.gaiagauntlet.gauntlet.plugins.timer.events.TimerCompleteEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.world.events.AllWorldsLoadedEvent;

/** Every trigger only schedules Auto on a later hub task, so none of them acts inside the code that fired it */
public class AutoSessionEvents {

    public static void onSessionState(SessionStateEvent evt) {
        GauntletUtils.sessionFor(evt.getSessionId())
                .flatMap(session -> session.get(AutoComponent.getComponentType()))
                .ifPresent(auto -> {
                    if (evt.getTo() == SessionState.ERROR)
                        auto.setFailures(auto.getFailures() + 1);
                    else if (evt.getTo() == SessionState.RUNNING)
                        auto.setFailures(0);
                });
        AutoUtils.schedule(evt.getSessionId());
    }

    public static void onPlayerGame(PlayerGameEvent evt) {
        AutoUtils.schedule(evt.getSessionId());
    }

    public static void onPlayerParty(PlayerPartyEvent evt) {
        AutoUtils.scheduleAll();
    }

    public static void onDisconnect(PlayerDisconnectEvent evt) {
        AutoUtils.scheduleAll();
    }

    public static void onWorldsLoaded(AllWorldsLoadedEvent evt) {
        AutoUtils.scheduleAll();
    }

    public static void onTimerComplete(TimerCompleteEvent evt) {
        GauntletUtils.withHubWorld().execute(() -> AutoUtils.fire(evt.getSessionId(), evt.getTimerId()));
    }
}
