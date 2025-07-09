package com.sennecools.tablist;

            import net.minecraft.network.chat.Component;
            import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
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

                    // Iterate over all players currently on the server
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        // Generate new header and footer strings for the player
                        String newHeader = TabListVariables.tablistChars(Config.templateHeader, player);
                        String newFooter = TabListVariables.tablistChars(Config.templateFooter, player);

                        // Check if the new header or footer is different from the last updated values
                        if (!newHeader.equals(lastHeader) || !newFooter.equals(lastFooter)) {
                            // Update the last header and footer values
                            lastHeader = newHeader;
                            lastFooter = newFooter;
                            // Send the updated header and footer to the player
                            player.connection.send(new ClientboundTabListPacket(Component.literal(newHeader), Component.literal(newFooter)));
                        }

                        // Update the player's display name with their rank
                        String playerNameWithRank = TabListVariables.getPlayerRank(player) + " " + player.getName().getString();
                        player.setCustomName(Component.literal(playerNameWithRank));
                        player.connection.send(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, player));
                    }
                }
            }