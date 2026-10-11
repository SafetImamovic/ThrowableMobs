package io.github.safetimamovic.throwablemobs.behavior;

import io.github.safetimamovic.throwablemobs.Constants;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Reads {@code data/<namespace>/throwablemobs/mob_profile/*.json} on server start and {@code /reload}. Vanilla logs
 * and skips files that don't parse. Registered on each loader under {@link #ID}.
 */
public final class MobProfileReloadListener extends SimpleJsonResourceReloadListener<ProfileFile>
{
        public static final Identifier ID = Constants.id("mob_profiles");

        public MobProfileReloadListener()
        {
                super(ProfileFile.CODEC, FileToIdConverter.json(Constants.MOD_ID + "/mob_profile"));
        }

        @Override
        protected void apply(Map<Identifier, ProfileFile> files, ResourceManager manager, ProfilerFiller profiler)
        {
                MobProfiles.prepared(files);
        }
}
