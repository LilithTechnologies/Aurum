package re.lilith.aurum.mixin.gbuffer;

import net.minecraft.client.render.entity.feature.DragonEyesFeatureRenderer;
import net.minecraft.client.render.entity.feature.EndermanEyesFeatureRenderer;
import net.minecraft.client.render.entity.feature.SpiderEyesFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.gbuffer.matching.SpecialCondition;
import re.lilith.aurum.gbuffer.matching.SpecialConditions;

@Mixin({
        EndermanEyesFeatureRenderer.class,
        SpiderEyesFeatureRenderer.class,
        DragonEyesFeatureRenderer.class
})
public class MixinEyesFeatureRenderer {
    @Inject(method = "render*", at = @At("HEAD"))
    private void aurum$beginEyes(CallbackInfo ci) {
        SpecialConditions.set(SpecialCondition.ENTITY_EYES);
    }

    @Inject(method = "render*", at = @At("RETURN"))
    private void aurum$endEyes(CallbackInfo ci) {
        SpecialConditions.set(null);
    }
}
