package re.lilith.aurum.celeritas.mixin;

import dev.rdh.argentum.impl.render.entity.instancing.EntityCapture;
import dev.rdh.argentum.impl.render.entity.instancing.EntityInstancing;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import re.lilith.aurum.pipeline.pathways.shadows.ShadowRenderer;

@Mixin(EntityInstancing.class)
public class MixinEntityInstancing {
    @Inject(method = "beginEntity", at = @At("HEAD"), cancellable = true)
    private void aurum$declineDuringShadowPass(EntityModel model, Identifier texture, boolean player, boolean preserveFixedFunction,
                                               int packedLight, float effectTime, float overlayRed, float overlayGreen,
                                               float overlayBlue, float overlayAlpha, CallbackInfoReturnable<EntityCapture> cir) {
        if (ShadowRenderer.ACTIVE) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "beginItemEntity", at = @At("HEAD"), cancellable = true)
    private void aurum$declineItemDuringShadowPass(ItemEntity entity, BakedModel model, int packedLight,
                                                   CallbackInfoReturnable<EntityCapture> cir) {
        if (ShadowRenderer.ACTIVE) {
            cir.setReturnValue(null);
        }
    }
}
