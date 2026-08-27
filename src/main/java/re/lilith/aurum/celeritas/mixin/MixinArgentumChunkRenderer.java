package re.lilith.aurum.celeritas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import re.lilith.aurum.pipeline.pathways.shadows.ShadowRenderer;

@Mixin(targets = "dev.rdh.argentum.impl.render.terrain.PrimitiveRenderSectionManager$ChunkRenderer")
public class MixinArgentumChunkRenderer {
    @Inject(method = "useBlockFaceCulling", at = @At("HEAD"), cancellable = true)
    private void aurum$keepAllFacesForShadows(CallbackInfoReturnable<Boolean> cir) {
        if (ShadowRenderer.ACTIVE) {
            cir.setReturnValue(false);
        }
    }
}
