package io.github.safetimamovic.throwablemobs.client;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.ControllerBuilder;
import dev.isxander.yacl3.api.controller.DoubleFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.isxander.yacl3.gui.image.ImageRenderer;
import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.config.CreatorConfig;
import io.github.safetimamovic.throwablemobs.config.PlayerConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.Range;
import org.jspecify.annotations.Nullable;

/**
 * The YACL config screen, built by walking the two {@link ModConfigSpec}s: a category per file, a group per section, in
 * the order the specs define them, so new options show up without code here. Names are the stock screen's translation
 * keys, descriptions the spec comments. Values are read from and written to the specs, so storage doesn't change. The
 * prompt and charge bar settings show a live preview ({@link HudPreview}).
 *
 * <p>Server options can only be changed where this client runs the server (singleplayer, LAN host) or, from 26.1 on,
 * from the title screen; anywhere else they're shown greyed out. Only loaded through {@link ConfigScreens}.
 */
final class YaclConfigScreen
{
        private static final String KEY = Constants.MOD_ID + ".configuration.";

        private YaclConfigScreen()
        {
        }

        static Screen create(@Nullable Screen parent)
        {
                Minecraft minecraft = Minecraft.getInstance();
                boolean serverLoaded = CreatorConfig.SPEC.isLoaded();
                boolean serverEditable = serverLoaded && (minecraft.level == null || minecraft.hasSingleplayerServer());
                Component lock = serverEditable ? null
                        : Component.translatable(serverLoaded ? "throwablemobs.yacl.server_locked"
                                : "throwablemobs.yacl.server_not_loaded");
                // Filled while the player category is built; the preview reads it once the screen is open.
                Map<String, Option<?>> playerOptions = new HashMap<>();
                HudPreview hud = new HudPreview(key ->
                {
                        Option<?> option = playerOptions.get(key);
                        return option == null ? null : option.pendingValue();
                });
                return YetAnotherConfigLib.createBuilder()
                        .title(Component.translatable(KEY + "title"))
                        .category(category(PlayerConfig.SPEC, "throwablemobs.yacl.player", null,
                                Map.of("prompt", hud, "charge_bar", hud), playerOptions))
                        .category(category(CreatorConfig.SPEC, "throwablemobs.yacl.server", lock, Map.of(),
                                new HashMap<>()))
                        .save(() ->
                        {
                                if (PlayerConfig.SPEC.isLoaded())
                                {
                                        PlayerConfig.SPEC.save();
                                }
                                if (serverEditable)
                                {
                                        CreatorConfig.SPEC.save();
                                }
                        })
                        .build()
                        .generateScreen(parent);
        }

        /**
         * {@code lock}: why the file can't be edited here, or {@code null} if it can. {@code previews}: a picture for
         * the settings of a section, by section. Each option is put in {@code built} by its key. Sections are one level
         * deep.
         */
        private static ConfigCategory category(ModConfigSpec spec, String name, @Nullable Component lock,
                Map<String, ImageRenderer> previews, Map<String, Option<?>> built)
        {
                ConfigCategory.Builder category = ConfigCategory.createBuilder().name(Component.translatable(name));
                if (lock != null)
                {
                        category.tooltip(lock);
                }
                for (Map.Entry<String, Object> section : spec.getSpec().valueMap().entrySet())
                {
                        if (!(section.getValue() instanceof UnmodifiableConfig values))
                        {
                                continue;
                        }
                        List<String> sectionPath = List.of(section.getKey());
                        OptionGroup.Builder group = OptionGroup.createBuilder()
                                .name(Component.translatable(KEY + section.getKey()));
                        String comment = spec.getLevelComment(sectionPath);
                        if (comment != null)
                        {
                                group.description(OptionDescription.of(Component.literal(comment)));
                        }
                        // YACL's lists are groups of their own, so they go right after their section.
                        List<OptionGroup> lists = new ArrayList<>();
                        for (String key : values.valueMap().keySet())
                        {
                                ConfigValue<?> value = spec.getValues().get(List.of(section.getKey(), key));
                                ImageRenderer preview = previews.get(section.getKey());
                                Component optionLock = ConfigScreens.lockedByServer(spec, sectionPath, key)
                                        ? ConfigScreens.SERVER_OFF : lock;
                                if (value.getDefault() instanceof List<?>)
                                {
                                        lists.add(listOption(spec, value, optionLock, preview));
                                }
                                else
                                {
                                        Option<?> option = option(spec, value, optionLock, preview);
                                        built.put(key, option);
                                        group.option(option);
                                }
                        }
                        category.group(group.build());
                        lists.forEach(category::group);
                }
                return category.build();
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static Option<?> option(ModConfigSpec spec, ConfigValue<?> value, @Nullable Component lock,
                @Nullable ImageRenderer preview)
        {
                Object initial = value.getDefault();
                if (initial instanceof Boolean)
                {
                        return option(spec, (ConfigValue<Boolean>) value, lock, preview,
                                TickBoxControllerBuilder::create);
                }
                if (initial instanceof Integer)
                {
                        Range<Integer> range = value.getSpec().getRange();
                        return option(spec, (ConfigValue<Integer>) value, lock, preview, option -> range == null
                                ? IntegerFieldControllerBuilder.create(option)
                                : IntegerFieldControllerBuilder.create(option).range(range.getMin(), range.getMax()));
                }
                if (initial instanceof Double)
                {
                        Range<Double> range = value.getSpec().getRange();
                        return option(spec, (ConfigValue<Double>) value, lock, preview, option -> range == null
                                ? DoubleFieldControllerBuilder.create(option)
                                : DoubleFieldControllerBuilder.create(option).range(range.getMin(), range.getMax()));
                }
                if (initial instanceof Enum<?>)
                {
                        return enumOption(spec, (ConfigValue) value, lock, preview);
                }
                throw new IllegalArgumentException("No YACL control for config value " + value.getPath());
        }

        private static <E extends Enum<E>> Option<E> enumOption(ModConfigSpec spec, ConfigValue<E> value,
                @Nullable Component lock, @Nullable ImageRenderer preview)
        {
                Class<E> type = value.getDefault().getDeclaringClass();
                return option(spec, value, lock, preview, option -> EnumControllerBuilder.create(option)
                        .enumClass(type)
                        .formatValue(constant -> Component.literal(constant.name())));
        }

        private static <T> Option<T> option(ModConfigSpec spec, ConfigValue<T> value, @Nullable Component lock,
                @Nullable ImageRenderer preview, Function<Option<T>, ControllerBuilder<T>> controller)
        {
                return Option.<T>createBuilder()
                        .name(name(value))
                        .description(description(value, lock, preview))
                        .binding(value.getDefault(), () -> spec.isLoaded() ? value.get() : value.getDefault(),
                                value::set)
                        .controller(controller)
                        .available(lock == null)
                        .build();
        }

        /** Lists of strings, the only kind the specs have (like {@code disabled_behaviors}). */
        @SuppressWarnings("unchecked")
        private static ListOption<String> listOption(ModConfigSpec spec, ConfigValue<?> value, @Nullable Component lock,
                @Nullable ImageRenderer preview)
        {
                ConfigValue<List<String>> list = (ConfigValue<List<String>>) value;
                return ListOption.<String>createBuilder()
                        .name(name(value))
                        .description(description(value, lock, preview))
                        .binding(List.copyOf(list.getDefault()),
                                () -> new ArrayList<>(spec.isLoaded() ? list.get() : list.getDefault()), list::set)
                        .controller(StringControllerBuilder::create)
                        .initial("")
                        .available(lock == null)
                        .build();
        }

        private static Component name(ConfigValue<?> value)
        {
                String key = value.getSpec().getTranslationKey();
                return Component.translatable(key != null ? key : KEY + value.getPath().getLast());
        }

        /**
         * The spec comment without the lines NeoForge adds (the controls show ranges and choices), then the lock, then
         * the preview if there is one.
         */
        private static OptionDescription description(ConfigValue<?> value, @Nullable Component lock,
                @Nullable ImageRenderer preview)
        {
                String comment = value.getSpec().getComment();
                String text = comment == null ? "" : Arrays.stream(comment.split("\n"))
                        .filter(line -> !line.startsWith("Range:") && !line.startsWith("Allowed Values:")
                                && !line.startsWith("Default:"))
                        .collect(Collectors.joining("\n"));
                MutableComponent description = Component.literal(text);
                if (lock != null)
                {
                        description.append("\n\n")
                                .append(lock.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                }
                OptionDescription.Builder builder = OptionDescription.createBuilder().text(description);
                if (preview != null)
                {
                        builder.customImage(preview);
                }
                return builder.build();
        }
}
