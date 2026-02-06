package com.sennecools.tablist;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.lang.management.ManagementFactory;
import java.util.regex.Pattern;

/**
 * Provides utility methods to process tab list templates by replacing placeholders
 * with real-time server data.
 */
public class TabListVariables {

    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("&([0-9a-fA-Fk-oK-OrR])");

    /**
     * Processes the tab list template and replaces placeholders with actual server data.
     *
     * @param template The raw template string containing placeholders.
     * @return The processed string with placeholders replaced and color codes converted.
     */
    public static String tablistChars(String template, ServerPlayer player) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || template == null) return "";

        String output = template;

        // Only compute and replace placeholders that are actually present in the template.
        if (output.contains("#TPS") || output.contains("#MSPT")) {
            double mspt = getMSPT(server);
            if (output.contains("#MSPT")) {
                output = output.replace("#MSPT", String.format("%.1f", mspt));
            }
            if (output.contains("#TPS")) {
                double tps = mspt == 0.0 ? 20.0 : Math.min(1000.0 / mspt, 20.0);
                output = output.replace("#TPS", String.format("%.1f", tps));
            }
        }
        if (output.contains("#PLAYERCOUNT")) {
            output = output.replace("#PLAYERCOUNT", String.valueOf(getPlayerCount(server)));
        }
        if (output.contains("#MEMORY")) {
            output = output.replace("#MEMORY", getMemoryUsage());
        }
        if (output.contains("#UPTIME")) {
            output = output.replace("#UPTIME", getServerUptime());
        }
        if (output.contains("#PING")) {
            output = output.replace("#PING", String.valueOf(getPlayerPing(player)));
        }
        if (output.contains("#RANK")) {
            output = output.replace("#RANK", getPlayerRank(player));
        }
        output = output.replace("#N", "\n");

        return convertColorCodes(output);
    }

    /**
     * Resolves the display name for a player in the tab list.
     * <p>
     * If {@code enableFTBRanksFormatting} is true and FTB Ranks is loaded, uses the
     * {@code ftbranks.name_format} permission. Otherwise, uses the configured
     * {@code displayNameFormat} template with {@code {name}} and {@code {rank}} placeholders.
     */
    public static String resolveDisplayName(ServerPlayer player) {
        if (Config.enableFTBRanksFormatting && isFTBRanksLoaded()) {
            String formatted = FTBRanksIntegration.getFormattedDisplayName(player);
            if (formatted != null) {
                return convertColorCodes(formatted);
            }
        }

        String format = Config.displayNameFormat;
        if (format == null) {
            format = "{name}";
        }
        String result = format.replace("{name}", player.getGameProfile().getName());
        result = result.replace("{rank}", getPlayerRank(player));
        return convertColorCodes(result);
    }

    private static String getPlayerRank(ServerPlayer player) {
        if (isFTBRanksLoaded()) {
            return FTBRanksIntegration.getPlayerRankName(player);
        }
        return "";
    }

    private static boolean isFTBRanksLoaded() {
        return ModList.get().isLoaded("ftbranks");
    }

    private static double getMSPT(MinecraftServer server) {
        return server.getAverageTickTimeNanos() / 1_000_000.0;
    }

    /**
     * Retrieves the current number of players on the server.
     *
     * @param server The current Minecraft server instance.
     * @return The player count.
     */
    private static int getPlayerCount(MinecraftServer server) {
        return server.getPlayerList().getPlayerCount();
    }

    /**
     * Retrieves the current memory usage of the server.
     *
     * @return A string representing used and maximum memory in megabytes.
     */
    private static String getMemoryUsage() {
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long maxMemory = Runtime.getRuntime().maxMemory();
        return String.format("%.1f MB / %.1f MB", usedMemory / 1048576.0, maxMemory / 1048576.0);
    }

    /**
     * Retrieves the server uptime in a human-readable format.
     *
     * @return A string representing the server uptime.
     */
    private static String getServerUptime() {
        long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        long seconds = (uptimeMillis / 1000) % 60;
        long minutes = (uptimeMillis / (1000 * 60)) % 60;
        long hours = (uptimeMillis / (1000 * 60 * 60)) % 24;
        long days = uptimeMillis / (1000 * 60 * 60 * 24);
        return days > 0
                ? String.format("%d days %02d:%02d:%02d", days, hours, minutes, seconds)
                : String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Retrieves the ping (latency) of a player.
     *
     * @param player The player whose ping is to be retrieved.
     * @return The player's ping in milliseconds.
     */
    private static int getPlayerPing(ServerPlayer player) {
        return player.connection.latency();
    }

    /**
     * Converts Minecraft-style color codes (using '&') to the section symbol '§'.
     *
     * @param text The text with color codes using '&'.
     * @return The text with color codes replaced.
     */
    static String convertColorCodes(String text) {
        return COLOR_CODE_PATTERN.matcher(text).replaceAll("§$1");
    }
}
