package com.gaiagauntlet.gauntlet.core.events.events;

import com.gaiagauntlet.gauntlet.core.events.GauntletEvent;
import com.gaiagauntlet.gauntlet.core.events.events.SessionPluginEvent.SessionPluginOp;

import lombok.Getter;

public class SessionPluginEvent extends GauntletEvent {
    @Getter private final String sessionId;
    @Getter private final String pluginId;
    @Getter private final SessionPluginOp op;

    public SessionPluginEvent(SessionPluginOp op, String sessionId, String pluginId) {
        this.op = op;
        this.sessionId = sessionId;
        this.pluginId = pluginId;
    }

    @Override
    public String toString() {
        return "SessionPluginEvent[" + sessionId + ": " + op + " " + pluginId + "]";
    }

    public enum SessionPluginOp {
        /** Installs the plugin and any dependencies the session is missing */
        INSTALL,
        /** Uninstalls the plugin unless another installed plugin depends on it */
        UNINSTALL
    }
}
