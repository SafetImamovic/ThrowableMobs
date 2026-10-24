//? if >=26.1 {
package io.github.safetimamovic.throwablemobs.client;

import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Draws with vanilla gizmos, which vanilla collects per client tick and draws every frame until the next one. */
final class GizmoCanvas implements PreviewCanvas
{
        static final GizmoCanvas INSTANCE = new GizmoCanvas();

        private GizmoCanvas()
        {
        }

        @Override
        public void line(Vec3 from, Vec3 to, int color, float width)
        {
                Gizmos.line(from, to, color, width);
        }

        @Override
        public void point(Vec3 at, int color, float size)
        {
                Gizmos.point(at, color, size);
        }

        @Override
        public void box(AABB box, int color, float width)
        {
                Gizmos.cuboid(box, GizmoStyle.stroke(color, width));
        }

        @Override
        public void footprint(AABB box, double y, int stroke, float width, int fill)
        {
                Gizmos.rect(new Vec3(box.minX, y, box.minZ), new Vec3(box.maxX, y, box.maxZ), Direction.UP,
                        GizmoStyle.strokeAndFill(stroke, width, fill));
        }
}
//?}
