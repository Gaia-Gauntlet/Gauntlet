package gaiagauntlet.plugins.gamestate.constants;

/** Where the event is in its life cycle. Transitions are owned by the orchestrator. */
public enum MatchState {
    /** Nothing scheduled. Players roam the hub. */
    IDLE,
    /** The pre-portal countdown is running in the hub. */
    LOBBY_COUNTDOWN,
    /** The portal is open and the get-to-the-portal countdown is running. */
    PORTAL_OPEN,
    /** Players are being moved into the arena in waves. */
    TRANSFERRING,
    /** Everyone is in the arena, waiting on their platforms for the intro. */
    STAGING,
    /** The match is live. */
    ACTIVE,
    /** The match is live with sudden death rules. */
    SUDDEN_DEATH,
    /** A winner has been decided; the end screen is showing. */
    ENDED,
    /** Players are being moved back to the hub. */
    RETURNING
}
