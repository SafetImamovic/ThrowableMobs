package io.github.safetimamovic.throwablemobs.tags;

import io.github.safetimamovic.throwablemobs.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

/**
 * Our own tags. Code only checks these; convention ({@code c:}) tags are part of their default contents in the shipped
 * data, so pack devs can remove them (see THROWABLE_MOBS.md §10, Layer 1).
 */
public final class ModTags
{
        /** Never picked up. Defaults include {@code #c:bosses} and {@code #c:capturing_not_supported}. */
        public static final TagKey<EntityType<?>> UNTHROWABLE = entity("unthrowable");

        /** Only the owner may pick these up, even when {@code ownerOnlyForTamed} is off. */
        public static final TagKey<EntityType<?>> OWNER_ONLY = entity("owner_only");

        /** Never ground to put a carried mob down on (with the default deny-list filter). */
        public static final TagKey<Block> DROP_GROUND_DENIED = block("drop_ground_denied");

        /** The only ground to put a carried mob down on, when the allow-list filter is on. */
        public static final TagKey<Block> DROP_GROUND_ALLOWED = block("drop_ground_allowed");

        private ModTags()
        {
        }

        private static TagKey<EntityType<?>> entity(String path)
        {
                return TagKey.create(Registries.ENTITY_TYPE, Constants.id(path));
        }

        private static TagKey<Block> block(String path)
        {
                return TagKey.create(Registries.BLOCK, Constants.id(path));
        }
}
