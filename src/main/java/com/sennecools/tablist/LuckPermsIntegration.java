package com.sennecools.tablist;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.UUID;

/** Optional LuckPerms integration, adapted from MihailDenisov's LPintegration branch. */
public final class LuckPermsIntegration {

    private static final PlayerMetaData EMPTY_META_DATA = new PlayerMetaData("", "", "");

    private LuckPermsIntegration() {
    }

    private static User getUser(UUID playerId) {
        try {
            // Online users are already loaded by LuckPerms; never block a server tick on storage.
            return LuckPermsProvider.get().getUserManager().getUser(playerId);
        } catch (IllegalStateException exception) {
            return null;
        }
    }

    public static PlayerMetaData getPlayerMetaData(ServerPlayer player) {
        return getPlayerMetaData(player.getUUID());
    }

    static PlayerMetaData getPlayerMetaData(UUID playerId) {
        User user = getUser(playerId);
        if (user == null) return EMPTY_META_DATA;

        CachedMetaData metaData = user.getCachedData().getMetaData(user.getQueryOptions());
        return new PlayerMetaData(
                Objects.requireNonNullElse(metaData.getPrefix(), ""),
                Objects.requireNonNullElse(metaData.getSuffix(), ""),
                user.getPrimaryGroup()
        );
    }

    public static int getGroupWeight(ServerPlayer player) {
        return getGroupWeight(player.getUUID());
    }

    static int getGroupWeight(UUID playerId) {
        User user = getUser(playerId);
        if (user == null) return 0;

        return user.getInheritedGroups(user.getQueryOptions()).stream()
                .map(Group::getWeight)
                .filter(weight -> weight.isPresent())
                .mapToInt(weight -> weight.getAsInt())
                .max()
                .orElse(0);
    }

    public record PlayerMetaData(String prefix, String suffix, String primaryGroup) {
    }
}
