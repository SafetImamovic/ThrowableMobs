package io.github.safetimamovic.throwablemobs;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Constants
{

        public static final String MOD_ID = "throwablemobs";
        public static final String MOD_NAME = "Throwable Mobs";
        public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

        private Constants()
        {
        }

        public static Identifier id(String path)
        {
                //? if >=1.21 {
                return Identifier.fromNamespaceAndPath(MOD_ID, path);
                //?} else
                /*return new Identifier(MOD_ID, path);*/
        }
}
