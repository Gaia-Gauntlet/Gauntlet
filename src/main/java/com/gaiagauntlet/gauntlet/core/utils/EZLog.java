package com.gaiagauntlet.gauntlet.core.utils;

import com.gaiagauntlet.gauntlet.core.admin.GaiaLog;
import com.gaiagauntlet.gauntlet.games.EliminationZone.EZController;

/** GaiaLog wrapper with a few QOL items */
public class EZLog {
    public static GaiaLog info() {
        return GaiaLog.atInfo().withGameId(EZController.ID);
    }
    public static GaiaLog warn() {
        return GaiaLog.atWarning().withGameId(EZController.ID);
    }
    public static GaiaLog error() {
        return GaiaLog.atError().withGameId(EZController.ID);
    }
    public static GaiaLog debug() {
        return GaiaLog.atDebug().withGameId(EZController.ID);
    }
}
