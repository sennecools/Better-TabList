package com.sennecools.tablist;

import com.sennecools.tablist.config.TabListConfig;
import com.sennecools.tablist.platform.Services;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TabListVariables {

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("&([0-9a-fA-Fk-oK-OrR])");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static String tablistChars(String template, ServerPlayer player) {
        //? if >=1.21.9 {
        /*MinecraftServer server = player.level().getServer();*/
        //?} else {
        MinecraftServer server = player.getServer();
        //?}
        if (server == null || template == null) return "";

        String output = template;

        if (output.contains("#SERVERNAME")) {
            String name = TabListConfig.serverName != null ? TabListConfig.serverName : "";
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
            //? if >=1.21.9 {
            /*output = output.replace("#PLAYERNAME", player.getGameProfile().name());*/
            //?} else {
            output = output.replace("#PLAYERNAME", player.getGameProfile().getName());
            //?}
        }
        if (output.contains("#WORLD")) {
            //? if >=1.21.11 {
            /*output = output.replace("#WORLD", player.level().dimension().identifier().getPath());*/
            //?} else {
            output = output.replace("#WORLD", player.level().dimension().location().getPath());
            //?}
        }
        if (output.contains("#AFK")) {
            boolean afk = TabListConfig.afkEnabled
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

    public static String resolveDisplayName(ServerPlayer player) {
        String displayName = buildDefaultDisplayName(player);

        if (TabListConfig.afkEnabled
                && TabListUpdater.INSTANCE != null
                && TabListUpdater.INSTANCE.isPlayerAFK(player)) {
            displayName = "\u00A77" + displayName.replaceAll("\u00A7[0-9a-fA-Fk-oK-OrRxX]", "");
        }

        return displayName;
    }

    private static String buildDefaultDisplayName(ServerPlayer player) {
        if (TabListConfig.enableFTBRanksFormatting && isFTBRanksLoaded()) {
            String formatted = FTBRanksIntegration.getFormattedDisplayName(player);
            if (formatted != null) {
                return convertColorCodes(formatted);
            }
        }

        String format = TabListConfig.displayNameFormat;
        if (format == null) {
            format = "{name}";
        }
        //? if >=1.21.9 {
        /*String result = format.replace("{name}", player.getGameProfile().name());*/
        //?} else {
        String result = format.replace("{name}", player.getGameProfile().getName());
        //?}
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
        return Services.PLATFORM.isModLoaded("ftbranks");
    }

    private static double getMSPT(MinecraftServer server) {
        return server.getAverageTickTimeNanos() / 1_000_000.0;
    }

    private static int getPlayerCount(MinecraftServer server) {
        return server.getPlayerList().getPlayerCount();
    }

    private static String getMemoryUsage() {
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long maxMemory = Runtime.getRuntime().maxMemory();
        return String.format("%.1f MB / %.1f MB", usedMemory / 1048576.0, maxMemory / 1048576.0);
    }

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

    private static int getPlayerPing(ServerPlayer player) {
        return player.connection.latency();
    }

    static String convertColorCodes(String text) {
        Matcher hexMatcher = HEX_COLOR_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1);
            StringBuilder replacement = new StringBuilder("\u00A7x");
            for (char c : hex.toCharArray()) {
                replacement.append('\u00A7').append(c);
            }
            hexMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement.toString()));
        }
        hexMatcher.appendTail(sb);

        return COLOR_CODE_PATTERN.matcher(sb.toString()).replaceAll("\u00A7$1");
    }
}
