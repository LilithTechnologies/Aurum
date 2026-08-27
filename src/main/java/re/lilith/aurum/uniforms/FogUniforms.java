package re.lilith.aurum.uniforms;

import com.mojang.blaze3d.platform.GlStateManager;
import re.lilith.aurum.gl.state.StateUpdateNotifiers;
import re.lilith.aurum.gl.uniform.UniformUpdateFrequency;
import re.lilith.aurum.gl.uniform.holder.DynamicUniformHolder;
import re.lilith.aurum.mixin.access.GlStateManagerAccessor;
import re.lilith.aurum.mixin.state.BooleanStateAccessor;

public class FogUniforms {
    private FogUniforms() {
        // no construction
    }

    public static void addFogUniforms(DynamicUniformHolder uniforms) {
        uniforms.uniform1i("fogMode", () -> {
            GlStateManager.FogState fog = GlStateManagerAccessor.getFOG();

            if (!((BooleanStateAccessor) fog.capState).isEnabled()) {
                return 0;
            }

            return GlStateManagerAccessor.getFOG().mode;
        }, listener -> {
            StateUpdateNotifiers.fogToggleNotifier.setListener(listener);
            StateUpdateNotifiers.fogModeNotifier.setListener(listener);
        });

        uniforms.uniform1i(UniformUpdateFrequency.ONCE, "fogShape", () -> 0);

        uniforms.uniform1f("fogDensity", () -> GlStateManagerAccessor.getFOG().density, listener -> {
            StateUpdateNotifiers.fogToggleNotifier.setListener(listener);
            StateUpdateNotifiers.fogDensityNotifier.setListener(listener);
        });

        uniforms.uniform1f("fogStart", () -> GlStateManagerAccessor.getFOG().start, listener -> {
            StateUpdateNotifiers.fogToggleNotifier.setListener(listener);
            StateUpdateNotifiers.fogStartNotifier.setListener(listener);
        });

        uniforms.uniform1f("fogEnd", () -> GlStateManagerAccessor.getFOG().end, listener -> {
            StateUpdateNotifiers.fogToggleNotifier.setListener(listener);
            StateUpdateNotifiers.fogEndNotifier.setListener(listener);
        });
    }
}
