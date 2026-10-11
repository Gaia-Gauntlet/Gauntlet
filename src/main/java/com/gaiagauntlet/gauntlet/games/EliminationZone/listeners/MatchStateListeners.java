package com.gaiagauntlet.gauntlet.games.EliminationZone.listeners;

import com.gaiagauntlet.gauntlet.core.GauntletUtils;
import com.gaiagauntlet.gauntlet.core.events.GauntletEventRegistry;
import com.gaiagauntlet.gauntlet.core.events.events.SessionEvent;
import com.gaiagauntlet.gauntlet.core.hud.events.HudEvent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.lobby.EZLobbyManager;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state.EZStates;
import com.gaiagauntlet.gauntlet.games.EliminationZone.ui.WinnerHud;
import com.gaiagauntlet.gauntlet.games.EliminationZone.weather.utils.WeatherUtils;
import com.gaiagauntlet.gauntlet.plugins.announcer.utils.Announcer;
import com.gaiagauntlet.gauntlet.plugins.gamestate.events.MatchStateEvent;
import com.gaiagauntlet.gauntlet.plugins.gamestate.utils.MatchUtils;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.lobbycontroller.LobbyController;
import com.gaiagauntlet.gauntlet.plugins.timer.events.TimerCompleteEvent;
import com.gaiagauntlet.gauntlet.plugins.timer.utils.TimerUtils;
import com.gaiagauntlet.gauntlet.utils.MusicUtils;
import com.gaiagauntlet.gauntlet.utils.WorldUtils;
import com.hypixel.hytale.protocol.packets.interface_.EventTitleStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.Objects;

public final class MatchStateListeners {

    public static final String GAME_END_TIMER_ID = "GameEnd";
    public static final float END_SEQUENCE_LENGTH = 10f; // TODO: Move to config
    public static final String END_SEQUENCE_MUSIC = "Battle_Of_Glory_Victory_Sting";

    private MatchStateListeners() {}

    public static void onSuddenDeath(MatchStateEvent evt) {
        if (!Objects.equals(evt.getTo(), EZStates.SUDDEN_DEATH.name())) return;

        World world = LobbyController.withArenaWorld(evt.getSessionId()).orElse(null);
        if (world == null) return;
        var game = GameStore.withStore(world, evt.getSessionId()).orElse(null);
        if (game == null) return;
        WeatherUtils.doWeather(game, "GG_Sudden_Death", Double.MAX_VALUE);

        MusicUtils.forcePlayMusicToAllPlayers("Battle_Of_Glory_Last_Stand_Combined");
        Announcer.title(
            world,
            Message.raw("SUDDEN DEATH!"),
            Message.raw("Zones close twice as fast!"),
            EventTitleStyle.VoidEviction,
            "SFX_Discovery_Z4_Short"
        );
    }

    public static void onMatchEnd(MatchStateEvent evt) {
        if (!Objects.equals(evt.getTo(), EZStates.ENDED.name())) return;

        new HudEvent(
            evt.getSessionId(), null, WinnerHud.ID, null, true, null
        ).dispatch();

        MusicUtils.forcePlayMusicToAllPlayers(
            END_SEQUENCE_MUSIC,
            GauntletUtils.sessionFor(evt.getSessionId()).orElse(null)
        );

        TimerUtils.start(GAME_END_TIMER_ID, evt.getGame(), END_SEQUENCE_LENGTH);
    }

    public static void onMatchEndTimer(TimerCompleteEvent evt) {
        if (!Objects.equals(evt.getTimerId(), GAME_END_TIMER_ID)) return;
        GauntletEventRegistry.dispatch(
            new SessionEvent(SessionEvent.SessionOperation.CLEAN, evt.getSessionId())
        );
    }
}
