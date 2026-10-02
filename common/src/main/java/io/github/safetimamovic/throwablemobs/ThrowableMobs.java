package io.github.safetimamovic.throwablemobs;

import io.github.safetimamovic.throwablemobs.platform.Services;

/**
 * Common entrypoint. Shared between all loaders, so it may only touch vanilla code and libraries
 * vanilla ships. Loader-specific behavior goes through {@link Services}.
 */
public final class ThrowableMobs {

    private ThrowableMobs() {
    }

    public static void init() {
        Constants.LOG.info("Throwable Mobs initializing on {} ({})",
                Services.PLATFORM.getPlatformName(), Services.PLATFORM.getEnvironmentName());
    }
}
