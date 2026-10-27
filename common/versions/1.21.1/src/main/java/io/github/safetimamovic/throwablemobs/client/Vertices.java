package io.github.safetimamovic.throwablemobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;

/** {@link ShapeCanvas}'s pose and vertices; the vertex API changed in 1.21, see the 1.20.1 copy. */
final class Vertices
{
        private Vertices()
        {
        }

        /** A pose for world-space shapes: empty, RenderSystem's model-view matrix has the camera rotation. */
        static PoseStack cameraPose(Camera camera)
        {
                return new PoseStack();
        }

        static void fill(VertexConsumer quads, PoseStack.Pose pose, float x, float y, float z, int color)
        {
                quads.addVertex(pose, x, y, z).setColor(color);
        }

        static void line(VertexConsumer lines, PoseStack.Pose pose, float x, float y, float z, int color, float nx,
                float ny, float nz)
        {
                lines.addVertex(pose, x, y, z).setColor(color).setNormal(pose, nx, ny, nz);
        }
}
