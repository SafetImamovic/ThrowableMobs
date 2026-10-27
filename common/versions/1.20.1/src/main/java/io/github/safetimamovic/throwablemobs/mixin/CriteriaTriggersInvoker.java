package io.github.safetimamovic.throwablemobs.mixin;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Registers our criterion: before 1.20.2 there is no trigger registry, only this private method. */
@Mixin(CriteriaTriggers.class)
public interface CriteriaTriggersInvoker
{
        @Invoker("register")
        static <T extends CriterionTrigger<?>> T invokeRegister(T trigger)
        {
                throw new AssertionError();
        }
}
