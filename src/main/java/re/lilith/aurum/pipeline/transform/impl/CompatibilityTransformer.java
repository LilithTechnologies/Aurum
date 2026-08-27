package re.lilith.aurum.pipeline.transform.impl;

import org.taumc.glsl.ShaderPrinter;
import org.taumc.glsl.Transformer;
import org.taumc.glsl.grammar.GLSLLexer;
import org.taumc.glsl.grammar.GLSLParser;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.pipeline.transform.patch.Parameters;
import re.lilith.aurum.pipeline.transform.patch.Patch;
import re.lilith.aurum.pipeline.transform.patch.PatchShaderType;

import java.util.EnumMap;
import java.util.HashMap;

public class CompatibilityTransformer {

    private static final PatchShaderType[] pipeline = {PatchShaderType.VERTEX, PatchShaderType.GEOMETRY, PatchShaderType.FRAGMENT};

    public static void transformEach(Transformer transformer, Parameters parameters) {
        if (parameters.type == PatchShaderType.VERTEX) {
            // This is a hacky patch for Sildur's Shaders that changes the way it does its waving water to make it work better
            // See https://github.com/IrisShaders/Iris/issues/509

            String oldCode = "fract(worldpos.y + 0.001)";
            String newCode = "fract(worldpos.y + 0.01)";

            transformer.replaceExpression(oldCode, newCode, GLSLParser::binary_expression);
            transformer.replaceExpression(oldCode, newCode, GLSLParser::postfix_expression);
        }

        if (parameters.type == PatchShaderType.FRAGMENT && parameters.patch != Patch.COMPOSITE) {
            injectAlphaTestDiscard(transformer);
        }

        renameLegacyTextureFunctions(transformer);
        injectInverseIfMissing(transformer, parameters.version);

        transformer.removeConstAssignment();
    }

    public static void transformCompute(Transformer transformer, int version) {
        renameLegacyTextureFunctions(transformer);
        injectInverseIfMissing(transformer, version);

        transformer.removeConstAssignment();
    }

    private static void injectInverseIfMissing(Transformer transformer, int version) {
        if (version >= 140 || !transformer.containsCall("inverse")) {
            return;
        }

        transformer.injectFunction(
                "mat2 inverse(mat2 m) {" +
                        "return mat2(m[1][1], -m[0][1], -m[1][0], m[0][0]) / (m[0][0] * m[1][1] - m[0][1] * m[1][0]);" +
                        "}");

        transformer.injectFunction(
                "mat3 inverse(mat3 m) {" +
                        "float b01 = m[2][2] * m[1][1] - m[1][2] * m[2][1];" +
                        "float b11 = -m[2][2] * m[1][0] + m[1][2] * m[2][0];" +
                        "float b21 = m[2][1] * m[1][0] - m[1][1] * m[2][0];" +
                        "float det = m[0][0] * b01 + m[0][1] * b11 + m[0][2] * b21;" +
                        "return mat3(" +
                        "b01, -m[2][2] * m[0][1] + m[0][2] * m[2][1], m[1][2] * m[0][1] - m[0][2] * m[1][1]," +
                        "b11, m[2][2] * m[0][0] - m[0][2] * m[2][0], -m[1][2] * m[0][0] + m[0][2] * m[1][0]," +
                        "b21, -m[2][1] * m[0][0] + m[0][1] * m[2][0], m[1][1] * m[0][0] - m[0][1] * m[1][0]" +
                        ") / det;" +
                        "}");

        transformer.injectFunction(
                "mat4 inverse(mat4 m) {" +
                        "float b00 = m[0][0] * m[1][1] - m[0][1] * m[1][0];" +
                        "float b01 = m[0][0] * m[1][2] - m[0][2] * m[1][0];" +
                        "float b02 = m[0][0] * m[1][3] - m[0][3] * m[1][0];" +
                        "float b03 = m[0][1] * m[1][2] - m[0][2] * m[1][1];" +
                        "float b04 = m[0][1] * m[1][3] - m[0][3] * m[1][1];" +
                        "float b05 = m[0][2] * m[1][3] - m[0][3] * m[1][2];" +
                        "float b06 = m[2][0] * m[3][1] - m[2][1] * m[3][0];" +
                        "float b07 = m[2][0] * m[3][2] - m[2][2] * m[3][0];" +
                        "float b08 = m[2][0] * m[3][3] - m[2][3] * m[3][0];" +
                        "float b09 = m[2][1] * m[3][2] - m[2][2] * m[3][1];" +
                        "float b10 = m[2][1] * m[3][3] - m[2][3] * m[3][1];" +
                        "float b11 = m[2][2] * m[3][3] - m[2][3] * m[3][2];" +
                        "float det = b00 * b11 - b01 * b10 + b02 * b09 + b03 * b08 - b04 * b07 + b05 * b06;" +
                        "return mat4(" +
                        "m[1][1] * b11 - m[1][2] * b10 + m[1][3] * b09," +
                        "m[0][2] * b10 - m[0][1] * b11 - m[0][3] * b09," +
                        "m[3][1] * b05 - m[3][2] * b04 + m[3][3] * b03," +
                        "m[2][2] * b04 - m[2][1] * b05 - m[2][3] * b03," +
                        "m[1][2] * b08 - m[1][0] * b11 - m[1][3] * b07," +
                        "m[0][0] * b11 - m[0][2] * b08 + m[0][3] * b07," +
                        "m[3][2] * b02 - m[3][0] * b05 - m[3][3] * b01," +
                        "m[2][0] * b05 - m[2][2] * b02 + m[2][3] * b01," +
                        "m[1][0] * b10 - m[1][1] * b08 + m[1][3] * b06," +
                        "m[0][1] * b08 - m[0][0] * b10 - m[0][3] * b06," +
                        "m[3][0] * b04 - m[3][1] * b02 + m[3][3] * b00," +
                        "m[2][1] * b02 - m[2][0] * b04 - m[2][3] * b00," +
                        "m[1][1] * b07 - m[1][0] * b09 - m[1][2] * b06," +
                        "m[0][0] * b09 - m[0][1] * b07 + m[0][2] * b06," +
                        "m[3][1] * b01 - m[3][0] * b03 - m[3][2] * b00," +
                        "m[2][0] * b03 - m[2][1] * b01 + m[2][2] * b00" +
                        ") / det;" +
                        "}");
    }

    private static void renameLegacyTextureFunctions(Transformer transformer) {
        if (transformer.hasVariable("texture")) {
            transformer.rename("texture", "gtexture");
        }
        if (transformer.hasVariable("gcolor")) {
            transformer.rename("gcolor", "gtexture");
        }

        transformer.renameFunctionCall("texture2D", "texture");
        transformer.renameFunctionCall("texture3D", "texture");
        transformer.renameFunctionCall("texture2DLod", "textureLod");
        transformer.renameFunctionCall("texture3DLod", "textureLod");
        transformer.renameFunctionCall("texture2DProj", "textureProj");
        transformer.renameFunctionCall("texture3DProj", "textureProj");
        transformer.renameFunctionCall("texture2DGrad", "textureGrad");
        transformer.renameFunctionCall("texture2DGradARB", "textureGrad");
        transformer.renameFunctionCall("texture3DGrad", "textureGrad");
        transformer.renameFunctionCall("texelFetch2D", "texelFetch");
        transformer.renameFunctionCall("texelFetch3D", "texelFetch");
        transformer.renameFunctionCall("textureSize2D", "textureSize");

        transformer.renameAndWrapShadow("shadow2D", "texture");
        transformer.renameAndWrapShadow("shadow2DLod", "textureLod");
    }

    private static void injectAlphaTestDiscard(Transformer transformer) {
        String output;

        if (transformer.containsCall("gl_FragData")) {
            output = "gl_FragData[0]";
        } else if (transformer.containsCall("gl_FragColor")) {
            output = "gl_FragColor";
        } else {
            return;
        }

        transformer.injectVariable("uniform float aurum_currentAlphaTest;");
        transformer.injectVariable("uniform int aurum_currentAlphaFunc;");
        transformer.injectFunction(
                "bool aurum_alphaTestPass(float a) {" +
                        "if (aurum_currentAlphaFunc == 7) return true;" +
                        "if (aurum_currentAlphaFunc == 0) return false;" +
                        "float qa = floor(a * 255.0 + 0.5);" +
                        "float qr = floor(aurum_currentAlphaTest * 255.0 + 0.5);" +
                        "if (aurum_currentAlphaFunc == 1) return qa < qr;" +
                        "if (aurum_currentAlphaFunc == 2) return qa == qr;" +
                        "if (aurum_currentAlphaFunc == 3) return qa <= qr;" +
                        "if (aurum_currentAlphaFunc == 4) return qa > qr;" +
                        "if (aurum_currentAlphaFunc == 5) return qa != qr;" +
                        "return qa >= qr;" +
                        "}");
        transformer.appendMain("if (!aurum_alphaTestPass(" + output + ".a)) discard;");
    }

    // does transformations that require cross-shader type data
    public static void transformGrouped(EnumMap<PatchShaderType, Transformer> trees) {
        PatchShaderType prevType = null;
        for (PatchShaderType type : pipeline) {
            if (trees.get(type) == null) {
                continue;
            }

            // if the current type has sources but the previous one doesn't, set the
            // previous one and continue
            if (prevType == null) {
                prevType = type;
                continue;
            }

            Transformer prevTransformer = trees.get(prevType);

            var outDec = new HashMap<>(prevTransformer.findQualifiers(GLSLLexer.OUT));
            outDec.putAll(prevTransformer.findQualifiers(GLSLLexer.VARYING));
            Transformer currentTransformer = trees.get(type);
            if (currentTransformer == null) {
                prevType = type;
                continue;
            }

            // MB: VARYING is intentionally not merged into inDec here (unlike outDec above). A
            // varying-to-varying pair on both stages (e.g. composite's near-universal
            // "varying vec4 texcoord;") is already fully self-consistent and needs no patching; only
            // merging on the outDec side is needed to recognize it as satisfying an "in"/"varying"
            // declaration in the next stage (the actual gap this fixed - entityColor declared
            // "varying" in the vertex stage but "in" in the geometry stage).
            var inDec = currentTransformer.findQualifiers(GLSLLexer.IN);
            for (String in : inDec.keySet()) {
                if (in.startsWith("gl_")) {
                    continue;
                }

                if (!outDec.containsKey(in)) {
                    if (!currentTransformer.containsCall(in)) {
                        continue;
                    }

                    String outDeclaration = ShaderPrinter.getFormattedShader(inDec.get(in).fully_specified_type())
                            + " " + in + ";";
                    prevTransformer.variable = null;
                    prevTransformer.injectVariable(outDeclaration.replaceFirst("in", "out"));

                    if (!prevTransformer.hasAssigment(in)) {
                        prevTransformer.initialize(inDec.get(in), in);
                    }
                    Aurum.LOGGER.warn("The in declaration '{}' in the {} shader is missing a corresponding out declaration in the previous stage {} and has been compatibility-patched. See debugging.md for more information.", in, type.name(), prevType.name());
                } else {
                    var outType = outDec.get(in).fully_specified_type().type_specifier().type_specifier_nonarray().children.getFirst();
                    var inType = inDec.get(in).fully_specified_type().type_specifier().type_specifier_nonarray().children.getFirst();

                    if (outDec.get(in).fully_specified_type().type_specifier().array_specifier() != null) {
                        continue;
                    }

                    if (inType.getText().equals(outType.getText())) {
                        if (!prevTransformer.hasAssigment(in)) {
                            prevTransformer.initialize(inDec.get(in), in);
                        }
                    } else {
                        int outComp = vecComponents(outType.getText());
                        int inComp = vecComponents(inType.getText());
                        if (outComp < 0 || inComp < 0) {
                            continue;
                        }

                        String temp = "aurum_template_" + in;
                        prevTransformer.rename(in, temp);
                        String newOut = ShaderPrinter.getFormattedShader(inDec.get(in).fully_specified_type())
                                + " " + in + ";";
                        prevTransformer.variable = null;
                        prevTransformer.injectVariable(newOut.replaceFirst("in", "out"));
                        String cast = outComp < inComp
                                ? in + " = " + inType.getText() + "(" + temp + ", vec4(0));"
                                : in + " = " + inType.getText() + "(" + temp + ");";
                        prevTransformer.appendMain(cast);

                        Aurum.LOGGER.warn("The out declaration '{}' in the {} shader has a different type {} than the corresponding in declaration of type {} in the following stage {} and has been compatibility-patched. See debugging.md for more information.", in, prevType.name(), outType.getText(), inType.getText(), type.name());
                    }
                }
            }

            prevType = type;
        }
    }

    private static int vecComponents(String type) {
        return switch (type) {
            case "vec2" -> 2;
            case "vec3" -> 3;
            case "vec4" -> 4;
            default -> -1;
        };
    }
}
