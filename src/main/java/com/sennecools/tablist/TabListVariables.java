package com.sennecools.tablist;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides utility methods to process tab list templates by replacing placeholders
 * with real-time server data.
 */
public class TabListVariables {

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("&([0-9a-fA-Fk-oK-OrR])");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

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
        if (output.contains("#SERVERNAME")) {
            String name = Config.serverName != null ? Config.serverName : "";
            output = output.replace("#SERVERNAME", name);
        }
        if (output.contains("#TPS") || output.contains("#MSPT") || output.contains("#CTPS")) {
            double mspt = getMSPT(server);
            double tps = mspt == 0.0 ? 20.0 : Math.min(1000.0 / mspt, 20.0);
            if (output.contains("#CTPS")) {
                String color;
                if (tps >= 18.0) {
                    color = "&a";
                } else if (tps >= 15.0) {
                    color = "&e";
                } else {
                    color = "&c";
                }
                output = output.replace("#CTPS", color + String.format("%.1f", tps));
            }
            if (output.contains("#MSPT")) {
                output = output.replace("#MSPT", String.format("%.1f", mspt));
            }
            if (output.contains("#TPS")) {
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
        if (output.contains("#MAXPLAYERS")) {
            output = output.replace("#MAXPLAYERS", String.valueOf(server.getMaxPlayers()));
        }
        if (output.contains("#PLAYERNAME")) {
            output = output.replace("#PLAYERNAME", player.getGameProfile().getName());
        }
        if (output.contains("#WORLD")) {
            output = output.replace("#WORLD", player.level().dimension().location().getPath());
        }
        if (output.contains("#AFK")) {
            boolean afk = Config.afkEnabled
                    && TabListUpdater.INSTANCE != null
                    && TabListUpdater.INSTANCE.isPlayerAFK(player);
            output = output.replace("#AFK", afk ? "AFK" : "");
        }
        if (output.contains("#DATE") || output.contains("#TIME")) {
            LocalDateTime now = LocalDateTime.now();
            if (output.contains("#DATE")) {
                output = output.replace("#DATE", now.format(DATE_FORMATTER));
            }
            if (output.contains("#TIME")) {
                output = output.replace("#TIME", now.format(TIME_FORMATTER));
            }
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
     * <p>
     * If the player is AFK (and AFK detection is enabled), the name is greyed out.
     */
    public static String resolveDisplayName(ServerPlayer player) {
        String displayName = buildDefaultDisplayName(player);

        if (Config.afkEnabled
                && TabListUpdater.INSTANCE != null
                && TabListUpdater.INSTANCE.isPlayerAFK(player)) {
            // Strip all existing §X color codes and prepend grey
            displayName = "§7" + displayName.replaceAll("§[0-9a-fA-Fk-oK-OrRxX]", "");
        }

        return displayName;
    }

    /**
     * Builds the display name from FTB Ranks formatting or the config template,
     * with color codes already converted.
     */
    private static String buildDefaultDisplayName(ServerPlayer player) {
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

    static int getPlayerRankPower(ServerPlayer player) {
        if (isFTBRanksLoaded()) {
            return FTBRanksIntegration.getPlayerRankPower(player);
        }
        return 0;
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
     * Converts Minecraft-style color codes to the section symbol '§'.
     * <p>
     * First processes hex colors ({@code &#RRGGBB} → {@code §x§R§R§G§G§B§B}),
     * then standard single-char codes ({@code &X} → {@code §X}).
     *
     * @param text The text with color codes using '&'.
     * @return The text with color codes replaced.
     */
    static String convertColorCodes(String text) {
        // First pass: hex colors &#RRGGBB → §x§R§R§G§G§B§B
        Matcher hexMatcher = HEX_COLOR_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            hexMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement.toString()));
        }
        hexMatcher.appendTail(sb);

        // Second pass: standard &X → §X
        return COLOR_CODE_PATTERN.matcher(sb.toString()).replaceAll("§$1");
    }
}
