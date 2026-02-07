package com.sennecools.tablist.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
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
    public static boolean enableFTBRanksFormatting;
    public static String sortMode;
    public static boolean afkEnabled;
    public static int afkTimeout;

    public static void load() {
        Path configPath = Services.PLATFORM.getConfigDir().resolve("tablist.toml");

        try (CommentedFileConfig config = CommentedFileConfig.builder(configPath)
                .autosave()
                .preserveInsertionOrder()
                .build()) {

            config.load();

            boolean needsSave = false;

            // ── Appearance ──
            needsSave |= setDefaultIfMissing(config, "appearance.server_name", "Your Server",
                    "Your server's name. Use #SERVERNAME in header/footer to insert it.");
            needsSave |= setDefaultIfMissing(config, "appearance.header", List.of(
                    "#N        &#FF5555&l#SERVERNAME        #N&#AAAAAA&m            #N",
                    "#N        &#5555FF&l#SERVERNAME        #N&#AAAAAA&m            #N"
            ), "Text shown above the player list. Multiple entries create animation frames.");
            needsSave |= setDefaultIfMissing(config, "appearance.footer", List.of(
                    "&fOnline: &e#PLAYERCOUNT&7/&e#MAXPLAYERS #N&7TPS: #CTPS &7| MSPT: &#55FFFF#MSPT #N&7Memory: &#AA55FF#MEMORY &7| Uptime: &#FFAA00#UPTIME",
                    "&fOnline: &e#PLAYERCOUNT&7/&e#MAXPLAYERS #N&7TPS: #CTPS &7| Ping: &#55FFFF#PING&7ms #N&7Memory: &#AA55FF#MEMORY &7| Uptime: &#FFAA00#UPTIME"
            ), "Text shown below the player list. Multiple entries create animation frames.");
            needsSave |= setDefaultIfMissing(config, "appearance.display_name_format", "{name} &7#AFK",
                    "Display name format. Supports {name}, {rank} placeholders + & color codes.");
            needsSave |= setDefaultIfMissing(config, "appearance.update_interval", 500,
                    "How often (ms) the tab list refreshes. Range: 1-10000. Default: 500.");
            needsSave |= setDefaultIfMissing(config, "appearance.animation_interval", 4,
                    "Update cycles between animation frame changes. Range: 1-200. Default: 4.");

            // ── Sorting ──
            needsSave |= setDefaultIfMissing(config, "sorting.sort_mode", "NONE",
                    "How to sort players: NONE, ALPHABETICAL, or RANK.");

            // ── FTB Ranks ──
            needsSave |= setDefaultIfMissing(config, "ftbranks.enable_ftbranks_formatting", true,
                    "When true and FTB Ranks is loaded, uses ftbranks.name_format permission.");

            // ── AFK ──
            needsSave |= setDefaultIfMissing(config, "afk.afk_enabled", true,
                    "Enable AFK detection. AFK players have greyed-out names.");
            needsSave |= setDefaultIfMissing(config, "afk.afk_timeout", 300,
                    "Seconds of inactivity before AFK. Range: 10-3600. Default: 300.");

            if (needsSave) {
                config.save();
            }

            // Load runtime values
            serverName = config.getOrElse("appearance.server_name", "Your Server");
            headerFrames = List.copyOf(config.getOrElse("appearance.header", List.of("")));
            footerFrames = List.copyOf(config.getOrElse("appearance.footer", List.of("")));
            updateInterval = clamp(config.getOrElse("appearance.update_interval", 500), 1, 10000);
            animationInterval = clamp(config.getOrElse("appearance.animation_interval", 4), 1, 200);
            displayNameFormat = config.getOrElse("appearance.display_name_format", "{name} &7#AFK");
            enableFTBRanksFormatting = config.getOrElse("ftbranks.enable_ftbranks_formatting", true);
            sortMode = config.getOrElse("sorting.sort_mode", "NONE");
            afkEnabled = config.getOrElse("afk.afk_enabled", true);
            afkTimeout = clamp(config.getOrElse("afk.afk_timeout", 300), 10, 3600);

            Constants.LOGGER.info("TabList config loaded. Update interval: {} ms", updateInterval);
        }
    }

    private static <T> boolean setDefaultIfMissing(CommentedConfig config, String path, T defaultValue, String comment) {
        if (!config.contains(path)) {
            config.set(path, defaultValue);
            config.setComment(path, comment);
            return true;
        }
        return false;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
