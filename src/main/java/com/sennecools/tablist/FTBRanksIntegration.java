package com.sennecools.tablist;

import dev.ftb.mods.ftbranks.api.FTBRanksAPI;
import dev.ftb.mods.ftbranks.api.PermissionValue;
import dev.ftb.mods.ftbranks.api.Rank;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Isolated helper for FTB Ranks integration.
 * This is the only class that imports FTB Ranks types, so it is never loaded
 * unless FTB Ranks is present on the classpath.
 */
public class FTBRanksIntegration {

    /**
     * Returns the name of the player's highest-power active rank,
     * or an empty string if no non-default rank is found.
     */
    public static String getPlayerRankName(ServerPlayer player) {
        List<Rank> ranks = FTBRanksAPI.manager().getRanks(player);
        if (!ranks.isEmpty()) {
            return ranks.getFirst().getName();
        }
        return "";
    }

    /**
     * Reads the {@code ftbranks.name_format} permission for the player and
     * replaces {@code {name}} with the player's real name.
     *
     * @return the formatted display name, or {@code null} if the permission is not set
     */
    public static String getFormattedDisplayName(ServerPlayer player) {
        PermissionValue value = FTBRanksAPI.getPermissionValue(player, "ftbranks.name_format");
        if (value.isEmpty()) {
            return null;
        }
        return value.asString()
                .map(format -> format.replace("{name}", player.getGameProfile().getName()))
                .orElse(null);
    }
}
