package com.gaiagauntlet.gauntlet.core.session.constants;

import javax.annotation.Nonnull;

public enum SessionState {
    IDLE,
    SETTING_UP,
    RUNNING,
    CLEANING,
    FINISHED,
    ERROR;

    /**
     * Transition Rules for the closed state machine
     * Returns TRUE if the provided destination is a valid transition
     */
    public boolean to(@Nonnull SessionState destination) {
        // Inverts for my brain - Checks rules based on
        // <can destination come from current> instead
        return switch (destination) {
            case IDLE -> switch (this) { // <other> -> idle
                case FINISHED, IDLE, ERROR, CLEANING -> true;
                case RUNNING, SETTING_UP -> false;
            };
            case SETTING_UP -> switch (this) { // <other> -> setting up
                case FINISHED, IDLE, ERROR -> true;
                case CLEANING, SETTING_UP, RUNNING -> false;
            };
            case RUNNING -> switch (this) { // <other> -> running
                case RUNNING, SETTING_UP -> true;
                case FINISHED, CLEANING, IDLE, ERROR -> false;
            };
            case CLEANING -> switch (this) { // <other> -> cleaning
                case RUNNING, ERROR, SETTING_UP -> true;
                case FINISHED, CLEANING, IDLE -> false;
            };
            case FINISHED -> switch (this) { // <other> -> finished
                case FINISHED, IDLE, ERROR, CLEANING -> true;
                case RUNNING, SETTING_UP -> false;
            };
            case ERROR -> true; // <any> -> error
        };
    }

    /** Whether the state is considered 'active' and cannot be changed */
    public boolean active() {
        return switch (this) { // <other> -> finished
            case RUNNING, SETTING_UP, CLEANING -> true;
            case FINISHED, IDLE, ERROR -> false;
        };
    }
}
