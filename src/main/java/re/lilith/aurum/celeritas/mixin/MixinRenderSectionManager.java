package re.lilith.aurum.celeritas.mixin;

import org.embeddedt.embeddium.impl.render.chunk.RenderSectionManager;
import org.embeddedt.embeddium.impl.render.chunk.lists.RenderListManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import re.lilith.aurum.pipeline.pathways.shadows.ShadowRenderer;

@Mixin(RenderSectionManager.class)
public class MixinRenderSectionManager {
    @Shadow
    @Final
    protected RenderListManager shadowRenderListManager;

    @Inject(method = "isInShadowPass", at = @At("HEAD"), cancellable = true)
    private void aurum$reportShadowPass(CallbackInfoReturnable<Boolean> cir) {
        if (this.shadowRenderListManager != null && ShadowRenderer.ACTIVE) {
            cir.setReturnValue(true);
        }
    }
}
