//? if fabric {
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
        //? if >=1.21.9 {
        /*if (player.level().getServer() == null) return;*/
        //?} else {
        if (player.getServer() == null) return;
        //?}
        ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                java.util.List.of(player)
        );
        //? if >=1.21.9 {
        /*player.level().getServer().getPlayerList().broadcastAll(packet);*/
        //?} else {
        player.getServer().getPlayerList().broadcastAll(packet);
        //?}
    }
}
//?}
