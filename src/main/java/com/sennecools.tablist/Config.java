package com.sennecools.tablist;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = "tablist", bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("#N next line\n#TPS show ticks per second\n#MSPT ms per tick\n#PLAYERCOUNT show how many players are online\n");
        BUILDER.push("TabList");
    }

    private static final ModConfigSpec.ConfigValue<String> HEADER = BUILDER.define("header", "#N             &a&lYOUR SERVER           #N&a&l&m    #N");

    private static final ModConfigSpec.ConfigValue<String> FOOTER = BUILDER.define("footer", "#N&f&e#PLAYERCOUNT#N&f&e#TPS &f| &e#MSPT#N");

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    public static String header;

    public static String footer;

    @SubscribeEvent
    static void onLoad(ModConfigEvent event) {
        header = (String)HEADER.get();
        footer = (String)FOOTER.get();
    }
}
