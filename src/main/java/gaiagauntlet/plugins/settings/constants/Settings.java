package gaiagauntlet.plugins.settings.constants;

import gaiagauntlet.plugins.settings.config.SettingKey;
import gaiagauntlet.plugins.settings.config.SettingsRegistry;

import javax.annotation.Nonnull;

/**
 * Every adjustable value in the plugin, declared once. Commands and the admin dashboard are generated
 * from this list, so a new setting is one line here plus the code that reads it.
 */
public final class Settings {

    // Hub
    public static final SettingKey<String> HUB_WORLD_NAME = SettingKey.text("hub.worldName",
            "Name of the main hub's permanent world", "GGHubHalloween");
    public static final SettingKey<String> HUB_INSTANCE_TEMPLATE = SettingKey.text("hub.instanceTemplate",
            "Instance template used to create a hub world when it does not exist", "GGHubHalloween");
    public static final SettingKey<Boolean> HUB_ENABLED = SettingKey.bool("hub.enabled",
            "Gather players in the hub world. When off, connecting players land straight in an open game's lobby and cannot leave it", false);
    public static final SettingKey<Boolean> HUB_SEND_NEW_PLAYERS = SettingKey.bool("hub.sendNewPlayers",
            "Move players who load into a world no hub or game owns to the main hub, or to an open game's lobby when the hub is off", true);

    // Lobby
    public static final SettingKey<Double> LOBBY_PRE_PORTAL_SECONDS = SettingKey.decimal("lobby.prePortalSeconds",
            "Countdown in the hub before the portal opens", 300, 0, 3600);
    public static final SettingKey<Double> LOBBY_POST_PORTAL_SECONDS = SettingKey.decimal("lobby.postPortalSeconds",
            "Time the portal stays open before everyone is transferred", 120, 0, 3600);
    public static final SettingKey<String> LOBBY_INSTANCE_TEMPLATE = SettingKey.text("lobby.instanceTemplate",
            "Instance template each game lobby is spawned from", "GGHubHalloween");
    public static final SettingKey<Integer> LOBBY_COUNT = SettingKey.integer("lobby.count",
            "How many lobby instances a new game gets", 1, 1, 16);

    // Transfer
    public static final SettingKey<Integer> TRANSFER_WAVE_SIZE = SettingKey.integer("transfer.waveSize",
            "Players moved between worlds together in one wave", 5, 1, 100);
    public static final SettingKey<Double> TRANSFER_WAVE_INTERVAL_SECONDS = SettingKey.decimal(
            "transfer.waveIntervalSeconds", "Delay between waves", 1, 0, 60);
    public static final SettingKey<Double> TRANSFER_READY_TIMEOUT_SECONDS = SettingKey.decimal(
            "transfer.readyTimeoutSeconds", "How long to wait for a client to load before retrying the move", 15, 1, 120);
    public static final SettingKey<Integer> TRANSFER_ARENA_SECTIONS_PER_SECOND = SettingKey.integer(
            "transfer.arenaSectionsPerSecond", "Chunk sections per second sent to a player entering the arena, or 0 for the normal rate", 120, 0, 2560);
    public static final SettingKey<Double> TRANSFER_ARENA_SLOW_SECONDS = SettingKey.decimal(
            "transfer.arenaSlowSeconds", "How long an arriving player gets the arena at the reduced rate", 15, 0, 120);

    // Arena
    public static final SettingKey<String> ARENA_INSTANCE_TEMPLATE = SettingKey.text("arena.instanceTemplate",
            "Instance template for the match arena", "GGEliminationZone");
    public static final SettingKey<Double> ARENA_START_BUFFER_SECONDS = SettingKey.decimal("arena.startBufferSeconds",
            "Wait after the last player arrives before the intro begins", 20, 0, 300);
    public static final SettingKey<Double> ARENA_CORNUCOPIA_SECONDS = SettingKey.decimal("arena.cornucopiaSeconds",
            "How long the spawn platforms take to rise", 27, 0, 300);
    public static final SettingKey<Double> ARENA_CAMERA_SEQUENCE_SECONDS = SettingKey.decimal(
            "arena.cameraSequenceSeconds", "Length of the intro camera sequence", 29, 0, 300);
    public static final SettingKey<Double> ARENA_GRACE_SECONDS = SettingKey.decimal("arena.graceSeconds",
            "Grace period after the match goes live during which players cannot be damaged", 60, 0, 600);

    // Match
    public static final SettingKey<String> FAKE_ROLE = SettingKey.text("fake.role",
            "NPC role the stand-in players of a load test are spawned as", "Temple_Kweebec_Merchant");

    public static final SettingKey<Integer> MATCH_SUDDEN_DEATH_AT = SettingKey.integer("match.suddenDeathAt",
            "Sudden death begins when this many players remain", 6, 0, 100);
    public static final SettingKey<Boolean> MATCH_FRIENDLY_FIRE = SettingKey.bool("match.friendlyFire",
            "Whether teammates can damage each other", false);
    public static final SettingKey<String> MATCH_SCORE_TABLE = SettingKey.text("match.scoreTable",
            "Points by placement, first place first. Teams past the end get the last value.",
            "5,4,3,2,1,0");
    public static final SettingKey<Double> MATCH_END_SCREEN_SECONDS = SettingKey.decimal("match.endScreenSeconds",
            "How long the winner screen shows before players return to the hub", 10, 0, 300);
    public static final SettingKey<Double> VOICE_TEAM_ONLY_AT_SECONDS_LEFT = SettingKey.decimal("voice.teamOnlyAtSecondsLeft",
            "Seconds left on the arena's pre-match countdown when living players start hearing only their own team", 56, 0, 3600);

    // Points of interest. Yaw is in radians, matching what the server reports for a player's facing.
    public static final SettingKey<Double> LOBBY_TIMER_X = SettingKey.decimal("lobby.timer.x", "Hub countdown display X", 27.54, -100000, 100000);
    public static final SettingKey<Double> LOBBY_TIMER_Y = SettingKey.decimal("lobby.timer.y", "Hub countdown display Y", 140, -1000, 1000);
    public static final SettingKey<Double> LOBBY_TIMER_Z = SettingKey.decimal("lobby.timer.z", "Hub countdown display Z", -240.54, -100000, 100000);
    public static final SettingKey<Double> LOBBY_TIMER_YAW = SettingKey.decimal("lobby.timer.yaw", "Hub countdown display facing (radians)", 3.1268, -7, 7);
    public static final SettingKey<Double> LOBBY_PORTAL_X = SettingKey.decimal("lobby.portal.x", "Hub portal X", 22.55, -100000, 100000);
    public static final SettingKey<Double> LOBBY_PORTAL_Y = SettingKey.decimal("lobby.portal.y", "Hub portal Y", 122, -1000, 1000);
    public static final SettingKey<Double> LOBBY_PORTAL_Z = SettingKey.decimal("lobby.portal.z", "Hub portal Z", -425.63, -100000, 100000);
    public static final SettingKey<Double> LOBBY_PORTAL_YAW = SettingKey.decimal("lobby.portal.yaw", "Hub portal facing (radians)", 3.1163, -7, 7);
    public static final SettingKey<Double> ARENA_TIMER_X = SettingKey.decimal("arena.timer.x", "Arena countdown display X", -98, -100000, 100000);
    public static final SettingKey<Double> ARENA_TIMER_Y = SettingKey.decimal("arena.timer.y", "Arena countdown display Y", 58, -1000, 1000);
    public static final SettingKey<Double> ARENA_TIMER_Z = SettingKey.decimal("arena.timer.z", "Arena countdown display Z", 0, -100000, 100000);
    public static final SettingKey<Double> ARENA_TIMER_YAW = SettingKey.decimal("arena.timer.yaw", "Arena countdown display facing (radians)", Math.PI, -7, 7);

    // Zones
    public static final SettingKey<Double> ZONE_START_SECONDS = SettingKey.decimal("zone.startSeconds",
            "Seconds after the match goes live before the first zone starts closing", 90, 0, 3600);
    public static final SettingKey<Double> ZONE_TICK_SECONDS = SettingKey.decimal("zone.tickSeconds",
            "How often the closing edge moves and the void repaints", 2, 0.25, 10);

    // Bosses
    public static final SettingKey<Integer> BOSS_MAX_ACTIVE = SettingKey.integer("boss.maxActive",
            "How many bosses may be in the arena at once", 2, 1, 10);

    // Events
    public static final SettingKey<String> EVENTS_PRESET = SettingKey.text("events.preset",
            "Name of the match event preset in events.json that the game runs", "Standard");

    // Admin
    public static final SettingKey<String> ADMIN_PLAYERS = SettingKey.text("admin.players",
            "GG admins, separated by commas. Only they see the plugin's warnings in chat and can open /gg admin", "MelodicAlbuild,Riprod");

    private static final SettingKey<?>[] ALL = {
            HUB_WORLD_NAME, HUB_INSTANCE_TEMPLATE, HUB_SEND_NEW_PLAYERS,
            LOBBY_PRE_PORTAL_SECONDS, LOBBY_POST_PORTAL_SECONDS, LOBBY_INSTANCE_TEMPLATE, LOBBY_COUNT,
            TRANSFER_WAVE_SIZE, TRANSFER_WAVE_INTERVAL_SECONDS, TRANSFER_READY_TIMEOUT_SECONDS,
            TRANSFER_ARENA_SECTIONS_PER_SECOND, TRANSFER_ARENA_SLOW_SECONDS,
            ARENA_INSTANCE_TEMPLATE, ARENA_START_BUFFER_SECONDS, ARENA_CORNUCOPIA_SECONDS,
            ARENA_CAMERA_SEQUENCE_SECONDS, ARENA_GRACE_SECONDS,
            MATCH_SUDDEN_DEATH_AT, MATCH_FRIENDLY_FIRE, MATCH_SCORE_TABLE, MATCH_END_SCREEN_SECONDS,
            VOICE_TEAM_ONLY_AT_SECONDS_LEFT,
            ZONE_START_SECONDS, ZONE_TICK_SECONDS,
            BOSS_MAX_ACTIVE, EVENTS_PRESET, FAKE_ROLE, ADMIN_PLAYERS,
            LOBBY_TIMER_X, LOBBY_TIMER_Y, LOBBY_TIMER_Z, LOBBY_TIMER_YAW,
            LOBBY_PORTAL_X, LOBBY_PORTAL_Y, LOBBY_PORTAL_Z, LOBBY_PORTAL_YAW,
            ARENA_TIMER_X, ARENA_TIMER_Y, ARENA_TIMER_Z, ARENA_TIMER_YAW,
    };

    private static SettingsRegistry registry;

    private Settings() {}

    /** Creates the registry with every key registered. The plugin loads it once its config exists. */
    @Nonnull
    public static SettingsRegistry create() {
        var created = new SettingsRegistry();
        for (var key : ALL) {
            created.register(key);
        }
        registry = created;
        return created;
    }

    @Nonnull
    public static SettingsRegistry get() {
        if (registry == null) {
            throw new IllegalStateException("Settings used before the plugin initialized them");
        }
        return registry;
    }

    public static <T> T value(@Nonnull SettingKey<T> key) {
        return get().get(key);
    }
}
