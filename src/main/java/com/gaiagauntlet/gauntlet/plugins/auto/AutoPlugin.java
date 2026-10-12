package com.gaiagauntlet.gauntlet.plugins.auto;

import java.util.List;
import java.util.Map;
import com.gaiagauntlet.gauntlet.core.events.GauntletEventRegistry;
import com.gaiagauntlet.gauntlet.core.events.events.PlayerGameEvent;
import com.gaiagauntlet.gauntlet.core.events.events.PlayerPartyEvent;
import com.gaiagauntlet.gauntlet.core.events.events.SessionEvent;
import com.gaiagauntlet.gauntlet.core.events.events.SessionStateEvent;
import com.gaiagauntlet.gauntlet.core.games.interfaces.SimpleGamePlugin;
import com.gaiagauntlet.gauntlet.plugins.auto.components.AutoComponent;
import com.gaiagauntlet.gauntlet.plugins.auto.components.assets.AutoSession;
import com.gaiagauntlet.gauntlet.plugins.auto.components.assets.SessionPreset;
import com.gaiagauntlet.gauntlet.plugins.auto.components.rotation.PresetRotation;
import com.gaiagauntlet.gauntlet.plugins.auto.components.rotation.SequentialRotation;
import com.gaiagauntlet.gauntlet.plugins.auto.components.rotation.VoteRotation;
import com.gaiagauntlet.gauntlet.plugins.auto.events.AutoSessionEvents;
import com.gaiagauntlet.gauntlet.plugins.auto.tv.AutoVolume;
import com.gaiagauntlet.gauntlet.plugins.timer.TimerPlugin;
import com.gaiagauntlet.gauntlet.plugins.timer.utils.TimerUtils;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.builtin.triggervolumes.TriggerVolumesPlugin;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.world.events.AllWorldsLoadedEvent;
import com.gaiagauntlet.gauntlet.core.games.interfaces.UiGamePlugin;
import com.gaiagauntlet.gauntlet.core.hud.interfaces.HudElement;
import com.gaiagauntlet.gauntlet.core.session.registry.SessionRegistry;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.AdminTab;
import com.gaiagauntlet.gauntlet.core.ui.interfaces.PageFactory;
import com.gaiagauntlet.gauntlet.plugins.auto.components.VoteComponent;
import com.gaiagauntlet.gauntlet.plugins.auto.ui.AutoTab;
import com.gaiagauntlet.gauntlet.plugins.auto.ui.MatchTab;
import com.gaiagauntlet.gauntlet.plugins.auto.ui.VotePage;
import com.gaiagauntlet.gauntlet.plugins.auto.utils.VoteUtils;
import com.gaiagauntlet.gauntlet.plugins.gamestore.GameStorePlugin;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameComponentRegistry;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class AutoPlugin implements SimpleGamePlugin, UiGamePlugin {
    public static final String ID = "AutoPlugin";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Auto";
    }

    @Override
    public List<String> getDependencies() {
        return List.of(GameStorePlugin.ID, TimerPlugin.ID);
    }

    @Override
    public void setup(ComponentAccessor<EntityStore> accessor, String sessionId, String gameId) {

    }

    public void init(JavaPlugin host) {
        PresetRotation.CODEC.register(SequentialRotation.TYPE, SequentialRotation.class, SequentialRotation.CODEC);
        PresetRotation.CODEC.register(VoteRotation.TYPE, VoteRotation.class, VoteRotation.CODEC);

        host.getAssetRegistry().register(HytaleAssetStore.builder(SessionPreset.class, new DefaultAssetMap<>())
                .setPath("Gauntlet/Plugins/" + ID + "/Presets")
                .setCodec(SessionPreset.CODEC)
                .setKeyFunction(SessionPreset::getId)
                .build());
        host.getAssetRegistry().register(HytaleAssetStore.builder(AutoSession.class, new DefaultAssetMap<>())
                .setPath("Gauntlet/Plugins/" + ID + "/Session")
                .setCodec(AutoSession.CODEC)
                .setKeyFunction(AutoSession::getId)
                .build());

        AutoComponent.setComponentType(SessionRegistry.register(AutoComponent.ID, AutoComponent.class,
                AutoComponent.CODEC));

        TriggerVolumesPlugin.get().registerEffectType(AutoVolume.ID, AutoVolume.class, AutoVolume.CODEC);

        GauntletEventRegistry.on(SessionEvent.class, AutoSessionEvents::onSession);
        GauntletEventRegistry.on(SessionStateEvent.class, AutoSessionEvents::onSessionState);
        GauntletEventRegistry.on(PlayerGameEvent.class, AutoSessionEvents::onPlayerGame);
        GauntletEventRegistry.on(PlayerPartyEvent.class, AutoSessionEvents::onPlayerParty);
        TimerUtils.registerOnComplete(ID, AutoSessionEvents::onTimerComplete);

        var events = host.getEventRegistry();
        events.register(EventPriority.LAST, PlayerDisconnectEvent.class, AutoSessionEvents::onDisconnect);
        events.register(AllWorldsLoadedEvent.class, AutoSessionEvents::onWorldsLoaded);
    }

    /**
     * Hands the countdown and vote checks to the hub thread. A throw would cancel
     * the schedule, so nothing escapes.
     */
    // private static void tick() {
    // try {
    // GauntletUtils.run(GauntletUtils.withHubWorld(), () -> {
    // try {
    // MatchUtils.tick();
    // VoteUtils.tick();
    // } catch (RuntimeException e) {
    // LOGGER.atWarning().withCause(e).log("Match tick failed");
    // }
    // });
    // } catch (RuntimeException e) {
    // // the universe is not up yet
    // }
    // }

    @Override
    public List<AdminTab> getAdminTabs() {
        return List.of(new AutoTab());
    }

    @Override
    public List<HudElement> getHudElements() {
        return List.of();
    }

    @Override
    public Map<String, PageFactory> getPages() {
        return Map.of(VoteUtils.PAGE_ID, VotePage::new);
    }
}
