package com.sennecools.tablist.platform;

import com.sennecools.tablist.Constants;

import java.util.ServiceLoader;

public class Services {

    public static final PlatformHelper PLATFORM = load(PlatformHelper.class);

    private static <T> T load(Class<T> clazz) {
        T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Constants.LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
