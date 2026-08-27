package re.lilith.aurum.pipeline.transform.impl;

import org.taumc.glsl.Transformer;
import org.taumc.glsl.grammar.GLSLParser;
import re.lilith.aurum.pipeline.transform.patch.Parameters;

public class CeleritasTerrainTransformer {
    public static void transform(
            Transformer transformer,
            Parameters parameters) {
        switch (parameters.type) {
            // For Celeritas patching, treat fragment and geometry the same
            case FRAGMENT:
            case GEOMETRY:
                transformFragment(transformer);
                break;
            case VERTEX:
                transformVertex(transformer, parameters);
                break;
            default:
                throw new IllegalStateException("Unexpected Celeritas terrain patching shader type: " + parameters.type);
        }
    }

    /**
     * Transforms vertex shaders.
     */
    public static void transformVertex(
            Transformer transformer,
            Parameters parameters) {
        transformer.injectVariable("attribute vec3 a_PosId;");
        transformer.injectVariable("attribute vec4 a_Color;");
        transformer.injectVariable("attribute vec2 a_TexCoord;");
        transformer.injectVariable("attribute vec2 aurum_LightCoord;");
        transformer.injectVariable("attribute vec3 aurum_Normal;"); // some are shared
        transformer.injectVariable("attribute vec4 aurum_SectionOffset;");
        transformer.injectVariable("uniform vec3 u_ModelScale;");
        transformer.injectVariable("uniform vec2 u_TextureScale;");
        transformer.injectVariable("uniform vec3 u_RegionOffset;");
        transformer.injectFunction("vec3 aurum_Pos = a_PosId;");
        transformer.injectFunction("vec4 aurum_Color = a_Color;");
        transformer.injectFunction("vec2 aurum_TexCoord = a_TexCoord;");
        transformer.injectFunction("vec4 aurum_ModelOffset = vec4(u_RegionOffset + aurum_SectionOffset.xyz, 0.0);");
        transformer.injectFunction("vec4 aurum_LightTexCoord = vec4(aurum_LightCoord, 0, 1);");
        transformer.injectFunction("vec4 ftransform() { return gl_ModelViewProjectionMatrix * gl_Vertex; }");

        transformShared(transformer);

        transformer.replaceExpression("gl_Vertex",
                "vec4((aurum_Pos * u_ModelScale) + aurum_ModelOffset.xyz, 1.0)", GLSLParser::postfix_expression);
        transformer.replaceExpression("gl_MultiTexCoord0",
                "vec4(aurum_TexCoord * u_TextureScale, 0.0, 1.0)", GLSLParser::postfix_expression);
        transformer.replaceExpression("gl_MultiTexCoord1",
                "aurum_LightTexCoord", GLSLParser::postfix_expression);
        transformer.replaceExpression("gl_MultiTexCoord2",
                "aurum_LightTexCoord", GLSLParser::postfix_expression);
        transformer.rename("gl_Color", "aurum_Color");
        transformer.rename("gl_Normal", "aurum_Normal");
        transformer.rename("ftransform", "aurum_ftransform");

        // TODO: chunk fade
        if (transformer.containsCall("mc_chunkFade") && !transformer.hasVariable("mc_chunkFade")) {
            transformer.injectVariable("const float mc_chunkFade = -1.0;");
        }
    }

    /**
     * Transforms fragment shaders. The fragment shader does only the shared things
     * from the vertex shader.
     */
    public static void transformFragment(
            Transformer transformer) {
        // interestingly there is nothing that isn't shared
        transformShared(transformer);
    }

    /**
     * Does the things that transformVertex and transformFragment have in common.
     */
    private static void transformShared(
            Transformer transformer) {
        transformer.injectVariable("uniform mat4 aurum_ModelViewMatrix;");
        transformer.injectVariable("uniform mat4 u_ModelViewProjectionMatrix;");
        transformer.injectVariable("uniform mat4 aurum_NormalMatrix;");
        transformer.injectVariable("uniform mat4 aurum_LightmapTextureMatrix;");
        transformer.rename("gl_ModelViewMatrix", "aurum_ModelViewMatrix");
        transformer.rename("gl_ModelViewProjectionMatrix", "u_ModelViewProjectionMatrix");
        transformer.replaceExpression("gl_NormalMatrix", "mat3(aurum_NormalMatrix)", GLSLParser::postfix_expression);

        transformer.replaceExpression("gl_TextureMatrix[0]", "mat4(1.0)", GLSLParser::postfix_expression);
        transformer.replaceExpression("gl_TextureMatrix[1]", "aurum_LightmapTextureMatrix", GLSLParser::postfix_expression);
    }
}
