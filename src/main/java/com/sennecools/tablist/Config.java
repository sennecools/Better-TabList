package com.sennecools.tablist;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Manages the configuration for the TabList mod.
 * <p>
 * This class defines the configuration values for the header, footer, and update interval.
 * It also provides a mechanism to reload these values when the config changes.
 */
@EventBusSubscriber(modid = TabList.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ── Appearance ──────────────────────────────────────────────────────
    static {
        BUILDER.comment("""
                Available placeholders (use in header, footer, and display_name_format):

                  General:
                    #N           - New line
                    #SERVERNAME  - Server name (set in config)
                    #TPS         - Ticks per second
                    #CTPS        - TPS with automatic color (green/yellow/red)
                    #MSPT        - Milliseconds per tick
                    #MEMORY      - Memory usage (used / max)
                    #UPTIME      - Server uptime

                  Players:
                    #PLAYERCOUNT - Number of players online
                    #MAXPLAYERS  - Max player slots
                    #PLAYERNAME  - Viewing player's name
                    #PING        - Player ping in ms
                    #RANK        - Player rank (requires FTB Ranks)
                    #AFK         - Shows "AFK" if the player is AFK, empty otherwise

                  World:
                    #WORLD       - Player's current dimension
                    #DATE        - Real date (yyyy-MM-dd)
                    #TIME        - Real time (HH:mm)

                Color codes: use & followed by a color/format code (e.g. &a for green, &l for bold).
                Hex colors: use &#RRGGBB (e.g. &#FF5555 for red).

                Animation: header and footer accept multiple entries (list). Each entry is
                a frame that cycles every update interval.""");
        BUILDER.push("appearance");
    }

    private static final ModConfigSpec.ConfigValue<String> SERVER_NAME = BUILDER
            .comment("Your server's name. Use #SERVERNAME in header/footer to insert it.",
                     "Handy for animation frames so you only type it once.")
            .define("server_name", "Your Server");

    private static final ModConfigSpec.ConfigValue<List<? extends String>> HEADER = BUILDER
            .comment("Text shown above the player list. Supports placeholders and & color codes.",
                     "Multiple entries create animation frames that cycle each update interval.")
            .defineList("header",
                    List.of(
                            "#N        &#FF5555&l#SERVERNAME        #N&#AAAAAA&m            #N",
                            "#N        &#5555FF&l#SERVERNAME        #N&#AAAAAA&m            #N"
                    ),
                    () -> "",
                    obj -> obj instanceof String);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> FOOTER = BUILDER
            .comment("Text shown below the player list. Supports placeholders and & color codes.",
                     "Multiple entries create animation frames that cycle each update interval.")
            .defineList("footer",
                    List.of(
                            "&fOnline: &e#PLAYERCOUNT&7/&e#MAXPLAYERS #N&7TPS: #CTPS &7| MSPT: &#55FFFF#MSPT #N&7Memory: &#AA55FF#MEMORY &7| Uptime: &#FFAA00#UPTIME",
                            "&fOnline: &e#PLAYERCOUNT&7/&e#MAXPLAYERS #N&7TPS: #CTPS &7| Ping: &#55FFFF#PING&7ms #N&7Memory: &#AA55FF#MEMORY &7| Uptime: &#FFAA00#UPTIME"
                    ),
                    () -> "",
                    obj -> obj instanceof String);

    private static final ModConfigSpec.ConfigValue<String> DISPLAY_NAME_FORMAT = BUILDER
            .comment("Display name format for each player in the tab list.",
                     "Supports {name} and {rank} placeholders + & color codes.",
                     "Example: \"&c[{rank}] &f{name} &7#AFK\"")
            .define("display_name_format", "{name} &7#AFK");

    private static final ModConfigSpec.IntValue UPDATE_INTERVAL = BUILDER
            .comment("How often (in milliseconds) the tab list refreshes. Lower = smoother but more traffic.",
                     "Range: 1 - 10000. Default: 500.")
            .defineInRange("update_interval", 500, 1, 10000);

    private static final ModConfigSpec.IntValue ANIMATION_INTERVAL = BUILDER
            .comment("How many update cycles between animation frame changes.",
                     "For example, with update_interval=500 and animation_interval=4, frames change every 2 seconds.",
                     "Range: 1 - 200. Default: 4.")
            .defineInRange("animation_interval", 4, 1, 200);

    static { BUILDER.pop(); }

    // ── Sorting ─────────────────────────────────────────────────────────
    static { BUILDER.push("sorting"); }

    private static final ModConfigSpec.ConfigValue<String> SORT_MODE = BUILDER
            .comment("How to sort players in the tab list.",
                     "NONE: default Minecraft ordering",
                     "ALPHABETICAL: sort by player name (case-insensitive)",
                     "RANK: sort by FTB Ranks power descending, then name (falls back to alphabetical without FTB Ranks)")
            .define("sort_mode", "NONE");

    static { BUILDER.pop(); }

    // ── FTB Ranks ───────────────────────────────────────────────────────
    static { BUILDER.push("ftbranks"); }

    private static final ModConfigSpec.BooleanValue ENABLE_FTBRANKS_FORMATTING = BUILDER
            .comment("When true and FTB Ranks is loaded, uses the ftbranks.name_format permission",
                     "instead of the display_name_format config template.")
            .define("enable_ftbranks_formatting", true);

    static { BUILDER.pop(); }

    // ── AFK ─────────────────────────────────────────────────────────────
    static { BUILDER.push("afk"); }

    private static final ModConfigSpec.BooleanValue AFK_ENABLED = BUILDER
            .comment("Enable AFK detection. AFK players have greyed-out names and the #AFK placeholder resolves to 'AFK'.")
            .define("afk_enabled", true);

    private static final ModConfigSpec.IntValue AFK_TIMEOUT = BUILDER
            .comment("Seconds of inactivity before a player is considered AFK.",
                     "Range: 10 - 3600. Default: 300 (5 minutes).")
            .defineInRange("afk_timeout", 300, 10, 3600);

    static { BUILDER.pop(); }

    // ── Spec ────────────────────────────────────────────────────────────
    public static final ModConfigSpec SPEC = BUILDER.build();

    // Runtime values loaded from config.
    public static String serverName;
    public static List<String> headerFrames;
    public static List<String> footerFrames;
    public static int updateInterval;
    public static int animationInterval;
    public static String displayNameFormat;
    public static boolean enableFTBRanksFormatting;
    public static String sortMode;
    public static boolean afkEnabled;
    public static int afkTimeout;

    @SubscribeEvent
    static void onLoad(ModConfigEvent event) {
        serverName = SERVER_NAME.get();
        headerFrames = List.copyOf(HEADER.get());
        footerFrames = List.copyOf(FOOTER.get());
        updateInterval = UPDATE_INTERVAL.get();
        animationInterval = ANIMATION_INTERVAL.get();
        displayNameFormat = DISPLAY_NAME_FORMAT.get();
        enableFTBRanksFormatting = ENABLE_FTBRANKS_FORMATTING.get();
        sortMode = SORT_MODE.get();
        afkEnabled = AFK_ENABLED.get();
        afkTimeout = AFK_TIMEOUT.get();
        TabList.LOGGER.info("TabList config reloaded. Update interval: {} ms", updateInterval);
    }
}
