package com.sennecools.tablist;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedDataManager;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.query.QueryOptions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LuckPermsIntegrationTest {

    private final UUID playerId = UUID.randomUUID();

    @Test
    void unregisteredProviderReturnsEmptyMetadataAndZeroWeight() {
        try (var provider = mockStatic(LuckPermsProvider.class)) {
            provider.when(LuckPermsProvider::get).thenThrow(new IllegalStateException());
            assertEquals(new LuckPermsIntegration.PlayerMetaData("", "", ""),
                    LuckPermsIntegration.getPlayerMetaData(playerId));
            assertEquals(0, LuckPermsIntegration.getGroupWeight(playerId));
        }
    }

    @Test
    void unloadedUserDoesNotTriggerStorageLoads() {
        LuckPerms api = mock(LuckPerms.class);
        UserManager users = mock(UserManager.class);
        when(api.getUserManager()).thenReturn(users);

        try (var provider = mockStatic(LuckPermsProvider.class)) {
            provider.when(LuckPermsProvider::get).thenReturn(api);
            assertEquals("", LuckPermsIntegration.getPlayerMetaData(playerId).prefix());
            assertEquals(0, LuckPermsIntegration.getGroupWeight(playerId));
            verify(users, times(2)).getUser(playerId);
            verifyNoMoreInteractions(users);
        }
    }

    @Test
    void readsContextualMetadataAndHighestInheritedWeight() {
        LuckPerms api = mock(LuckPerms.class);
        UserManager users = mock(UserManager.class);
        User user = mock(User.class);
        QueryOptions options = mock(QueryOptions.class);
        CachedDataManager cache = mock(CachedDataManager.class);
        CachedMetaData metadata = mock(CachedMetaData.class);
        Group unweighted = mock(Group.class);
        Group member = mock(Group.class);
        Group admin = mock(Group.class);
        when(api.getUserManager()).thenReturn(users);
        when(users.getUser(playerId)).thenReturn(user);
        when(user.getQueryOptions()).thenReturn(options);
        when(user.getCachedData()).thenReturn(cache);
        when(cache.getMetaData(options)).thenReturn(metadata);
        when(metadata.getPrefix()).thenReturn("&#FF5555[Admin] ");
        when(metadata.getSuffix()).thenReturn(null);
        when(user.getPrimaryGroup()).thenReturn("admin");
        when(user.getInheritedGroups(options)).thenReturn(List.of(unweighted, member, admin));
        when(unweighted.getWeight()).thenReturn(OptionalInt.empty());
        when(member.getWeight()).thenReturn(OptionalInt.of(10));
        when(admin.getWeight()).thenReturn(OptionalInt.of(100));

        try (var provider = mockStatic(LuckPermsProvider.class)) {
            provider.when(LuckPermsProvider::get).thenReturn(api);
            assertEquals(new LuckPermsIntegration.PlayerMetaData("&#FF5555[Admin] ", "", "admin"),
                    LuckPermsIntegration.getPlayerMetaData(playerId));
            assertEquals(100, LuckPermsIntegration.getGroupWeight(playerId));
            verify(cache).getMetaData(options);
            verify(user).getInheritedGroups(options);
        }
    }
}
