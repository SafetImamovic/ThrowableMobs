//? if <26.1 {
/*package io.github.safetimamovic.throwablemobs.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/^*
 * Draws like the gizmos of newer versions: what's added during a client tick is drawn every frame until the next tick
 * replaces it. Each loader calls {@link #render} from its world render event. Points are small cubes.
 *^/
public final class ShapeCanvas implements PreviewCanvas
{
        static final ShapeCanvas INSTANCE = new ShapeCanvas();
        /^* Gizmo sizes are pixels; a point is this many blocks wide per pixel. *^/
        private static final double BLOCKS_PER_PIXEL = 0.0125;

        private final List<Shape> shapes = new ArrayList<>();

        private ShapeCanvas()
        {
        }

        /^* Start of each client tick: forgets last tick's shapes. *^/
        void clear()
        {
                shapes.clear();
        }

        @Override
        public void line(Vec3 from, Vec3 to, int color, float width)
        {
                shapes.add(new Line(from, to, color, width));
        }

        @Override
        public void point(Vec3 at, int color, float size)
        {
                shapes.add(new Cube(new AABB(at, at).inflate(size * BLOCKS_PER_PIXEL / 2.0), color));
        }

        @Override
        public void box(AABB box, int color, float width)
        {
                shapes.add(new Edges(box, color, width));
        }

        @Override
        public void footprint(AABB box, double y, int stroke, float width, int fill)
        {
                shapes.add(new Fill(box, y, fill));
                shapes.add(new Edges(new AABB(box.minX, y, box.minZ, box.maxX, y, box.maxZ), stroke, width));
        }

        /^* Every frame while the world renders, after translucent blocks, with the camera rotation applied. *^/
        public static void render()
        {
                List<Shape> shapes = INSTANCE.shapes;
                if (shapes.isEmpty())
                {
                        return;
                }
                Minecraft minecraft = Minecraft.getInstance();
                Camera camera = minecraft.gameRenderer.getMainCamera();
                PoseStack poseStack = Vertices.cameraPose(camera);
                poseStack.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);
                PoseStack.Pose pose = poseStack.last();
                MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

                VertexConsumer quads = buffers.getBuffer(RenderType.debugQuads());
                for (Shape shape : shapes)
                {
                        if (shape instanceof Fill fill)
                        {
                                AABB box = fill.box();
                                float y = (float) fill.y();
                                Vertices.fill(quads, pose, (float) box.minX, y, (float) box.minZ, fill.color());
                                Vertices.fill(quads, pose, (float) box.minX, y, (float) box.maxZ, fill.color());
                                Vertices.fill(quads, pose, (float) box.maxX, y, (float) box.maxZ, fill.color());
                                Vertices.fill(quads, pose, (float) box.maxX, y, (float) box.minZ, fill.color());
                        }
                }
                buffers.endBatch(RenderType.debugQuads());

                VertexConsumer cubes = buffers.getBuffer(RenderType.debugFilledBox());
                for (Shape shape : shapes)
                {
                        if (shape instanceof Cube cube)
                        {
                                AABB box = cube.box();
                                int color = cube.color();
                                LevelRenderer.addChainedFilledBoxVertices(poseStack, cubes, box.minX, box.minY, box.minZ,
                                        box.maxX, box.maxY, box.maxZ, red(color), green(color), blue(color),
                                        alpha(color));
                        }
                }
                buffers.endBatch(RenderType.debugFilledBox());

                // Line width is render state, so one batch per width.
                List<Float> widths = new ArrayList<>();
                for (Shape shape : shapes)
                {
                        float width = shape instanceof Line line ? line.width()
                                : shape instanceof Edges edges ? edges.width() : 0.0F;
                        if (width > 0.0F && !widths.contains(width))
                        {
                                widths.add(width);
                        }
                }
                for (float width : widths)
                {
                        RenderSystem.lineWidth(width);
                        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
                        for (Shape shape : shapes)
                        {
                                if (shape instanceof Line line && line.width() == width)
                                {
                                        Vec3 normal = line.to().subtract(line.from()).normalize();
                                        vertex(lines, pose, line.from(), line.color(), normal);
                                        vertex(lines, pose, line.to(), line.color(), normal);
                                }
                                else if (shape instanceof Edges edges && edges.width() == width)
                                {
                                        int color = edges.color();
                                        LevelRenderer.renderLineBox(poseStack, lines, edges.box(), red(color),
                                                green(color), blue(color), alpha(color));
                                }
                        }
                        buffers.endBatch(RenderType.lines());
                }
                RenderSystem.lineWidth(1.0F);
        }

        private static void vertex(VertexConsumer lines, PoseStack.Pose pose, Vec3 at, int color, Vec3 normal)
        {
                Vertices.line(lines, pose, (float) at.x, (float) at.y, (float) at.z, color, (float) normal.x,
                        (float) normal.y, (float) normal.z);
        }

        private static float alpha(int color)
        {
                return (color >>> 24) / 255.0F;
        }

        private static float red(int color)
        {
                return (color >> 16 & 0xFF) / 255.0F;
        }

        private static float green(int color)
        {
                return (color >> 8 & 0xFF) / 255.0F;
        }

        private static float blue(int color)
        {
                return (color & 0xFF) / 255.0F;
        }

        private sealed interface Shape permits Line, Cube, Edges, Fill
        {
        }

        private record Line(Vec3 from, Vec3 to, int color, float width) implements Shape
        {
        }

        private record Cube(AABB box, int color) implements Shape
        {
        }

        private record Edges(AABB box, int color, float width) implements Shape
        {
        }

        private record Fill(AABB box, double y, int color) implements Shape
        {
        }
}
*///?}
