package dev.mika.ultrastyle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.mika.ultrastyle.mixin.GuiGraphicsAccessor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState;
import net.minecraft.client.renderer.state.gui.GlyphRenderState;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

/** Projects only our own HUD vertices, including individual glyphs, into screen space. */
public final class PerspectiveHudCanvas {
    private final GuiRenderState destination;
    private final HudPerspective projection;
    private final float originX, originY, scale;
    private final Matrix3x2fStack pose = new Matrix3x2fStack(8);

    public PerspectiveHudCanvas(GuiGraphicsExtractor graphics, HudPerspective projection,
                                float originX, float originY, float scale) {
        destination = ((GuiGraphicsAccessor) graphics).ultrastyle$renderState();
        this.projection = projection;
        this.originX = originX;
        this.originY = originY;
        this.scale = scale;
    }
    public Matrix3x2fStack pose() { return pose; }
    public void fill(int x0, int y0, int x1, int y1, int color) {
        if (x0 == x1 || y0 == y1) return;
        var rectangle = new ColoredRectangleRenderState(RenderPipelines.GUI, TextureSetup.noTexture(),
            new Matrix3x2f(pose), x0, y0, x1, y1, color, color, null);
        submit(rectangle, rectangle.bounds());
    }
    public void text(Font font, String text, int x, int y, int color, boolean shadow) {
        Matrix3x2f transform = new Matrix3x2f(pose);
        font.prepareText(text, x, y, color, shadow, 0).visit(new Font.GlyphVisitor() {
            @Override public void acceptRenderable(TextRenderable glyph) {
                // Include the shadow and fractional glyph positions in sorting/culling bounds.
                int left = (int) Math.floor(glyph.left()) - 2;
                int top = (int) Math.floor(glyph.top()) - 2;
                var bounds = new ScreenRectangle(left, top,
                    (int) Math.ceil(glyph.right()) - left + 2,
                    (int) Math.ceil(glyph.bottom()) - top + 2).transformMaxBounds(transform);
                submit(new GlyphRenderState(transform, glyph, null), bounds);
            }
        });
    }
    private void submit(GuiElementRenderState original, ScreenRectangle localBounds) {
        if (localBounds == null) return;
        float left = sx(localBounds.left());
        float right = sx(localBounds.right());
        float top = Math.min(sy(localBounds.left(), localBounds.top()), sy(localBounds.right(), localBounds.top()));
        float bottom = Math.max(sy(localBounds.left(), localBounds.bottom()), sy(localBounds.right(), localBounds.bottom()));
        int bx = (int) Math.floor(left), by = (int) Math.floor(top);
        var bounds = new ScreenRectangle(bx, by, (int) Math.ceil(right) - bx, (int) Math.ceil(bottom) - by);
        destination.addGuiElement(new GuiElementRenderState() {
            @Override public RenderPipeline pipeline() { return original.pipeline(); }
            @Override public TextureSetup textureSetup() { return original.textureSetup(); }
            @Override public ScreenRectangle scissorArea() { return null; }
            @Override public ScreenRectangle bounds() { return bounds; }
            @Override public void buildVertices(VertexConsumer output) {
                original.buildVertices(new VertexConsumer() {
                    @Override public VertexConsumer addVertex(float x, float y, float z) {
                        output.addVertex(sx(x), sy(x, y), z); return this;
                    }
                    @Override public VertexConsumer setColor(int r, int g, int b, int a) { output.setColor(r, g, b, a); return this; }
                    @Override public VertexConsumer setColor(int color) { output.setColor(color); return this; }
                    @Override public VertexConsumer setUv(float u, float v) { output.setUv(u, v); return this; }
                    @Override public VertexConsumer setUv1(int u, int v) { output.setUv1(u, v); return this; }
                    @Override public VertexConsumer setUv2(int u, int v) { output.setUv2(u, v); return this; }
                    @Override public VertexConsumer setUv3(float u, float v) { output.setUv3(u, v); return this; }
                    @Override public VertexConsumer setNormal(float x, float y, float z) { output.setNormal(x, y, z); return this; }
                    @Override public VertexConsumer setLineWidth(float width) { output.setLineWidth(width); return this; }
                });
            }
        });
    }
    private float sx(float x) { return originX + scale * projection.x(x); }
    private float sy(float x, float y) { return originY + scale * projection.y(x, y); }
}
