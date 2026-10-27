package io.github.safetimamovic.throwablemobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;

/** {@link ShapeCanvas}'s pose and vertices; before 1.21 each one is ended explicitly, see the 1.21.1 copy. */
final class Vertices
{
        private Vertices()
        {
        }

        /** A pose for world-space shapes, with the camera rotation. */
        static PoseStack cameraPose(Camera camera)
        {
                // What GameRenderer.renderLevel applies before the level renders. Before 1.20.5 it isn't in
                // RenderSystem's model-view matrix, so shapes drawn without it turn with the camera.
                PoseStack poseStack = new PoseStack();
                poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
                poseStack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
                return poseStack;
        }

        static void fill(VertexConsumer quads, PoseStack.Pose pose, float x, float y, float z, int color)
        {
                quads.vertex(pose.pose(), x, y, z).color(color).endVertex();
        }

        static void line(VertexConsumer lines, PoseStack.Pose pose, float x, float y, float z, int color, float nx,
                float ny, float nz)
        {
                lines.vertex(pose.pose(), x, y, z).color(color).normal(pose.normal(), nx, ny, nz).endVertex();
        }
}
