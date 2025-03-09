package com.sennecools.tablist;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Updates the player's tab list periodically.
 * <p>
 * Checks the server status and updates the tab list header and footer only when they change.
 */
public class TabListUpdater {
    private String lastHeader = "";
    private String lastFooter = "";
    private int tickCounter = 0;

    /**
     * Called every server tick after game logic updates.
     * Updates the tab list for players if the header or footer has changed.
     *
     * @param event The post-tick event containing the server tick info.
     */
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null) return;

        tickCounter++;
        int updateTicks = Config.updateInterval / 50;
        if (tickCounter < updateTicks) {
            return;
        }
        tickCounter = 0;

        // Process the header and footer templates with current server data.
        String newHeader = TabListVariables.tablistChars(Config.templateHeader);
        String newFooter = TabListVariables.tablistChars(Config.templateFooter);

        // Only update if there is a change.
        if (!newHeader.equals(lastHeader) || !newFooter.equals(lastFooter)) {
            lastHeader = newHeader;
            lastFooter = newFooter;

            // Update each player's tab list.
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.connection.send(new ClientboundTabListPacket(Component.literal(newHeader), Component.literal(newFooter)));
            }
        }
    }
}
