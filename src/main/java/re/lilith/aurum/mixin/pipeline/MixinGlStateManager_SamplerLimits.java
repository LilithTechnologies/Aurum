package re.lilith.aurum.mixin.pipeline;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import re.lilith.aurum.gl.sampler.SamplerLimits;

@Mixin(GlStateManager.class)
public class MixinGlStateManager_SamplerLimits {
    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 8), require = 1)
    private static int aurum$increaseMaximumAllowedTextureUnits(int existingValue) {
        return SamplerLimits.get().getMaxTextureUnits();
    }
}
