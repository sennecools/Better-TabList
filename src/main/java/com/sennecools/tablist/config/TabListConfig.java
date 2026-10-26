package com.sennecools.tablist.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.sennecools.tablist.Constants;
import com.sennecools.tablist.platform.Services;

import java.nio.file.Path;
import java.util.List;

public class TabListConfig {

    // Runtime values
    public static String serverName;
    public static List<String> headerFrames;
    public static List<String> footerFrames;
    public static int updateInterval;
    public static int animationInterval;
    public static String displayNameFormat;
    public static NameFormattingProvider nameFormattingProvider;
    public static String sortMode;
    public static boolean afkEnabled;
    public static int afkTimeout;
    public static boolean useSparkMetrics;

    public static void load() {
        load(Services.PLATFORM.getConfigDir().resolve("tablist.toml"));
    }

    static void load(Path configPath) {
        try (CommentedFileConfig config = CommentedFileConfig.builder(configPath)
                .sync()
                .preserveInsertionOrder()
                .build()) {

            config.load();

            boolean needsSave = false;

            // Preserve the old FTB setting when upgrading an existing config.
            String defaultProvider = config.getOrElse("ftbranks.enable_ftbranks_formatting", true)
                    ? "FTB" : "NONE";

            // ── Appearance ──
            needsSave |= setDefaultIfMissing(config, "appearance.server_name", "Your Server",
                    "Your server's name, shown wherever you use #SERVERNAME.");
            needsSave |= setDefaultIfMissing(config, "appearance.header", List.of(
                    "#N        &#FF5555&l#SERVERNAME        #N&#AAAAAA&m            #N",
                    "#N        &#5555FF&l#SERVERNAME        #N&#AAAAAA&m            #N"
            ), """
                    Text above the player list. Each quoted entry is one animation frame.
                    Use #N for a new line WITHIN a frame; extra entries create animation, not extra lines.
                    One entry = static header. An empty list [] hides the header.
                    Colors: &a = green, &l = bold, &r = reset, &#FF5555 = hex color.
                    """.stripTrailing());
            needsSave |= setDefaultIfMissing(config, "appearance.footer", List.of(
                    "&fOnline: &e#PLAYERCOUNT&7/&e#MAXPLAYERS #N&7TPS: #CTPS &7| MSPT: &#55FFFF#MSPT #N&7Memory: &#AA55FF#MEMORY &7| Uptime: &#FFAA00#UPTIME",
                    "&fOnline: &e#PLAYERCOUNT&7/&e#MAXPLAYERS #N&7TPS: #CTPS &7| Ping: &#55FFFF#PING&7ms #N&7Memory: &#AA55FF#MEMORY &7| Uptime: &#FFAA00#UPTIME"
            ), """
                    Text below the player list. Same frame/new-line rules as the header.
                    Common placeholders: #PLAYERCOUNT, #MAXPLAYERS, #CTPS, #MSPT, #PING, #MEMORY, #UPTIME.
                    Example: "&fOnline: &e#PLAYERCOUNT&7/#MAXPLAYERS #N&7TPS: #CTPS"
                    """.stripTrailing());
            needsSave |= setDefaultIfMissing(config, "appearance.name_formatting_provider", defaultProvider,
                    """
                    Choose ONE optional integration for name formatting and rank sorting:
                    "NONE" = use display_name_format without rank integration.
                    "FTB"  = use FTB Ranks; ftbranks.name_format overrides display_name_format when set.
                    "LP"   = use LuckPerms prefixes, suffixes, primary groups, and inherited group weights.
                    Install the chosen integration separately. Without it, names still work.
                    The default is FTB for compatibility with older configs.
                    """.stripTrailing());
            needsSave |= setDefaultIfMissing(config, "appearance.display_name_format", "{name} &7#AFK",
                    """
                    How each player's name appears in the tab list.
                    Simple:    "{name} &7#AFK"
                    Status:    "{dimension} {name} {health} &7#AFK"
                    LuckPerms: "{prefix}{name}{suffix}&r &7#AFK" (set provider to "LP" above)
                    Placeholders: {name}, {rank}, {prefix}, {suffix}, {primary_group},
                    {dimension}, {health}, {world}, {ping}, {gamemode}, and #AFK.
                    Health includes absorption; 20 health points = 10 hearts. #AFK is empty when active.
                    """.stripTrailing());
            needsSave |= setDefaultIfMissing(config, "appearance.update_interval", 500,
                    """
                    Refresh delay in MILLISECONDS. 500 = twice per second; 1000 = once per second.
                    Range: 1-10000. Default: 500. Actual updates happen on server ticks (normally 50 ms).
                    """.stripTrailing());
            needsSave |= setDefaultIfMissing(config, "appearance.animation_interval", 4,
                    """
                    Number of refreshes before switching to the next header/footer frame.
                    With update_interval = 500 and animation_interval = 4, frames change every 2 seconds.
                    Range: 1-200. Default: 4. This does nothing when there is only one frame.
                    """.stripTrailing());

            // ── Sorting ──
            needsSave |= setDefaultIfMissing(config, "sorting.sort_mode", "NONE",
                    """
                    "NONE" = leave ordering alone. "ALPHABETICAL" = sort by player name.
                    "RANK" = highest FTB rank power or LuckPerms inherited group weight first,
                    then alphabetically. Select FTB or LP in name_formatting_provider above.
                    Players already on another scoreboard team stay on that team.
                    """.stripTrailing());

            // ── AFK ──
            needsSave |= setDefaultIfMissing(config, "afk.afk_enabled", true,
                    """
                    true = enable inactivity detection and grey out AFK player names.
                    Add #AFK to display_name_format to also show an AFK label.
                    Movement, turning, or chatting marks a player as active.
                    """.stripTrailing());
            needsSave |= setDefaultIfMissing(config, "afk.afk_timeout", 300,
                    "Inactivity timeout in SECONDS. 300 = 5 minutes. Range: 10-3600. Default: 300.");

            needsSave |= setDefaultIfMissing(config, "performance.metrics_provider", "VANILLA",
                    """
                    "VANILLA" = built-in estimated TPS, average MSPT, and JVM process CPU usage.
                    "SPARK" = use spark statistics when the spark mod is installed and ready.
                    Basic placeholders: #TPS, #CTPS, #MSPT, #CMSPT, #CPU, #CCPU.
                    Spark-only: #TPS_1M, #TPS_5M, #MSPT_P95; colored variants start with #C.
                    Missing spark falls back to vanilla. Unavailable rolling statistics show N/A.
                    """.stripTrailing());

            needsSave |= setCommentIfMissing(config, "appearance", """
                    Better TabList - config/tablist.toml
                    Edit this file, then run /tablist reload as an operator (permission level 2).
                    Quick setup: change server_name, choose a provider, then customize display_name_format.
                    Full guide: https://github.com/sennecools/Better-TabList#configuration

                    APPEARANCE - server branding, animated text, and player names
                    """.stripTrailing());
            needsSave |= setCommentIfMissing(config, "sorting", "PLAYER ORDER - optional alphabetical or rank sorting");
            needsSave |= setCommentIfMissing(config, "afk", "INACTIVITY - AFK detection and name appearance");
            needsSave |= setCommentIfMissing(config, "performance", "SERVER METRICS - built-in values or optional spark statistics");

            if (needsSave) {
                TomlWriter writer = new TomlWriter();
                writer.setIndent("    ");
                writer.setIndentArrayElementsPredicate(array -> !array.isEmpty());
                writer.write(config, configPath, WritingMode.REPLACE);
            }

            // Load runtime values
            serverName = config.getOrElse("appearance.server_name", "Your Server");
            headerFrames = List.copyOf(config.getOrElse("appearance.header", List.of("")));
            footerFrames = List.copyOf(config.getOrElse("appearance.footer", List.of("")));
            updateInterval = clamp(config.getOrElse("appearance.update_interval", 500), 1, 10000);
            animationInterval = clamp(config.getOrElse("appearance.animation_interval", 4), 1, 200);
            displayNameFormat = config.getOrElse("appearance.display_name_format", "{name} &7#AFK");
            String provider = config.getOrElse("appearance.name_formatting_provider", defaultProvider);
            nameFormattingProvider = NameFormattingProvider.fromString(provider);
            if (provider == null || !provider.trim().equalsIgnoreCase(nameFormattingProvider.name())) {
                Constants.LOGGER.warn("Unknown name_formatting_provider '{}'. Falling back to NONE.", provider);
            }
            sortMode = config.getOrElse("sorting.sort_mode", "NONE");
            afkEnabled = config.getOrElse("afk.afk_enabled", true);
            afkTimeout = clamp(config.getOrElse("afk.afk_timeout", 300), 10, 3600);
            String metricsProvider = config.getOrElse("performance.metrics_provider", "VANILLA");
            useSparkMetrics = "SPARK".equalsIgnoreCase(metricsProvider.trim());
            if (!useSparkMetrics && !"VANILLA".equalsIgnoreCase(metricsProvider.trim())) {
                Constants.LOGGER.warn("Unknown metrics_provider '{}'. Falling back to VANILLA.", metricsProvider);
            }

            Constants.LOGGER.info("TabList config loaded. Update interval: {} ms", updateInterval);
        }
    }

    private static <T> boolean setDefaultIfMissing(CommentedConfig config, String path, T defaultValue, String comment) {
        boolean changed = false;
        if (!config.contains(path)) {
            config.set(path, defaultValue);
            changed = true;
        }
        return setCommentIfMissing(config, path, comment) || changed;
    }

    private static boolean setCommentIfMissing(CommentedConfig config, String path, String comment) {
        if (config.getComment(path) != null) return false;
        // TomlWriter prefixes each line with '#'; add a space for readable '# comment' output.
        config.setComment(path, " " + comment.replace("\n", "\n "));
        return true;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
