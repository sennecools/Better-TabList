package com.sennecools.tablist;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Main mod class for TabList.
 * <p>
 * Registers the configuration and event listeners during initialization.
 */
@Mod("tablist")
public class TabList {
    public static final String MOD_ID = "tablist";

    public static final Logger LOGGER = LogUtils.getLogger();

    public TabList(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modContainer.registerConfig(ModConfig.Type.COMMON, (IConfigSpec) Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        NeoForge.EVENT_BUS.register(new TabListUpdater());
    }
}
