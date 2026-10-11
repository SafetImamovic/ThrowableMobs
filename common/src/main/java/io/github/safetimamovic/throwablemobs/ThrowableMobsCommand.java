package io.github.safetimamovic.throwablemobs;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.safetimamovic.throwablemobs.api.MobProfile;
import io.github.safetimamovic.throwablemobs.api.ThrowBehavior;
import io.github.safetimamovic.throwablemobs.behavior.DataBehavior;
import io.github.safetimamovic.throwablemobs.behavior.MobProfiles;
import io.github.safetimamovic.throwablemobs.behavior.MobProfiles.Layer;
import io.github.safetimamovic.throwablemobs.carry.CarryRules;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

/**
 * {@code /throwablemobs profile <mob>} (game masters): the mob's resolved profile and, for every value, the layer it
 * came from, so pack devs can see how their files merged. Registered on each loader.
 */
public final class ThrowableMobsCommand
{
        private static final List<Field> FIELDS = List.of(
                new Field("mass", MobProfile::mass),
                new Field("gravity_multiplier", MobProfile::gravityMultiplier),
                new Field("drag_multiplier", MobProfile::dragMultiplier),
                new Field("restitution", MobProfile::restitution),
                new Field("block_restitution", MobProfile::blockRestitution),
                new Field("max_bounces", MobProfile::maxBounces),
                new Field("bounce_impacts", MobProfile::bounceImpacts),
                new Field("self_multiplier", MobProfile::selfMultiplier),
                new Field("target_multiplier", MobProfile::targetMultiplier),
                new Field("behaviors", profile -> profile.behaviors() == null ? null
                        : profile.behaviors().stream().map(ThrowableMobsCommand::name)
                                .collect(Collectors.joining(", ", "[", "]"))));

        private ThrowableMobsCommand()
        {
        }

        public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
        {
                dispatcher.register(Commands.literal(Constants.MOD_ID)
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("profile")
                                .then(Commands.argument("mob", EntityArgument.entity())
                                        .executes(ThrowableMobsCommand::profile))));
        }

        private static int profile(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
        {
                CommandSourceStack source = context.getSource();
                Entity entity = EntityArgument.getEntity(context, "mob");
                if (!(entity instanceof Mob mob))
                {
                        source.sendFailure(Component.translatable("throwablemobs.command.profile.not_mob"));
                        return 0;
                }
                List<Layer> layers = MobProfiles.layers(mob);
                MobProfile resolved = MobProfiles.resolve(mob);
                source.sendSuccess(() -> Component.translatable("throwablemobs.command.profile.header",
                        Component.literal(EntityType.getKey(mob.getType()).toString()).withStyle(ChatFormatting.AQUA),
                        layers.isEmpty() ? Component.translatable("throwablemobs.command.profile.no_layers")
                                : Component.literal(layers.stream().map(Layer::source)
                                        .collect(Collectors.joining(" < ")))), false);
                for (Field field : FIELDS)
                {
                        Object value = field.get().apply(resolved);
                        String layer = sourceOf(field, layers);
                        MutableComponent line = Component.literal("  " + field.key() + ": ")
                                .append(Component.literal(value != null ? value.toString() : defaultValue(field, mob))
                                        .withStyle(value != null ? ChatFormatting.WHITE : ChatFormatting.GRAY))
                                .append(Component.literal("  (" + (layer != null ? layer : "default") + ")")
                                        .withStyle(ChatFormatting.DARK_GRAY));
                        source.sendSuccess(() -> line, false);
                }
                return layers.size();
        }

        /** The highest layer that sets the field, stopping at a layer that replaces the ones below it. */
        private static @Nullable String sourceOf(Field field, List<Layer> layers)
        {
                for (int i = layers.size() - 1; i >= 0; i--)
                {
                        Layer layer = layers.get(i);
                        if (field.get().apply(layer.profile()) != null)
                        {
                                return layer.source();
                        }
                        if (layer.profile().replace())
                        {
                                return null;
                        }
                }
                return null;
        }

        private static String defaultValue(Field field, Mob mob)
        {
                return switch (field.key())
                {
                        case "mass" -> String.format("%.1f from hitbox", CarryRules.mass(mob));
                        case "behaviors" -> "none";
                        default -> "config";
                };
        }

        private static String name(ThrowBehavior behavior)
        {
                return behavior instanceof DataBehavior data
                        ? data.action().type().id() + " on " + data.trigger().getSerializedName()
                        : behavior.getClass().getSimpleName();
        }

        private record Field(String key, Function<MobProfile, @Nullable Object> get)
        {
        }
}
