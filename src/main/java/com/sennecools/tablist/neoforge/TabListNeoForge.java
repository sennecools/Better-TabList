//? if neoforge {
/*package com.sennecools.tablist.neoforge;

import com.sennecools.tablist.Constants;
import com.sennecools.tablist.TabListUpdater;
import com.sennecools.tablist.TabListVariables;
import com.sennecools.tablist.config.TabListConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(Constants.MOD_ID)
public class TabListNeoForge {

    private final TabListUpdater updater;

    public TabListNeoForge() {
        TabListConfig.load();
        updater = new TabListUpdater();
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        updater.onServerTick(event.getServer());
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        updater.onPlayerChat(event.getPlayer());
    }

    @SubscribeEvent
    public void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            String displayName = TabListVariables.resolveDisplayName(player);
            event.setDisplayName(Component.literal(displayName));
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            updater.onPlayerLogout(player);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(updater.buildReloadCommand());
    }
}
*///?}
