package io.github.safetimamovic.throwablemobs;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.safetimamovic.throwablemobs.api.MobProfile;
import io.github.safetimamovic.throwablemobs.api.ThrowBehavior;
import io.github.safetimamovic.throwablemobs.behavior.DataBehavior;
import io.github.safetimamovic.throwablemobs.behavior.MobProfiles;
import io.github.safetimamovic.throwablemobs.behavior.MobProfiles.Layer;
import io.github.safetimamovic.throwablemobs.carry.CarryRules;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.ThrowConfig;
import java.util.List;
import java.util.Locale;
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
 * <p>
 * Also stubs for the client's subcommands (see {@code ThrowableMobsClientCommand}), which the client runs before they
 * reach the server. They are here for suggestions: Fabric API copies client commands into the server's tree, and when
 * the root is already there (for game masters, from {@code profile}) the copied children are lost.
 */
public final class ThrowableMobsCommand
{
        /** Run by the client; same names as in {@code ThrowableMobsClientCommand}. */
        private static final List<String> CLIENT_SUBCOMMANDS = List.of("help", "config", "keybinds");

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
                LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(Constants.MOD_ID)
                        .executes(ThrowableMobsCommand::clientOnly)
                        .then(Commands.literal("profile")
                                //? if >=26.1 {
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                //?} else
                                /*.requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))*/
                                .then(Commands.argument("mob", EntityArgument.entity())
                                        .executes(ThrowableMobsCommand::profile)));
                for (String subcommand : CLIENT_SUBCOMMANDS)
                {
                        root.then(Commands.literal(subcommand).executes(ThrowableMobsCommand::clientOnly));
                }
                dispatcher.register(root);
        }

        /** From the console, a command block or a client without the mod: nothing the server can open. */
        private static int clientOnly(CommandContext<CommandSourceStack> context)
        {
                context.getSource().sendFailure(Component.translatable("throwablemobs.command.client_only"));
                return 0;
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

        /** What applies when no layer sets the field: the hitbox mass, a config value, or a neutral multiplier. */
        private static String defaultValue(Field field, Mob mob)
        {
                ThrowConfig config = ThrowableMobsConfig.throwing();
                return switch (field.key())
                {
                        case "mass" -> String.format(Locale.ROOT, "%.1f from hitbox", CarryRules.mass(mob));
                        case "restitution" -> config.restitution() + " from config";
                        case "block_restitution" -> config.blockRestitution() + " from config";
                        case "max_bounces" -> config.maxBounces() + " from config";
                        case "bounce_impacts" -> config.bounceImpacts().getSerializedName() + " from config";
                        case "behaviors" -> "none";
                        default -> "1.0";
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
