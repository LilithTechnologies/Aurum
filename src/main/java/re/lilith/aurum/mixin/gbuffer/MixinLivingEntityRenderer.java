package re.lilith.aurum.mixin.gbuffer;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import re.lilith.aurum.uniforms.utility.EntityColorState;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity> {
    @org.spongepowered.asm.mixin.Shadow
    protected abstract int method_5776(T entity, float brightness, float tickDelta);

    @Inject(method = "method_10252", at = @At("RETURN"))
    private void aurum$setEntityColor(T entity, float tickDelta, boolean combineTextures,
                                      CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }

        if (entity.hurtTime > 0 || entity.deathTime > 0) {
            // The vanilla path uploads a constant of (1, 0, 0, 0.3) for the hurt flash.
            EntityColorState.set(1.0F, 0.0F, 0.0F, 0.3F);
            return;
        }

        int overlay = this.method_5776(entity, entity.getBrightnessAtEyes(tickDelta), tickDelta);

        EntityColorState.set(
                (overlay >> 16 & 0xFF) / 255.0F,
                (overlay >> 8 & 0xFF) / 255.0F,
                (overlay & 0xFF) / 255.0F,
                (overlay >> 24 & 0xFF) / 255.0F);
    }

    @Inject(method = "method_10260", at = @At("HEAD"))
    private void aurum$clearEntityColor(CallbackInfo ci) {
        EntityColorState.reset();
    }
}
