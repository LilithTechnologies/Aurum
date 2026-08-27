package re.lilith.aurum.uniforms;

import com.mojang.blaze3d.platform.GlStateManager;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import re.lilith.aurum.gl.state.StateUpdateNotifiers;
import re.lilith.aurum.gl.uniform.holder.DynamicUniformHolder;
import re.lilith.aurum.mixin.access.GlStateManagerAccessor;
import re.lilith.aurum.mixin.state.BooleanStateAccessor;
import re.lilith.aurum.pipeline.state.CapturedRenderingState;

import static re.lilith.aurum.gl.uniform.UniformUpdateFrequency.PER_FRAME;

// These are uniforms used internally by Aurum to fix certain things, such as the alpha test
public final class AurumInternalUniforms {
    private static final Vector4f FOG_COLOR = new Vector4f();
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);

    private AurumInternalUniforms() {
    }

    private static Vector4f getColorModulator() {
        GlStateManager.Color4 color = GlStateManagerAccessor.getCOLOR();

        return COLOR_MODULATOR.set(color.red, color.green, color.blue, color.alpha);
    }

    private static Vector4f getFogColor() {
        var fogColor = CapturedRenderingState.INSTANCE.getFogColor();

        return FOG_COLOR.set((float) fogColor.x, (float) fogColor.y, (float) fogColor.z, 1.0F);
    }

    private static float getEffectiveAlphaRef() {
        GlStateManager.AlphaTestState alpha = GlStateManagerAccessor.getALPHA_TEST();

        if (!((BooleanStateAccessor) alpha.capState).isEnabled() || alpha.func == GL11.GL_ALWAYS) {
            return -1.0f;
        }

        return alpha.ref;
    }

    private static int getEffectiveAlphaFunc() {
        GlStateManager.AlphaTestState alpha = GlStateManagerAccessor.getALPHA_TEST();

        if (!((BooleanStateAccessor) alpha.capState).isEnabled()) {
            return 7;
        }

        return alpha.func & 0x7;
    }

    public static void addFogUniforms(DynamicUniformHolder uniforms) {
        uniforms.uniform4f(PER_FRAME, "aurum_FogColor", AurumInternalUniforms::getFogColor);

        uniforms
                .uniform1f(PER_FRAME, "aurum_FogStart", () -> GlStateManagerAccessor.getFOG().start)
                .uniform1f(PER_FRAME, "aurum_FogEnd", () -> GlStateManagerAccessor.getFOG().end)
                .uniform1f(PER_FRAME, "aurum_FogDensity", () -> Math.max(0.0F, GlStateManagerAccessor.getFOG().density));

        uniforms
                .uniform1f("aurum_currentAlphaTest", AurumInternalUniforms::getEffectiveAlphaRef, StateUpdateNotifiers.alphaTestNotifier)
                .uniform1f("alphaTestRef", AurumInternalUniforms::getEffectiveAlphaRef, StateUpdateNotifiers.alphaTestNotifier)
                .uniform1i("aurum_currentAlphaFunc", AurumInternalUniforms::getEffectiveAlphaFunc, StateUpdateNotifiers.alphaFuncNotifier);

        uniforms.uniform4f("aurum_ColorModulator", AurumInternalUniforms::getColorModulator, StateUpdateNotifiers.colorModulatorNotifier);
    }
}
