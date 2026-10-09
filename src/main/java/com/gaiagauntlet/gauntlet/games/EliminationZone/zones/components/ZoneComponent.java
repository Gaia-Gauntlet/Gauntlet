package com.gaiagauntlet.gauntlet.games.EliminationZone.zones.components;

import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.core.events.GauntletEventRegistry;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.VoidTerrain;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.events.ZoneEvents;
import com.gaiagauntlet.gauntlet.games.EliminationZone.zones.services.ZoneTickSystem;
import com.gaiagauntlet.gauntlet.plugins.announcer.utils.Announcer;
import com.gaiagauntlet.gauntlet.plugins.events.events.MatchEventRegistry;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponent;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameComponentType;
import com.gaiagauntlet.gauntlet.plugins.gamestore.components.GameEcs;
import com.gaiagauntlet.gauntlet.plugins.gamestore.registry.GameComponentRegistry;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.EventTitleStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.World;

import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The zone closing sequence of one match: the shuffled order zones close in,
 * where the current
 * zone's edge is, the hold between zones, and the void terrain that follows the
 * edge. Every method
 * runs on the arena thread. The sequence is driven by {@link ZoneTickSystem}
 * and reports each
 * milestone as a {@link ZoneEvents} event.
 */
public final class ZoneComponent implements GameComponent {

    public static final GameComponentType<@NotNull ZoneComponent> TYPE = GameComponentRegistry.register(
            "Zones", ZoneComponent.class);

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String SOUND_ZONE_CLOSING = "SFX_Discovery_Z4_Short";

    private String sessionId = "";
    @Nullable
    private World arena;
    private List<ZoneDefinition> order = new ArrayList<>();
    /**
     * The last match's closing order by zone id. Kept across matches so the next
     * order can differ from it.
     */
    private List<String> previousOrder = List.of();
    /** How many shuffles are tried to find an order unlike the last match's. */
    private static final int SHUFFLE_TRIES = 12;
    private List<ZonePhase> phases = List.of();
    private final List<ZoneDefinition> closed = new ArrayList<>();
    private final VoidTerrain voidTerrain = new VoidTerrain();

    private boolean active;
    private boolean paused;
    private int stepIndex = -1;
    private double pendingDelaySeconds;
    private double closeElapsed;
    private double holdElapsed;
    private boolean holding;
    private boolean warningFired;
    private double closeRadius;
    @Getter
    private float accumulated;

    public ZoneComponent(String sessionId) {
        this.sessionId = sessionId;
    }

    // Sequence control

    /**
     * Of a few shuffles, the one with the fewest zones in the same position as the
     * last match's order.
     */
    @Nonnull
    private List<ZoneDefinition> leastLikePrevious(@Nonnull List<ZoneDefinition> zones) {
        List<ZoneDefinition> best = null;
        int bestShared = Integer.MAX_VALUE;
        for (int attempt = 0; attempt < SHUFFLE_TRIES && bestShared > 0; attempt++) {
            var candidate = new ArrayList<>(zones);
            Collections.shuffle(candidate, ThreadLocalRandom.current());
            int shared = 0;
            for (int i = 0; i < candidate.size() && i < previousOrder.size(); i++) {
                if (candidate.get(i).id().equals(previousOrder.get(i))) {
                    shared++;
                }
            }
            if (shared < bestShared) {
                best = candidate;
                bestShared = shared;
            }
        }
        return best;
    }

    public boolean increment(float dt, float tickSeconds) {
        accumulated += dt;
        if (accumulated < tickSeconds)
            return false;
        tick(accumulated);
        accumulated = 0;
        return true;
    }

    /**
     * Starts the sequence in the arena with the zones in a random order, picked to
     * share as few
     * positions as it can with the last match's order. Closing begins after the
     * delay; zero starts the
     * first zone immediately.
     */
    public void begin(@Nonnull String gameId, @Nonnull World arena, @Nonnull List<ZoneDefinition> zones,
            @Nonnull List<ZonePhase> phases, double delaySeconds) {
        if (zones.isEmpty() || phases.isEmpty()) {
            LOGGER.atWarning().log("[%s] Zone closing not started: %d zones, %d phases", gameId, zones.size(),
                    phases.size());
            return;
        }
        this.sessionId = gameId;
        this.arena = arena;
        this.order = leastLikePrevious(zones);
        previousOrder = this.order.stream().map(ZoneDefinition::id).toList();
        this.phases = List.copyOf(phases);
        closed.clear();
        voidTerrain.reset();
        active = true;
        paused = false;
        stepIndex = -1;
        pendingDelaySeconds = Math.max(0.0, delaySeconds);
        holding = false;
        LOGGER.atInfo().log("[%s] Zone closing armed: %s, first zone in %.0fs", gameId,
                String.join(", ", this.order.stream().map(ZoneDefinition::id).toList()), pendingDelaySeconds);
        if (pendingDelaySeconds <= 0.0) {
            enterStep(0);
        }
    }

    public void stop() {
        if (active) {
            LOGGER.atInfo().log("[%s] Zone closing stopped", sessionId);
        }
        active = false;
    }

    /** Advances the sequence by the elapsed seconds. */
    public void tick(float seconds) {
        if (!active || paused || seconds <= 0.0 || isFinished()) {
            return;
        }
        var step = seconds;
        if (pendingDelaySeconds > 0.0) {
            if (pendingDelaySeconds >= step) {
                pendingDelaySeconds -= step;
                return;
            }
            step -= pendingDelaySeconds;
            pendingDelaySeconds = 0.0;
        }
        if (stepIndex < 0) {
            enterStep(0);
        }
        if (holding) {
            advanceHold(step);
        } else {
            advanceClose(step);
        }
        maybeWarn();
    }

    /**
     * Repaints the newly voided ground behind the edge. Chunk loads are
     * asynchronous, so this catches up over ticks.
     */
    public void paintVoid(String sessionId) {
        if (!active || arena == null || stepIndex < 0) {
            return;
        }
        var bands = new ArrayList<VoidTerrain.Band>();
        for (var zone : closed) {
            bands.add(new VoidTerrain.Band(zone, zone.innerRadius()));
        }
        var current = activeZone();
        if (current != null && !closed.contains(current)) {
            bands.add(new VoidTerrain.Band(current, closeRadius));
        }
        voidTerrain.advance(sessionId, arena, bands, this::isInVoid);
    }

    private void advanceClose(double step) {
        var phase = phaseFor(stepIndex);
        double duration = phase.durationSeconds();
        double elapsed = closeElapsed + step;
        var zone = activeZone();
        if (zone == null) {
            return;
        }
        if (elapsed < duration) {
            closeElapsed = elapsed;
            closeRadius = closeRadiusAt(zone, elapsed, duration);
            return;
        }
        closeElapsed = duration;
        closeRadius = zone.innerRadius();
        seal();
        if (holding) {
            advanceHold(elapsed - duration);
        }
    }

    private void advanceHold(double step) {
        double hold = phaseFor(stepIndex).holdSeconds();
        double elapsed = holdElapsed + step;
        if (elapsed < hold) {
            holdElapsed = elapsed;
            return;
        }
        double leftover = elapsed - hold;
        holdElapsed = hold;
        holding = false;
        enterStep(stepIndex + 1);
        if (leftover > 0.0 && !isFinished()) {
            advanceClose(leftover);
        }
    }

    /**
     * The edge position after {@code elapsed} of a close: the sector's area shrinks
     * linearly, so the radius follows a square.
     */
    private static double closeRadiusAt(@Nonnull ZoneDefinition zone, double elapsed, double duration) {
        if (duration <= 0.0 || elapsed >= duration) {
            return zone.innerRadius();
        }
        if (elapsed <= 0.0) {
            return zone.outerRadius();
        }
        double t = elapsed / duration;
        return zone.outerRadius() + (zone.innerRadius() - zone.outerRadius()) * t * t;
    }

    private void enterStep(int index) {
        stepIndex = index;
        closeElapsed = 0.0;
        holdElapsed = 0.0;
        warningFired = false;
        if (index >= order.size()) {
            LOGGER.atInfo().log("[%s] Every zone is closed; the center is the final safe area", sessionId);
            return;
        }
        var zone = order.get(index);
        closeRadius = zone.outerRadius();
        var phase = phaseFor(index);
        LOGGER.atInfo().log("[%s] Zone %d '%s' closing over %.0fs (%.0f to %.0f)", sessionId, index, zone.id(),
                phase.durationSeconds(), zone.outerRadius(), zone.innerRadius());
        GaiaLog.atInfo().log("Zone " + zone.id() + " closing over " + Math.round(phase.durationSeconds()) + "s")
                .withGameId(sessionId);
        if (arena != null) {
            Announcer.title(arena, Message.raw(zone.id()), Message.raw("ZONE CLOSING"), EventTitleStyle.VoidEviction,
                    SOUND_ZONE_CLOSING);
            Announcer.chat(arena, Message.raw(zone.id() + " is closing in!").color(Color.RED));
            MatchEventRegistry.dispatch(
                    new ZoneEvents.ClosingStarted(sessionId, index, zone, phase.durationSeconds()), EZController.ID);
        }
    }

    private void seal() {
        var zone = activeZone();
        if (zone == null) {
            return;
        }
        closed.add(zone);
        boolean last = stepIndex + 1 >= order.size();
        if (last) {
            holding = false;
            stepIndex = order.size();
        } else {
            holding = true;
            holdElapsed = 0.0;
        }
        LOGGER.atInfo().log("[%s] Zone '%s' sealed%s", sessionId, zone.id(), last ? " (final zone)" : "");
        GaiaLog.atInfo().log("Zone " + zone.id() + " sealed" + (last ? ", every zone is closed" : ""))
                .withGameId(sessionId);
        if (arena != null) {
            Announcer.chat(arena, Message.raw(zone.id() + " is sealed!").color(Color.RED));
            MatchEventRegistry.dispatch(
                    new ZoneEvents.Closed(sessionId, last ? order.size() - 1 : stepIndex, zone, last), EZController.ID);
        }
    }

    private void maybeWarn() {
        if (warningFired || isFinished() || holding || stepIndex < 0) {
            return;
        }
        var phase = phaseFor(stepIndex);
        double remaining = sealSeconds();
        if (remaining > phase.warningSeconds()) {
            return;
        }
        warningFired = true;
        var zone = activeZone();
        var next = nextZone();
        if (zone == null || arena == null) {
            return;
        }
        var text = next == null
                ? zone.id() + " seals in " + Math.round(remaining) + "s, the last zone!"
                : zone.id() + " seals in " + Math.round(remaining) + "s, " + next.id() + " is next!";
        Announcer.chat(arena, Message.raw(text).color(Color.RED));
        MatchEventRegistry.dispatch(new ZoneEvents.Warning(sessionId, zone, remaining), EZController.ID);
    }

    // Admin controls

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    /** Ends the current wait, close, or hold right now. */
    public void skip() {
        if (!active || isFinished()) {
            throw new IllegalStateException("Every zone has already closed");
        }
        pendingDelaySeconds = 0.0;
        if (stepIndex < 0) {
            enterStep(0);
        } else if (holding) {
            holding = false;
            enterStep(stepIndex + 1);
        } else {
            closeElapsed = phaseFor(stepIndex).durationSeconds();
            var zone = activeZone();
            closeRadius = zone == null ? closeRadius : zone.innerRadius();
            seal();
        }
    }

    /** Adds seconds before the next milestone. */
    public void delay(double seconds) {
        if (!active || seconds <= 0.0) {
            return;
        }
        pendingDelaySeconds += seconds;
    }

    /** Moves the named zone to the front of the zones still waiting. */
    public void setNext(@Nonnull String zoneId) {
        if (!active) {
            throw new IllegalStateException("Zone closing is not running");
        }
        int firstPending = stepIndex + 1;
        int current = -1;
        for (int i = 0; i < order.size(); i++) {
            if (order.get(i).id().equalsIgnoreCase(zoneId)) {
                current = i;
            }
        }
        if (current < firstPending || current >= order.size()) {
            throw new IllegalArgumentException("'" + zoneId + "' is not a zone still waiting to close. Waiting: "
                    + String.join(", ", pendingZones().stream().map(ZoneDefinition::id).toList()));
        }
        if (current != firstPending) {
            var zone = order.remove(current);
            order.add(firstPending, zone);
        }
    }

    // Queries

    public boolean isActive() {
        return active;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isFinished() {
        return stepIndex >= order.size();
    }

    public boolean isHolding() {
        return holding;
    }

    /** True once a zone's edge has started moving. */
    public boolean isClosingStarted() {
        return active && stepIndex >= 0 && !isFinished();
    }

    @Nullable
    public ZoneDefinition activeZone() {
        return stepIndex >= 0 && stepIndex < order.size() ? order.get(stepIndex) : null;
    }

    @Nullable
    public ZoneDefinition nextZone() {
        int next = stepIndex + 1;
        return next >= 0 && next < order.size() ? order.get(next) : null;
    }

    @Nonnull
    public List<ZoneDefinition> closedZones() {
        return List.copyOf(closed);
    }

    /** Zones not yet sealed, the active one first. */
    @Nonnull
    public List<ZoneDefinition> pendingZones() {
        int from = Math.clamp(stepIndex, 0, order.size());
        return List.copyOf(order.subList(from, order.size()));
    }

    @Nonnull
    public List<ZoneDefinition> order() {
        return List.copyOf(order);
    }

    public int stepIndex() {
        return stepIndex;
    }

    public double closeRadius() {
        return closeRadius;
    }

    public double pendingDelaySeconds() {
        return pendingDelaySeconds;
    }

    /** Progress of the active zone's close, 0 to 1. */
    public double progress() {
        var zone = activeZone();
        if (zone == null) {
            return isFinished() ? 1.0 : 0.0;
        }
        double span = zone.outerRadius() - zone.innerRadius();
        return span <= 0.0 ? 1.0 : Math.clamp((zone.outerRadius() - closeRadius) / span, 0.0, 1.0);
    }

    /** Seconds until the active zone seals. */
    public double sealSeconds() {
        if (isFinished() || holding || stepIndex < 0) {
            return 0.0;
        }
        return Math.max(0.0, phaseFor(stepIndex).durationSeconds() - closeElapsed);
    }

    /**
     * Seconds until the next milestone: the first zone, the seal, or the end of the
     * hold.
     */
    public double timerSeconds() {
        if (isFinished()) {
            return 0.0;
        }
        if (stepIndex < 0) {
            return pendingDelaySeconds;
        }
        var phase = phaseFor(stepIndex);
        if (holding) {
            return pendingDelaySeconds + Math.max(0.0, phase.holdSeconds() - holdElapsed);
        }
        return pendingDelaySeconds + sealSeconds();
    }

    /**
     * True when the point is in a sealed zone's band or behind the active zone's
     * edge.
     */
    public boolean isInVoid(double x, double y, double z) {
        if (!active) {
            return false;
        }
        for (var zone : closed) {
            if (zone.isInVoid(zone.innerRadius(), x, y, z)) {
                return true;
            }
        }
        var current = activeZone();
        return current != null && current.isInVoid(closeRadius, x, y, z);
    }

    @Nonnull
    public VoidTerrain voidTerrain() {
        return voidTerrain;
    }

    @Nonnull
    private ZonePhase phaseFor(int index) {
        return phases.get(Math.clamp(index, 0, phases.size() - 1));
    }

    public void reset() {
        active = false;
        paused = false;
        stepIndex = -1;
        closed.clear();
        order = new ArrayList<>();
        pendingDelaySeconds = 0.0;
        holding = false;
        voidTerrain.reset();
        arena = null;
        accumulated = 0;
    }
}
