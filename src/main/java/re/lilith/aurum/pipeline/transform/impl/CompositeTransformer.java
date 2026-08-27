package re.lilith.aurum.pipeline.transform.impl;

import org.taumc.glsl.Transformer;

public class CompositeTransformer {
    /**
     * @return true if the shader needs the GL_ARB_shader_texture_lod extension declared
     * (uses texture2DLod/texture3DLod on a #version &lt;= 120 shader).
     */
    public static boolean transform(
            Transformer transformer,
            int glslVersion) {
        CompositeDepthTransformer.transform(transformer);

        // if using a lod texture sampler and on version 120, patch in the extension
        // #extension GL_ARB_shader_texture_lod : require
        return glslVersion <= 120
                && (transformer.containsCall("texture2DLod") || transformer.containsCall("texture3DLod"));
    }
}
