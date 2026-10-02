package io.github.safetimamovic.throwablemobs;

import net.fabricmc.api.ModInitializer;

public class ThrowableMobsFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        ThrowableMobs.init();
    }
}
