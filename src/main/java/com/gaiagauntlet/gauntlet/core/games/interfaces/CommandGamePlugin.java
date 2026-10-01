package com.gaiagauntlet.gauntlet.core.games.interfaces;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;

import java.util.List;

public interface CommandGamePlugin extends GamePlugin {
    public List<AbstractCommand> getCommands();
}
