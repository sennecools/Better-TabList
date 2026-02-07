//? if neoforge {
/*package com.sennecools.tablist.neoforge;

import com.sennecools.tablist.platform.PlatformHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public class NeoForgePlatformHelper implements PlatformHelper {

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public void refreshDisplayName(ServerPlayer player) {
        player.refreshTabListName();
    }
}
*///?}
