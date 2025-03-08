package com.sennecools.tablist;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

@Mod("tablist")
public class TabList {
    public static final String MOD_ID = "tablist";

    private static final Logger LOGGER = LogUtils.getLogger();

    public TabList(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, (IConfigSpec)Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        NeoForge.EVENT_BUS.register(new TabListUpdater());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {}
}
