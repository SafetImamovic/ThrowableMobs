package io.github.safetimamovic.throwablemobs.behavior.builtin;

import io.github.safetimamovic.throwablemobs.api.ThrowableMobsApi;

/** Our own behavior types, registered through the same API other mods use. */
public final class BuiltinBehaviors
{
        private BuiltinBehaviors()
        {
        }

        public static void register()
        {
                ThrowableMobsApi.registerBehaviorType(ExplodeAction.TYPE);
                ThrowableMobsApi.registerBehaviorType(TeleportRandomAction.TYPE);
                ThrowableMobsApi.registerBehaviorType(AngerThrowerAction.TYPE);
                ThrowableMobsApi.registerBehaviorType(SplitAction.TYPE);
                ThrowableMobsApi.registerBehaviorType(IgniteAction.TYPE);
                ThrowableMobsApi.registerBehaviorType(DropItemAction.TYPE);
                ThrowableMobsApi.registerBehaviorType(ParticlesAction.TYPE);
                ThrowableMobsApi.registerBehaviorType(RadarAction.TYPE);
        }
}
