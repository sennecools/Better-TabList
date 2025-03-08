package com.sennecools.tablist;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class TabListUpdater {
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            MutableComponent mutableComponent1 = Component.literal(TabListVariables.tablistChars(Config.header));
            MutableComponent mutableComponent2 = Component.literal(TabListVariables.tablistChars(Config.footer));
            ClientboundTabListPacket packet = new ClientboundTabListPacket((Component)mutableComponent1, (Component)mutableComponent2);
            player.connection.send((Packet)packet);
        }
    }
}
