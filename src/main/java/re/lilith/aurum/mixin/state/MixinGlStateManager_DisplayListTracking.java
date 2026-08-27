package re.lilith.aurum.mixin.state;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.gbuffer.state.StateTracker;
import re.lilith.aurum.pipeline.WorldRenderingPipeline;
import re.lilith.aurum.pipeline.samplers.AurumSamplers;

@Mixin(GlStateManager.class)
public class MixinGlStateManager_DisplayListTracking {
    @Shadow
    private static int activeTexture;

    @Inject(method = "enableTexture()V", at = @At("HEAD"))
    private static void aurum$onEnableTexture(CallbackInfo ci) {
        if (activeTexture == AurumSamplers.ALBEDO_TEXTURE_UNIT) {
            StateTracker.INSTANCE.albedoSampler = true;
        } else if (activeTexture == AurumSamplers.LIGHTMAP_TEXTURE_UNIT) {
            StateTracker.INSTANCE.lightmapSampler = true;
        } else {
            return;
        }

        Aurum.getPipelineManager().getPipeline().ifPresent(p -> p.setInputs(StateTracker.INSTANCE.getInputs()));
    }

    @Inject(method = "callList(I)V", at = @At("HEAD"))
    private static void aurum$beforeCallList(int listId, CallbackInfo ci) {
        Aurum.getPipelineManager().getPipeline().ifPresent(WorldRenderingPipeline::syncProgram);
    }

    @Inject(method = "disableTexture()V", at = @At("HEAD"))
    private static void aurum$onDisableTexture(CallbackInfo ci) {
        if (activeTexture == AurumSamplers.ALBEDO_TEXTURE_UNIT) {
            StateTracker.INSTANCE.albedoSampler = false;
        } else if (activeTexture == AurumSamplers.LIGHTMAP_TEXTURE_UNIT) {
            StateTracker.INSTANCE.lightmapSampler = false;
        } else {
            return;
        }

        Aurum.getPipelineManager().getPipeline().ifPresent(p -> p.setInputs(StateTracker.INSTANCE.getInputs()));
    }

}
