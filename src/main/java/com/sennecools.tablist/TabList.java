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

/**
 * Main mod class for TabList.
 * <p>
 * Registers the configuration and event listeners during initialization.
 */
@Mod("tablist")
public class TabList {
    public static final String MOD_ID = "tablist";

    private static final Logger LOGGER = LogUtils.getLogger();

    public TabList(IEventBus modEventBus, ModContainer modContainer) {
        // Register the common setup event listener.
        modEventBus.addListener(this::commonSetup);
        // Register this mod to the global event bus.
        NeoForge.EVENT_BUS.register(this);
        // Register mod configuration.
        modContainer.registerConfig(ModConfig.Type.COMMON, (IConfigSpec) Config.SPEC);
    }

    /**
     * Called during the common setup phase.
     * Registers the TabListUpdater to handle tab list updates.
     *
     * @param event The common setup event.
     */
    private void commonSetup(FMLCommonSetupEvent event) {
        NeoForge.EVENT_BUS.register(new TabListUpdater());
    }

    /**
     * Handles the server starting event.
     * Currently, no additional actions are taken at server start.
     *
     * @param event The server starting event.
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // No action required on server start.
    }
}
