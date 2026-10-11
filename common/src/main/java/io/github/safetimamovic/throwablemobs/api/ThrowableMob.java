package io.github.safetimamovic.throwablemobs.api;

import org.jspecify.annotations.Nullable;

/**
 * Implement on a mob class to give it its own behavior and physics without registering anything. A mob registered
 * with {@link ThrowableMobsApi} or given a datapack profile has those applied on top.
 */
public interface ThrowableMob
{
        @Nullable ThrowBehavior getThrowBehavior();

        default @Nullable MobProfile getThrowProfile()
        {
                return null;
        }
}
