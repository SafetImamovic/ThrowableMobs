package io.github.safetimamovic.throwablemobs;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class ThrowableMobsNeoForge {

    public ThrowableMobsNeoForge(IEventBus modEventBus) {
        ThrowableMobs.init();
    }
}
