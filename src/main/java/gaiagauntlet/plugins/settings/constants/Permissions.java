package gaiagauntlet.plugins.settings.constants;

public class Permissions {
    /** Required for every command and dashboard action that changes the event. */
    public static final String ADMIN = "gg.admin";

    /**
     * Lets a player leave the Creator Hall keeping their Creative mode and inventory, and drop items there
     * while in Creative. The hall's trigger volume checks this node by name. Ops hold every node.
     */
    public static final String CREATOR_HALL_KEEP = "gg.creatorhall.keep";

    private Permissions() {}
}
