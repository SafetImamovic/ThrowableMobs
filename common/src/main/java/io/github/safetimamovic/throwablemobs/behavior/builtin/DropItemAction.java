package io.github.safetimamovic.throwablemobs.behavior.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.api.BehaviorAction;
import io.github.safetimamovic.throwablemobs.api.BehaviorType;
import io.github.safetimamovic.throwablemobs.api.ThrowContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** {@code throwablemobs:drop_item}: drops {@code count} of {@code item} at the mob, like a chicken laying an egg. */
public record DropItemAction(Item item, int count) implements BehaviorAction
{
        public static final MapCodec<DropItemAction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(DropItemAction::item),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(DropItemAction::count))
                .apply(instance, DropItemAction::new));

        public static final BehaviorType<DropItemAction> TYPE = new BehaviorType<>(Constants.id("drop_item"), CODEC);

        @Override
        public BehaviorType<?> type()
        {
                return TYPE;
        }

        @Override
        public void run(ThrowContext ctx)
        {
                //? if >=26.1 {
                ctx.mob().spawnAtLocation(ctx.level(), new ItemStack(item, count));
                //?} else
                /*ctx.mob().spawnAtLocation(new ItemStack(item, count));*/
        }
}
