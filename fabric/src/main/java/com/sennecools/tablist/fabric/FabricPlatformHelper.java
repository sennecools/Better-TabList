package com.sennecools.tablist.fabric;

import com.sennecools.tablist.platform.PlatformHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.EnumSet;

public class FabricPlatformHelper implements PlatformHelper {

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public void refreshDisplayName(ServerPlayer player) {
        if (player.getServer() == null) return;
        ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                java.util.List.of(player)
        );
        player.getServer().getPlayerList().broadcastAll(packet);
    }
}
