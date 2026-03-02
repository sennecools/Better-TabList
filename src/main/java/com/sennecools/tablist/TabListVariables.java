package com.sennecools.tablist;

import com.sennecools.tablist.config.TabListConfig;
import com.sennecools.tablist.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TabListVariables {

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("&([0-9a-fA-Fk-oK-OrR])");
    private static final Pattern GRADIENT_MINIMESSAGE_PATTERN = Pattern.compile("<gradient:(#[0-9a-fA-F]{6}(?::#[0-9a-fA-F]{6})+)>(.*?)</gradient>");
    private static final Pattern GRADIENT_TAB_PATTERN = Pattern.compile("<(#[0-9a-fA-F]{6})>(.*?)</(#[0-9a-fA-F]{6})>");
    private static final Pattern HEX_CODE_IN_TEXT_PATTERN = Pattern.compile("&x(&[0-9a-fA-F]){6}");
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
        text = processGradients(text);

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

    /**
     * Parses a string with legacy color codes (§X and §x§R§R§G§G§B§B) and returns a proper Component.
     * This is necessary because Component.literal() does NOT parse formatting codes.
     */
    public static Component parseColoredText(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        MutableComponent result = Component.empty();
        StringBuilder currentText = new StringBuilder();
        Style currentStyle = Style.EMPTY;

        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);

            if (c == '\u00A7' && i + 1 < text.length()) {
                // Append any accumulated text with current style
                if (currentText.length() > 0) {
                    result.append(Component.literal(currentText.toString()).withStyle(currentStyle));
                    currentText = new StringBuilder();
                }

                char code = text.charAt(i + 1);

                // Check for hex color: §x§R§R§G§G§B§B
                if ((code == 'x' || code == 'X') && i + 13 < text.length()) {
                    StringBuilder hexBuilder = new StringBuilder();
                    boolean validHex = true;
                    for (int j = 0; j < 6; j++) {
                        int idx = i + 2 + (j * 2);
                        if (idx + 1 < text.length() && text.charAt(idx) == '\u00A7') {
                            char hexChar = text.charAt(idx + 1);
                            if (isHexChar(hexChar)) {
                                hexBuilder.append(hexChar);
                            } else {
                                validHex = false;
                                break;
                            }
                        } else {
                            validHex = false;
                            break;
                        }
                    }

                    if (validHex && hexBuilder.length() == 6) {
                        int rgb = Integer.parseInt(hexBuilder.toString(), 16);
                        currentStyle = currentStyle.withColor(TextColor.fromRgb(rgb));
                        i += 14; // Skip §x + 6 * (§ + hex char)
                        continue;
                    }
                }

                // Standard color codes
                ChatFormatting formatting = getFormatting(code);
                if (formatting != null) {
                    if (formatting.isColor()) {
                        // Reset formatting modifiers when applying a new color
                        currentStyle = Style.EMPTY.withColor(formatting);
                    } else if (formatting == ChatFormatting.RESET) {
                        currentStyle = Style.EMPTY;
                    } else {
                        // Apply formatting modifier (bold, italic, etc.)
                        currentStyle = applyFormatting(currentStyle, formatting);
                    }
                    i += 2;
                    continue;
                }
            }

            currentText.append(c);
            i++;
        }

        // Append any remaining text
        if (currentText.length() > 0) {
            result.append(Component.literal(currentText.toString()).withStyle(currentStyle));
        }

        return result;
    }

    private static boolean isHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static ChatFormatting getFormatting(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> ChatFormatting.BLACK;
            case '1' -> ChatFormatting.DARK_BLUE;
            case '2' -> ChatFormatting.DARK_GREEN;
            case '3' -> ChatFormatting.DARK_AQUA;
            case '4' -> ChatFormatting.DARK_RED;
            case '5' -> ChatFormatting.DARK_PURPLE;
            case '6' -> ChatFormatting.GOLD;
            case '7' -> ChatFormatting.GRAY;
            case '8' -> ChatFormatting.DARK_GRAY;
            case '9' -> ChatFormatting.BLUE;
            case 'a' -> ChatFormatting.GREEN;
            case 'b' -> ChatFormatting.AQUA;
            case 'c' -> ChatFormatting.RED;
            case 'd' -> ChatFormatting.LIGHT_PURPLE;
            case 'e' -> ChatFormatting.YELLOW;
            case 'f' -> ChatFormatting.WHITE;
            case 'k' -> ChatFormatting.OBFUSCATED;
            case 'l' -> ChatFormatting.BOLD;
            case 'm' -> ChatFormatting.STRIKETHROUGH;
            case 'n' -> ChatFormatting.UNDERLINE;
            case 'o' -> ChatFormatting.ITALIC;
            case 'r' -> ChatFormatting.RESET;
            default -> null;
        };
    }

    private static Style applyFormatting(Style style, ChatFormatting formatting) {
        return switch (formatting) {
            case BOLD -> style.withBold(true);
            case ITALIC -> style.withItalic(true);
            case UNDERLINE -> style.withUnderlined(true);
            case STRIKETHROUGH -> style.withStrikethrough(true);
            case OBFUSCATED -> style.withObfuscated(true);
            default -> style;
        };
    }

    private static String processGradients(String text) {
        // Process MiniMessage-style gradients first: <gradient:#FF0000:#0000FF>text</gradient>
        Matcher miniMatcher = GRADIENT_MINIMESSAGE_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (miniMatcher.find()) {
            String colorsStr = miniMatcher.group(1);
            String innerText = miniMatcher.group(2);
            String[] colorHexes = colorsStr.split(":");
            List<int[]> stops = new ArrayList<>();
            for (String hex : colorHexes) {
                stops.add(parseHexColor(hex));
            }
            miniMatcher.appendReplacement(sb, Matcher.quoteReplacement(applyGradient(innerText, stops)));
        }
        miniMatcher.appendTail(sb);
        text = sb.toString();

        // Process TAB-style gradients: <#FF0000>text</#0000FF>
        Matcher tabMatcher = GRADIENT_TAB_PATTERN.matcher(text);
        sb = new StringBuilder();
        while (tabMatcher.find()) {
            String startHex = tabMatcher.group(1);
            String innerText = tabMatcher.group(2);
            String endHex = tabMatcher.group(3);
            List<int[]> stops = new ArrayList<>();
            stops.add(parseHexColor(startHex));
            stops.add(parseHexColor(endHex));
            tabMatcher.appendReplacement(sb, Matcher.quoteReplacement(applyGradient(innerText, stops)));
        }
        tabMatcher.appendTail(sb);

        return sb.toString();
    }

    private static int[] parseHexColor(String hex) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        return new int[]{
                Integer.parseInt(hex.substring(0, 2), 16),
                Integer.parseInt(hex.substring(2, 4), 16),
                Integer.parseInt(hex.substring(4, 6), 16)
        };
    }

    private static String applyGradient(String innerText, List<int[]> stops) {
        String stripped = HEX_CODE_IN_TEXT_PATTERN.matcher(innerText).replaceAll("");

        List<Character> visibleChars = new ArrayList<>();
        List<String> formattingBefore = new ArrayList<>();
        StringBuilder currentFormatting = new StringBuilder();
        String activeFormatting = "";

        for (int i = 0; i < stripped.length(); i++) {
            if (stripped.charAt(i) == '&' && i + 1 < stripped.length()) {
                char code = stripped.charAt(i + 1);
                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f') || (code >= 'A' && code <= 'F')
                        || (code >= 'k' && code <= 'o') || (code >= 'K' && code <= 'O')
                        || code == 'r' || code == 'R') {
                    if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')
                            || (code >= 'A' && code <= 'F') || code == 'r' || code == 'R') {
                        activeFormatting = "";
                    } else {
                        activeFormatting += "&" + code;
                    }
                    currentFormatting.append("&").append(code);
                    i++;
                    continue;
                }
            }
            visibleChars.add(stripped.charAt(i));
            formattingBefore.add(currentFormatting.toString());
            currentFormatting = new StringBuilder();
        }

        int charCount = visibleChars.size();
        if (charCount == 0) {
            return stripped;
        }

        List<String> activeFormattingAtChar = new ArrayList<>();
        String runningFormatting = "";
        for (int i = 0; i < stripped.length(); i++) {
            if (stripped.charAt(i) == '&' && i + 1 < stripped.length()) {
                char code = stripped.charAt(i + 1);
                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f') || (code >= 'A' && code <= 'F')
                        || code == 'r' || code == 'R') {
                    runningFormatting = "";
                    i++;
                    continue;
                } else if ((code >= 'k' && code <= 'o') || (code >= 'K' && code <= 'O')) {
                    runningFormatting += "\u00A7" + code;
                    i++;
                    continue;
                }
            }
            activeFormattingAtChar.add(runningFormatting);
        }

        StringBuilder result = new StringBuilder();
        int segments = stops.size() - 1;

        for (int i = 0; i < charCount; i++) {
            double t = charCount == 1 ? 0.0 : (double) i / (charCount - 1);

            double segmentPos = t * segments;
            int segIndex = Math.min((int) segmentPos, segments - 1);
            double localT = segmentPos - segIndex;

            int[] startColor = stops.get(segIndex);
            int[] endColor = stops.get(segIndex + 1);

            int r = (int) Math.round(startColor[0] + (endColor[0] - startColor[0]) * localT);
            int g = (int) Math.round(startColor[1] + (endColor[1] - startColor[1]) * localT);
            int b = (int) Math.round(startColor[2] + (endColor[2] - startColor[2]) * localT);

            r = Math.max(0, Math.min(255, r));
            g = Math.max(0, Math.min(255, g));
            b = Math.max(0, Math.min(255, b));

            String hexColor = String.format("\u00A7x\u00A7%x\u00A7%x\u00A7%x\u00A7%x\u00A7%x\u00A7%x",
                    (r >> 4) & 0xF, r & 0xF,
                    (g >> 4) & 0xF, g & 0xF,
                    (b >> 4) & 0xF, b & 0xF);

            result.append(hexColor);

            if (i < activeFormattingAtChar.size()) {
                result.append(activeFormattingAtChar.get(i));
            }

            result.append(visibleChars.get(i));
        }

        return result.toString();
    }
}
