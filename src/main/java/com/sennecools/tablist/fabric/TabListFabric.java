//? if fabric {
package com.sennecools.tablist.fabric;

import com.sennecools.tablist.TabListUpdater;
import com.sennecools.tablist.config.TabListConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

public class TabListFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        TabListConfig.load();
        TabListUpdater updater = new TabListUpdater();

        ServerTickEvents.END_SERVER_TICK.register(updater::onServerTick);

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            updater.onPlayerChat(sender);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            updater.onPlayerLogout(player);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(updater.buildReloadCommand());
        });
    }
}
//?}
