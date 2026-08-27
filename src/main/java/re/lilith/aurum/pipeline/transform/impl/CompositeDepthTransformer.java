package re.lilith.aurum.pipeline.transform.impl;

import org.taumc.glsl.Transformer;
import org.taumc.glsl.grammar.GLSLParser;

class CompositeDepthTransformer {
    public static void transform(Transformer transformer) {
        // replace original declaration
        if (transformer.hasVariable("centerDepthSmooth")) {
            transformer.removeVariable("centerDepthSmooth");
            transformer.injectVariable("uniform sampler2D aurum_centerDepthSmooth;");

            // if centerDepthSmooth is not declared as a uniform, we don't make it available
            transformer.replaceExpression("centerDepthSmooth",
                    "texture2D(aurum_centerDepthSmooth, vec2(0.5)).r", GLSLParser::postfix_expression);
        }
    }
}
