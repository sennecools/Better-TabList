package com.sennecools.tablist.forge;

import com.sennecools.tablist.Constants;
import com.sennecools.tablist.TabListUpdater;
import com.sennecools.tablist.TabListVariables;
import com.sennecools.tablist.config.TabListConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class TabListForge {

    private final TabListUpdater updater;

    public TabListForge() {
        TabListConfig.load();
        updater = new TabListUpdater();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.getServer() != null) {
            updater.onServerTick(event.getServer());
        }
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
}
