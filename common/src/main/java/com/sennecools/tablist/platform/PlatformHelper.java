package com.sennecools.tablist.platform;

import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public interface PlatformHelper {

    boolean isModLoaded(String modId);

    Path getConfigDir();

    void refreshDisplayName(ServerPlayer player);
}
