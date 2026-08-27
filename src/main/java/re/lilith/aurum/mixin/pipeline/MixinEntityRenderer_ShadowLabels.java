package re.lilith.aurum.mixin.pipeline;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.pipeline.pathways.shadows.ShadowRenderer;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer_ShadowLabels {
    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void aurum$skipLabelDuringShadowPass(Entity entity, String text, double x, double y, double z, int maxDistance, CallbackInfo ci) {
        if (ShadowRenderer.ACTIVE) {
            ci.cancel();
        }
    }
}
