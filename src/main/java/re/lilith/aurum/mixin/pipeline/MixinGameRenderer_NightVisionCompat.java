package re.lilith.aurum.mixin.pipeline;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GameRenderer.class, priority = 1010)
public class MixinGameRenderer_NightVisionCompat {
    @Inject(method = "getNightVisionStrength", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/effect/StatusEffectInstance;getDuration()I"), cancellable = true,
            require = 0)
    private void aurum$safecheckNightvisionStrength(LivingEntity entity, float tickDelta, CallbackInfoReturnable<Float> cir) {
        if (entity.getEffectInstance(StatusEffect.NIGHTVISION) == null) {
            cir.setReturnValue(0.0f);
        }
    }
}
