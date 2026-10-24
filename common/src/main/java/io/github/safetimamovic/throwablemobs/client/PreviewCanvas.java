package io.github.safetimamovic.throwablemobs.client;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Where the trajectory preview draws, in world coordinates: vanilla gizmos ({@link GizmoCanvas}) where there are any,
 * else shapes kept for the world renderer ({@link ShapeCanvas}). Colors are ARGB; widths and sizes in pixels.
 */
interface PreviewCanvas
{
        void line(Vec3 from, Vec3 to, int color, float width);

        void point(Vec3 at, int color, float size);

        /** The box's edges. */
        void box(AABB box, int color, float width);

        /** A filled, outlined horizontal rectangle at height {@code y}, over {@code box}'s ground area. */
        void footprint(AABB box, double y, int stroke, float width, int fill);
}
