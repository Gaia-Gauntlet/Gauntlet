package com.gaiagauntlet.gauntlet.core.events;

import java.util.Collection;
import java.util.function.Consumer;

import com.gaiagauntlet.gauntlet.core.admin.AdminLog;
import com.gaiagauntlet.gauntlet.core.session.components.GameSession;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import static com.gaiagauntlet.gauntlet.plugins.announcer.utils.MessageUtils.*;

import lombok.Getter;

/**
 * Wrapper for all events
 * 
 * For your sanity, it goes down in "scale"
 * Universe -> Session -> Game -> Player
 */
public abstract class GauntletEvent {

    public abstract class Event implements IEvent<Void> {
        private static HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

        private Consumer<Message> consumer = null;
        private Consumer<Message> runnable = null;

        // part of the event builder to add a callback
        public Event withMessages(Consumer<Message> consumer) {
            this.consumer = consumer;
            return this;
        }
        /** Registers a callback that is run exactly once at the end of the operation */
        public Event withCallback(Consumer<Message> runnable) {
            this.runnable = runnable;
            return this;
        }

        public void complete() {
            complete(null);
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

    /**
     * | ---------------------- \
     * Session Management
     * \ ---------------------- |
     */

    public class NewSession extends Event {
        @Getter
        private GameSession newSession;

        public NewSession(GameSession newSession) {
            this.newSession = newSession;
        }

    }

    public class Session extends Event {
        @Getter
        private GauntletEvent.SessionOperation op;
        @Getter
        private String sessionId;

        public Session(SessionOperation op, String id) {
            this.op = op;
            this.sessionId = id;
        }
    }

    public enum SessionOperation {
        /** Starts the session */
        SETUP,
        /** Cancels the current game in the session */
        CANCEL,
        /** Deletes the session */
        DELETE
    }

    /** Sets the session's game queue - does NOT override the current game */
    public class SessionQueue extends Event {
        @Getter private final String sessionId;
        @Getter private final Collection<String> newQueue;
        @Getter private final SessionQueueOp op = SessionQueueOp.SET;

        public SessionQueue(String sessionId, Collection<String> games) {
            newQueue = games;
            this.sessionId = sessionId;
        }
    }

    public enum SessionQueueOp {
        SET,
        REMOVE,
        APPEND
    }

    /**
     * | ---------------------- \
     * Game Management
     * \ ---------------------- |
     */

    /** Operations relating to the game */
    public class Game extends Event {
        @Getter
        private GauntletEvent.GameOperation op;
    }

    public enum GameOperation {
        START,
        STOP,
        CANCEL,
        NEXT
    }

    /**
     * Natural game-end event. Emitted by games when they complete
     * This triggers the cleanup code for the controller
     */
    public class GameEnd extends Event {

    }

    /**
     * | ---------------------- \
     * Player Management
     * \ ---------------------- |
     */

    /** Player has connected */
    public class ConnectPlayer extends Event {

    }

    /** Player has disconnected */
    public class DisconnectPlayer extends Event {

    }

    /** Adding a player to a game/session */
    public class AddPlayer extends Event {

    }

    /** Removing a player from a game/session */
    public class RemovePlayer extends Event {

    }
}
