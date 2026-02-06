package com.sennecools.tablist;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Manages the configuration for the TabList mod.
 * <p>
 * This class defines the configuration values for the header, footer, and update interval.
 * It also provides a mechanism to reload these values when the config changes.
 */
@EventBusSubscriber(modid = TabList.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Define configuration category with placeholder explanations.
    static {
        BUILDER.comment("""
                Placeholders for TabList mod:
                  #N         - New line
                  #TPS       - Ticks per second
                  #MSPT      - Milliseconds per tick
                  #PLAYERCOUNT - Number of players online
                  #MEMORY    - Memory usage
                  #UPTIME   - Server uptime
                  #RANK      - Player rank (requires FTB Ranks)""");
        BUILDER.push("TabList");
    }

    // Configuration values for header, footer, and update interval.
    private static final ModConfigSpec.ConfigValue<String> HEADER = BUILDER.define("header", "#N             &c&lYOUR SERVER NAME           #N&a&l&m    #N");
    private static final ModConfigSpec.ConfigValue<String> FOOTER = BUILDER.define("footer", "&fOnline: &e#PLAYERCOUNT #N&7| TPS: &a#TPS &7 MSPT: &a#MSPT &7 | #NMemory: &b#MEMORY &7| Uptime: &d#UPTIME");
    private static final ModConfigSpec.IntValue UPDATE_INTERVAL = BUILDER.defineInRange("update_interval", 500, 1, 10000);

    private static final ModConfigSpec.ConfigValue<String> DISPLAY_NAME_FORMAT = BUILDER
            .comment("Display name format for the tab list. Supports {name} and {rank} placeholders + & color codes.",
                     "Example: \"&c[{rank}] &f{name}\"")
            .define("display_name_format", "{name}");

    private static final ModConfigSpec.BooleanValue ENABLE_FTBRANKS_FORMATTING = BUILDER
            .comment("When true and FTB Ranks is loaded, uses the ftbranks.name_format permission",
                     "instead of the display_name_format config template.")
            .define("enable_ftbranks_formatting", true);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    // Raw template values loaded from the configuration (unmodified).
    public static String templateHeader;
    public static String templateFooter;
    public static int updateInterval;
    public static String displayNameFormat;
    public static boolean enableFTBRanksFormatting;

    /**
     * Reloads the configuration values when the mod configuration is loaded or reloaded.
     *
     * @param event The configuration event.
     */
    @SubscribeEvent
    static void onLoad(ModConfigEvent event) {
        templateHeader = HEADER.get();
        templateFooter = FOOTER.get();
        updateInterval = UPDATE_INTERVAL.get();
        displayNameFormat = DISPLAY_NAME_FORMAT.get();
        enableFTBRanksFormatting = ENABLE_FTBRANKS_FORMATTING.get();
        TabList.LOGGER.info("TabList config reloaded. Update interval: {} ms", updateInterval);
    }
}
