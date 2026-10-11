package io.github.safetimamovic.throwablemobs.api;

/**
 * One data-driven building block ("atom") such as {@code throwablemobs:explode}, decoded from a behavior in a mob
 * profile by its {@link BehaviorType}. The profile decides when it runs ({@link Trigger}) and under which conditions;
 * the action only does the work.
 */
public interface BehaviorAction
{
        BehaviorType<?> type();

        /** Runs for every trigger except impacts, and for impacts unless {@link #onImpact} is overridden. */
        default void run(ThrowContext ctx)
        {
        }

        /** Impact triggers. By default runs {@link #run} and leaves the impact unchanged. */
        default ImpactResult onImpact(ImpactContext ctx, ImpactResult current)
        {
                run(ctx);
                return current;
        }
}
