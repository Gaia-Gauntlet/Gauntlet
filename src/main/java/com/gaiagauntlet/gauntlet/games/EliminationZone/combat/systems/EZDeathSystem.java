package com.gaiagauntlet.gauntlet.games.EliminationZone.combat.systems;

import com.gaiagauntlet.gauntlet.games.EliminationZone.EZGameConfig;
import com.gaiagauntlet.gauntlet.games.EliminationZone.combat.components.EZCombatComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.components.EZGameComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.components.EZPlayerComponent;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state.EZState;
import com.gaiagauntlet.gauntlet.games.EliminationZone.managers.state.EZStates;
import com.gaiagauntlet.gauntlet.plugins.announcer.utils.Announcer;
import com.gaiagauntlet.gauntlet.plugins.career.components.CareerStats;
import com.gaiagauntlet.gauntlet.plugins.career.utils.CareerUtils;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.utils.GameStore;
import com.gaiagauntlet.gauntlet.plugins.spectator.components.SpectatorComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.EliminatedComponent;
import com.gaiagauntlet.gauntlet.plugins.teams.components.entity.TeamPlayerComponent;
import com.gaiagauntlet.gauntlet.utils.PlayerUtils;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entity.gamemode.GameModeTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class EZDeathSystem extends DeathSystems.OnDeathSystem {

    private static final String SOUND_DEATH = "SFX_Emberwulf_Death";
    private static final String PARTICLE_SYSTEM_DEATH = "MemoryUnlock";

    // runs before the death screen system so the hidden menu and the spectator gamemode are in place when it reads them
    private static final Set<Dependency<EntityStore>> DEPENDENCIES = Set.of(
            new SystemDependency<>(Order.BEFORE, DeathSystems.PlayerDeathScreen.class));

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return EZPlayerComponent.getComponentType();
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return DEPENDENCIES;
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent death, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        var playerComp = store.getComponent(ref, EZPlayerComponent.getComponentType());
        if (playerComp == null) return;
        var gameStore = GameStore.withStore(store.getExternalData().getWorld(), playerComp.getSessionId()).orElse(null);
        if (gameStore == null)
            return;
        var state = EZState.currentState(gameStore);
        if (state != EZStates.RUNNING && state != EZStates.SUDDEN_DEATH)
            return;

        death.setShowDeathMenu(false);
        GameModeTypes.enterOnDeath(ref, commandBuffer, SpectatorComponent.GAME_MODE);
        if (store.getComponent(ref, SpectatorComponent.getComponentType()) == null) {
            commandBuffer.addComponent(ref, SpectatorComponent.getComponentType(), new SpectatorComponent());
        }
        if (store.getComponent(ref, EliminatedComponent.getComponentType()) != null)
            return;
        commandBuffer.addComponent(ref, EliminatedComponent.getComponentType(), new EliminatedComponent());

        var victim = store.getComponent(ref, PlayerRef.getComponentType());
        if (victim != null)
            gameStore.ensure(EZGameComponent.getComponentType(), EZGameComponent::new).eliminate(victim.getUuid());
        var killer = victim == null ? null : killerOf(victim, death, gameStore, store);
        var killerTeamPlayer = killer == null ? null
                : store.getComponent(killer, TeamPlayerComponent.getComponentType());
        if (killerTeamPlayer != null)
            killerTeamPlayer.incrementKills();

        var killerPlayer = killer != null
            ? killer.getStore().getComponent(killer, PlayerRef.getComponentType())
            : null;
        var playerRef = ref.getStore().getComponent(ref, PlayerRef.getComponentType());

        assert playerRef != null;
        var world = store.getExternalData().getWorld();
        sendDeathMessage(death, playerRef, killerPlayer, world);
        world.execute(() -> EZState.checkEnd(gameStore));
    }

    private static void sendDeathMessage(
        DeathComponent component,
        PlayerRef playerRef,
        @Nullable PlayerRef killerRef,
        World world
    ) {
        DamageCause damageCause = component.getDeathCause();
        Message damageCauseMessage = damageCause != null
            ? Message.translation("server.general.damageCauses." + damageCause.getId().toLowerCase())
            : Message.empty();

        if (killerRef != null) {
            // Death message
            Announcer.chat(world,
                Message.translation("gg.announcement.kill.player")
                    .param("killer", Message.raw(killerRef.getUsername()))
                    .param("victim", Message.raw(playerRef.getUsername()))
                    .param("killMethod", damageCauseMessage)
                    .bold(true)
            );
            CareerUtils.add(playerRef, CareerStats.DEATHS, 1L);
            CareerUtils.add(killerRef, CareerStats.KILLS, 1L);

        } else {
            Announcer.chat(world,
                Message.translation("gg.announcement.kill.environment")
                    .param("victim", Message.raw(playerRef.getUsername()))
                    .param("killMethod", damageCauseMessage)
                    .bold(true));
            CareerUtils.add(playerRef, CareerStats.DEATHS, 1L);
            CareerUtils.add(playerRef, CareerStats.NATURALDEATHS, 1L);
        }
        Ref<EntityStore> ref = playerRef.getReference();
        assert ref != null;
        ParticleUtil.spawnParticleEffect(
            PARTICLE_SYSTEM_DEATH,
            playerRef.getTransform().getPosition(),
            ref.getStore());
        Announcer.sound(world, SOUND_DEATH);

    }

    @Nullable
    private static Ref<EntityStore> killerOf(@Nonnull PlayerRef victim, @Nonnull DeathComponent death,
            @Nonnull GameEcs gameStore, @Nonnull Store<EntityStore> store) {
        var window = EZGameConfig.get(gameStore).getKillCreditSeconds();
        var recent = gameStore.get(EZCombatComponent.getComponentType())
                .map(combat -> combat.recentAttacker(victim.getUuid(), window))
                .map(PlayerUtils::get)
                .map(PlayerRef::getReference)
                .orElse(null);
        if (recent != null && recent.isValid() && recent.getStore() == store)
            return recent;
        var info = death.getDeathInfo();
        if (info != null && info.getSource() instanceof Damage.EntitySource source) {
            var sourceRef = source.getRef();
            if (sourceRef.isValid()
                && !sourceRef.equals(victim.getReference())
                && store.getComponent(sourceRef, PlayerRef.getComponentType()) != null)
                return sourceRef;
        }
        return null;
    }
}
