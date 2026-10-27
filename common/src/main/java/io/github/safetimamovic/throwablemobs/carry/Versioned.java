package io.github.safetimamovic.throwablemobs.carry;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Calls that need different code on some of the Minecraft versions the mod builds for. */
final class Versioned
{
        private Versioned()
        {
        }

        /** How far {@code entity} moved last tick. Server-side players move on their own client, not by velocity. */
        static Vec3 knownMovement(Entity entity)
        {
                //? if >=1.21 {
                return entity.getKnownMovement();
                //?} else {
                /*return entity instanceof ServerPlayer player
                        ? player.position().subtract(player.xo, player.yo, player.zo)
                        : entity.getDeltaMovement();
                *///?}
        }

        /** Round-trip time to {@code player}'s client, in milliseconds. */
        static int latency(ServerPlayer player)
        {
                //? if >=1.20.2 {
                return player.connection.latency();
                //?} else
                /*return player.latency;*/
        }

        //? if <1.20.5 {
        /*/^* Block reach before the reach attributes of 1.20.5: 4.5, 5 in creative. *^/
        static double blockReach(Player player)
        {
                return player.isCreative() ? 5.0 : 4.5;
        }

        /^*
         * Whether {@code box} is within entity reach plus {@code tolerance} of {@code player}'s eyes, like 1.20.5's
         * {@code canInteractWithEntity}: reach 3, 5 in creative.
         *^/
        static boolean withinReach(Player player, AABB box, double tolerance)
        {
                double reach = (player.isCreative() ? 5.0 : 3.0) + tolerance;
                Vec3 eye = player.getEyePosition();
                double dx = Math.max(Math.max(box.minX - eye.x, eye.x - box.maxX), 0.0);
                double dy = Math.max(Math.max(box.minY - eye.y, eye.y - box.maxY), 0.0);
                double dz = Math.max(Math.max(box.minZ - eye.z, eye.z - box.maxZ), 0.0);
                return dx * dx + dy * dy + dz * dz < reach * reach;
        }
        *///?}
}
