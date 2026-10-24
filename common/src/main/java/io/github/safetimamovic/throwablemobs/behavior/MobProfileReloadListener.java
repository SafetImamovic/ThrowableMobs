package io.github.safetimamovic.throwablemobs.behavior;

import io.github.safetimamovic.throwablemobs.Constants;
import java.util.Map;
//? if >=26.1 {
import net.minecraft.resources.FileToIdConverter;
//?} else {
/*import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.HashMap;
*///?}
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Reads {@code data/<namespace>/throwablemobs/mob_profile/*.json} on server start and {@code /reload}. Vanilla logs
 * and skips files that don't parse. Registered on each loader under {@link #ID}.
 */
//? if >=26.1 {
public final class MobProfileReloadListener extends SimpleJsonResourceReloadListener<ProfileFile>
//?} else
/*public final class MobProfileReloadListener extends SimpleJsonResourceReloadListener*/
{
        public static final Identifier ID = Constants.id("mob_profiles");

        //? if >=26.1 {
        public MobProfileReloadListener()
        {
                super(ProfileFile.CODEC, FileToIdConverter.json(Constants.MOD_ID + "/mob_profile"));
        }

        @Override
        protected void apply(Map<Identifier, ProfileFile> files, ResourceManager manager, ProfilerFiller profiler)
        {
                MobProfiles.prepared(files);
        }
        //?} else {
        /*public MobProfileReloadListener()
        {
                super(new Gson(), Constants.MOD_ID + "/mob_profile");
        }

        // Before 1.21.2 the listener hands over JSON; parse it like the newer versions do.
        @Override
        protected void apply(Map<Identifier, JsonElement> json, ResourceManager manager, ProfilerFiller profiler)
        {
                Map<Identifier, ProfileFile> files = new HashMap<>();
                json.forEach((id, element) -> ProfileFile.CODEC.parse(JsonOps.INSTANCE, element)
                        .resultOrPartial(error -> Constants.LOG.error("Couldn't parse mob profile {}: {}", id, error))
                        .ifPresent(file -> files.put(id, file)));
                MobProfiles.prepared(files);
        }
        *///?}
}
