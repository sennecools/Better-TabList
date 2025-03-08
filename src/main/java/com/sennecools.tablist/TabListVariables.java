package com.sennecools.tablist;


import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class TabListVariables {
    public static String tablistChars(String string) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return string;
        String output = string.replaceAll("#TPS", String.valueOf(getTPS(server)));
        output = output.replaceAll("#MSPT", String.valueOf(getMSPT(server)));
        output = output.replaceAll("#PLAYERCOUNT", Integer.toString(getPlayerCount(server)));
        output = output.replaceAll("#N", "\n");
        return output.replaceAll("&", "§");
    }

    private static double getTPS(MinecraftServer server) {
        double mspt = getMSPT(server);
        if (mspt == 0.0D) return 20.0D;

        double tps = Math.min(1000.0D / mspt, 20.0D);
        return Math.round(tps * 10.0D) / 10.0D;
    }

    private static double getMSPT(MinecraftServer server) {
        return Math.round(server.getAverageTickTimeNanos() / 100000.0D) / 10.0D;
    }

    private static int getPlayerCount(MinecraftServer server) {
        return server.getPlayerList().getPlayerCount();
    }
}
