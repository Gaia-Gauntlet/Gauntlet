package com.gaiagauntlet.gauntlet.core.events;

import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.error;

import java.util.function.Consumer;

import com.gaiagauntlet.gauntlet.core.admin.AdminLog;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;

/**
 * Wrapper for all events
 * 
 * For your sanity, it goes down in "scale"
 * Universe -> Session -> Game -> Player
 */
public abstract class GauntletEvent implements IEvent<Void> {
    private static HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private Consumer<Message> consumer = null;
    private Consumer<Message> runnable = null;

    // part of the event builder to add a callback
    public GauntletEvent withMessages(Consumer<Message> consumer) {
        this.consumer = consumer;
        return this;
    }

    /** Registers a callback that is run exactly once at the end of the operation */
    public GauntletEvent withCallback(Consumer<Message> runnable) {
        this.runnable = runnable;
        return this;
    }

    public void complete() {
        complete(Message.raw("Completed Successfully"));
    }

    public void complete(Message reason) {
        if (this.runnable != null) {
            try {
                this.runnable.accept(reason);
                this.runnable = null; // clear the runner
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Failed to run callback for event!");
            }
        }
    }

    public void Error(String error) {
        AdminLog.add(error(error).getRawText());
        if (consumer == null)
            return;
        consumer.accept(error(error));
    }

    public void Message(Message message) {
        LOGGER.atWarning().log(message.getRawText());
        if (consumer == null)
            return;
        consumer.accept(message);
    }
}
