package com.sennecools.tablist;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TabListUpdater {

    private int ticksSinceLastUpdate = 0;
    private final Map<UUID, String> lastSentContent = new ConcurrentHashMap<>();

    /**
     * Called after each server tick to determine if it's time to refresh the tab list.
     *
     * @param event the server post-tick event
     */
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null) {
            return;
        }

        // Increment tick counter and check if update interval reached
        ticksSinceLastUpdate++;
        int updateIntervalTicks = Config.updateInterval / 50; // convert ms to ticks
        if (ticksSinceLastUpdate < updateIntervalTicks) {
            return;
        }
        // Reset counter for next cycle
        ticksSinceLastUpdate = 0;

        // Broadcast refresh to all connected players
        server.getPlayerList().getPlayers().forEach(this::refreshPlayerTab);
    }

    private void refreshPlayerTab(ServerPlayer player) {
        String header = TabListVariables.tablistChars(Config.templateHeader, player);
        String footer = TabListVariables.tablistChars(Config.templateFooter, player);

        String combined = header + "\0" + footer;
        String previous = lastSentContent.put(player.getUUID(), combined);
        if (combined.equals(previous)) {
            return;
        }

        ClientboundTabListPacket packet =
            new ClientboundTabListPacket(
                Component.literal(header),
                Component.literal(footer)
            );
        player.connection.send(packet);
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        lastSentContent.remove(event.getEntity().getUUID());
    }
}
