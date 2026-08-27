package re.lilith.aurum.uniforms.builtin;

import org.joml.Matrix4f;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.gl.uniform.UniformUpdateFrequency;
import re.lilith.aurum.gl.uniform.holder.UniformHolder;

public class BuiltinReplacementUniforms {
    private static final Matrix4f lightmapTextureMatrix;

    static {
        // This mimics the transformations done in LightmapTextureManager to the GL_TEXTURE matrix.
        lightmapTextureMatrix = new Matrix4f();
        lightmapTextureMatrix.identity();
        lightmapTextureMatrix.scale(0.00390625f);
        lightmapTextureMatrix.translate(8.0f, 8.0f, 8.0f);
    }

    public static void addBuiltinReplacementUniforms(UniformHolder uniforms) {
        uniforms.uniformJomlMatrix(UniformUpdateFrequency.ONCE, "aurum_LightmapTextureMatrix", () -> {
            Aurum.LOGGER.warn("A shader appears to require the lightmap texture matrix even after transformations have occurred");
            Aurum.LOGGER.warn("Aurum handles this correctly but it indicates that the shader is doing weird things with lightmap coordinates");

            return lightmapTextureMatrix;
        });
    }
}
