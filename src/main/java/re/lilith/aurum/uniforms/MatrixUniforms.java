package re.lilith.aurum.uniforms;

import org.joml.Matrix4f;
import re.lilith.aurum.gl.uniform.holder.UniformHolder;
import re.lilith.aurum.pipeline.pathways.shadows.ShadowMatrices;
import re.lilith.aurum.pipeline.pathways.shadows.ShadowRenderer;
import re.lilith.aurum.pipeline.state.CapturedRenderingState;
import re.lilith.aurum.shaderpack.PackDirectives;

import java.util.function.Supplier;

import static re.lilith.aurum.gl.uniform.UniformUpdateFrequency.PER_FRAME;

public final class MatrixUniforms {
    private MatrixUniforms() {
    }

    public static void addMatrixUniforms(UniformHolder uniforms, PackDirectives directives) {
        addMatrix(uniforms, "ModelView", CapturedRenderingState.INSTANCE::getGbufferModelView);
        // TODO: In some cases, gbufferProjectionInverse takes on a value much different than OptiFine...
        // We need to audit Mojang's linear algebra.
        addMatrix(uniforms, "Projection", CapturedRenderingState.INSTANCE::getGbufferProjection);
        addShadowMatrix(uniforms, "ModelView", () ->
                new Matrix4f(ShadowRenderer.createShadowModelView(directives.getSunPathRotation(), directives.getShadowDirectives().getIntervalSize()).last().pose()));
        addShadowMatrix(uniforms, "Projection", () -> ShadowMatrices.createOrthoMatrix(directives.getShadowDirectives().getDistance()));
    }

    private static void addMatrix(UniformHolder uniforms, String name, Supplier<Matrix4f> supplier) {
        uniforms
                .uniformJomlMatrix(PER_FRAME, "gbuffer" + name, supplier)
                .uniformJomlMatrix(PER_FRAME, "gbuffer" + name + "Inverse", new Inverted(supplier))
                .uniformJomlMatrix(PER_FRAME, "gbufferPrevious" + name, new Previous(supplier));
    }

    private static void addShadowMatrix(UniformHolder uniforms, String name, Supplier<Matrix4f> supplier) {
        uniforms
                .uniformJomlMatrix(PER_FRAME, "shadow" + name, supplier)
                .uniformJomlMatrix(PER_FRAME, "shadow" + name + "Inverse", new Inverted(supplier));
    }

    // JomlMatrixUniform copies the value it's handed into its own field before comparing/pushing it, so
    // these suppliers can safely hand back a mutated-in-place buffer instead of allocating a fresh matrix
    // every frame for every program that reads gbufferXInverse/gbufferPreviousX.
    private static class Inverted implements Supplier<Matrix4f> {
        private final Supplier<Matrix4f> parent;
        private final Matrix4f result = new Matrix4f();

        Inverted(Supplier<Matrix4f> parent) {
            this.parent = parent;
        }

        @Override
        public Matrix4f get() {
            return result.set(parent.get()).invert();
        }
    }

    private static class Previous implements Supplier<Matrix4f> {
        private final Supplier<Matrix4f> parent;
        private final Matrix4f[] buffers = {new Matrix4f(), new Matrix4f()};
        private int previousIndex = 0;

        Previous(Supplier<Matrix4f> parent) {
            this.parent = parent;
        }

        @Override
        public Matrix4f get() {
            Matrix4f previous = buffers[previousIndex];
            buffers[1 - previousIndex].set(parent.get());
            previousIndex = 1 - previousIndex;

            return previous;
        }
    }
}
