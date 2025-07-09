package com.sennecools.tablist;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Periodically updates the tab list header, footer, and player display names.
 * Sends an update to every online player at a fixed interval.
 * Designed for NeoForge 1.21.+
 */
public class TabListUpdater {

    /** Counter tracking ticks since last update. */
    private int ticksSinceLastUpdate = 0;

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

    /**
     * Refreshes the tab list header/footer and display name for a single player.
     *
     * @param player the player to update
     */
    private void refreshPlayerTab(ServerPlayer player) {
        updateTabListHeaderFooter(player);
        updatePlayerDisplayName(player);
    }

    /**
     * Sends a tab list packet with updated header and footer.
     *
     * @param player the player to send the packet to
     */
    private void updateTabListHeaderFooter(ServerPlayer player) {
        String header = TabListVariables.tablistChars(Config.templateHeader, player);
        String footer = TabListVariables.tablistChars(Config.templateFooter, player);
        ClientboundTabListPacket packet =
            new ClientboundTabListPacket(
                Component.literal(header),
                Component.literal(footer)
            );
        player.connection.send(packet);
    }

    /**
     * Updates the player's display name in the tab list with their rank prefix.
     *
     * @param player the player to update
     */
    private void updatePlayerDisplayName(ServerPlayer player) {
        String displayName = " " + player.getName().getString();
        player.setCustomName(Component.literal(displayName));
        ClientboundPlayerInfoUpdatePacket packet =
            new ClientboundPlayerInfoUpdatePacket(
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
                player
            );
        player.connection.send(packet);
    }
}
