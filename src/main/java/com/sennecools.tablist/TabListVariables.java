package com.sennecools.tablist;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides utility methods to process tab list templates by replacing placeholders
 * with real-time server data.
 */
public class TabListVariables {

    /**
     * Processes the tab list template and replaces placeholders with actual server data.
     *
     * @param template The raw template string containing placeholders.
     * @return The processed string with placeholders replaced and color codes converted.
     */
    public static String tablistChars(String template, ServerPlayer player) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || template == null) return "";

        // Start with the raw template and define placeholders for replacement.
        String output = template;
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("#TPS", String.format("%.1f", getTPS(server)));
        placeholders.put("#MSPT", String.format("%.1f", getMSPT(server)));
        placeholders.put("#PLAYERCOUNT", String.valueOf(getPlayerCount(server)));
        placeholders.put("#MEMORY", getMemoryUsage());
        placeholders.put("#UPTIME", getServerUptime());
        placeholders.put("#PING", String.valueOf(getPlayerPing(player)));
        placeholders.put("#N", "\n");

        // Replace each placeholder in the template with its corresponding value.
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            output = output.replace(entry.getKey(), entry.getValue());
        }

        return convertColorCodes(output);
    }

    /**
     * Calculates the ticks per second (TPS) based on the server's average tick time.
     *
     * @param server The current Minecraft server instance.
     * @return The TPS value.
     */
    private static double getTPS(MinecraftServer server) {
        double mspt = getMSPT(server);
        return mspt == 0.0 ? 20.0 : Math.round(Math.min(1000.0 / mspt, 20.0) * 10.0) / 10.0;
    }

    /**
     * Calculates the milliseconds per tick (MSPT) for the server.
     *
     * @param server The current Minecraft server instance.
     * @return The MSPT value.
     */
    private static double getMSPT(MinecraftServer server) {
        return Math.round(server.getAverageTickTimeNanos() / 100000.0) / 10.0;
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
    private static String convertColorCodes(String text) {
        return text.replace("&", "§");
    }
}
