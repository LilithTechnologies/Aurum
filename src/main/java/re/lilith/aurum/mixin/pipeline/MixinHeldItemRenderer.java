package re.lilith.aurum.mixin.pipeline;

import net.minecraft.client.render.item.HeldItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.pipeline.pathways.HandRenderer;

@Mixin(HeldItemRenderer.class)
public class MixinHeldItemRenderer {
    @Inject(method = "renderArmHoldingItem", at = @At("HEAD"), cancellable = true)
    private void aurum$skipTranslucentHands(float tickDelta, CallbackInfo ci) {
        if (Aurum.getCurrentPack().isPresent()) {
            if (HandRenderer.INSTANCE.isRenderingSolid() && HandRenderer.INSTANCE.isHandTranslucent()) {
                ci.cancel();
            } else if (!HandRenderer.INSTANCE.isRenderingSolid() && !HandRenderer.INSTANCE.isHandTranslucent()) {
                ci.cancel();
            }
        }
    }
}
