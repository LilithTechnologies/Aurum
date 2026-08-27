package re.lilith.aurum.pipeline.transform.impl.attribute;

import org.taumc.glsl.Transformer;
import org.taumc.glsl.grammar.GLSLParser;
import re.lilith.aurum.pipeline.transform.patch.PatchShaderType;

public class AttributeTransformer {
    public static void transform(
            Transformer transformer,
            AttributeParameters parameters,
            boolean isCoreProfile) {
        if (isCoreProfile) {
            if (parameters.type == PatchShaderType.VERTEX) {
                throw new IllegalStateException("Vertex shaders must be in the compatibility profile to run properly!");
            }
            return;
        }

        // Some shader packs don't scroll the enchantment glint texture through the texture matrix themselves.
        // See GlintScrollInjector.
        if (parameters.scrollGlint && parameters.type == PatchShaderType.VERTEX) {
            transformer.replaceExpression("gl_MultiTexCoord0", "(gl_TextureMatrix[0] * gl_MultiTexCoord0)", GLSLParser::postfix_expression);
        }

        // gl_MultiTexCoord1 and gl_MultiTexCoord2 are both ways to refer to the
        // lightmap texture coordinate.
        // See https://github.com/IrisShaders/Iris/issues/1149
        //
        // Newer versions put the lightmap on texture unit 2, because unit 1 holds the overlay.
        // This version has no overlay and keeps the lightmap on unit 1, so collapse to unit 1.
        if (parameters.inputs.lightmap()) {
            transformer.rename("gl_MultiTexCoord2", "gl_MultiTexCoord1");
        } else {
            transformer.replaceExpression("gl_MultiTexCoord1", "vec4(240.0, 240.0, 0.0, 1.0)", GLSLParser::postfix_expression);
            transformer.replaceExpression("gl_MultiTexCoord2", "vec4(240.0, 240.0, 0.0, 1.0)", GLSLParser::postfix_expression);
        }

        if (!parameters.inputs.texture()) {
            transformer.replaceExpression("gl_MultiTexCoord0", "vec4(240.0, 240.0, 0.0, 1.0)", GLSLParser::postfix_expression);
        }

        patchTextureMatrices(transformer, parameters.inputs.lightmap());

        if (parameters.inputs.overlay()) {
            patchOverlayColor(transformer, parameters);
        }

        if (parameters.type == PatchShaderType.VERTEX
                && transformer.hasVariable("gl_MultiTexCoord3")
                && !transformer.hasVariable("mc_midTexCoord")) {
            // TODO: proper type conversion
            // gl_MultiTexCoord3 is a super legacy alias of mc_midTexCoord. We don't do this
            // replacement if we think mc_midTexCoord could be defined just we can't handle
            // an existing declaration robustly. But basically the proper way to do this is
            // to define mc_midTexCoord only if it's not defined, and if it is defined,
            // figure out its type, then replace all occurrences of gl_MultiTexCoord3 with
            // the correct conversion from mc_midTexCoord's declared type to vec4.
            transformer.rename("gl_MultiTexCoord3", "mc_midTexCoord");
            transformer.injectVariable("attribute vec4 mc_midTexCoord;");
        }

        // TODO: chunk fade
        if (parameters.type == PatchShaderType.VERTEX
                && transformer.containsCall("mc_chunkFade")
                && !transformer.hasVariable("mc_chunkFade")) {
            transformer.injectVariable("const float mc_chunkFade = -1.0;");
        }
    }

    private static void patchTextureMatrices(Transformer transformer, boolean hasLightmap) {
        transformer.rename("gl_TextureMatrix", "aurum_TextureMatrix");

        String lightmapTextureMatrix = hasLightmap
                ? "gl_TextureMatrix[1]"
                : "mat4(0.00390625, 0.0, 0.0, 0.0," +
                "     0.0, 0.00390625, 0.0, 0.0," +
                "     0.0, 0.0, 0.00390625, 0.0," +
                "     0.03125, 0.03125, 0.03125, 0.00390625)"; // 0.03125 == 0.00390625 * 8

        // column major
        transformer.injectFunction("mat4 aurum_TextureMatrix[8] = mat4[8](" +
                "gl_TextureMatrix[0]," +
                lightmapTextureMatrix + "," +
                "mat4(1.0)," +
                "mat4(1.0)," +
                "mat4(1.0)," +
                "mat4(1.0)," +
                "mat4(1.0)," +
                "mat4(1.0)" +
                ");");
    }

    // Add entity color -> overlay color attribute support.
    private static void patchOverlayColor(Transformer transformer, AttributeParameters parameters) {
        // delete original declaration
        if (transformer.hasVariable("entityColor")) {
            transformer.removeVariable("entityColor");
        }

        if (parameters.type == PatchShaderType.VERTEX) {
            // add our own declarations
            transformer.injectVariable("uniform vec4 aurum_EntityColor;");
            transformer.injectVariable("varying vec4 entityColor;");

            // this is so we can pass through the overlay color at the end to the geometry
            // or fragment stage.
            transformer.prependMain(
                    "{ entityColor = aurum_EntityColor;" +
                            // Workaround for a shader pack bug:
                            // https://github.com/IrisShaders/Iris/issues/1549
                            // Some shader packs incorrectly ignore the alpha value, and assume that rgb
                            // will be
                            // zero if there is no hit flash, we try to emulate that here
                            " entityColor.rgb *= float(entityColor.a != 0.0); }");
        } else if (parameters.type == PatchShaderType.GEOMETRY) {
            // replace read references to grab the color from the first vertex.
            transformer.replaceExpression("entityColor", "entityColor[0]", GLSLParser::postfix_expression);

            // TODO: this is passthrough behavior
            transformer.injectVariable("out vec4 entityColorGS;");
            transformer.injectVariable("in vec4 entityColor[];");
            transformer.prependMain("entityColorGS = entityColor[0];");
        } else if (parameters.type == PatchShaderType.FRAGMENT) {
            transformer.injectVariable("varying vec4 entityColor;");

            // Different output name to avoid a name collision in the geometry shader.
            if (parameters.hasGeometry) {
                transformer.rename("entityColor", "entityColorGS");
            }
        }
    }
}
