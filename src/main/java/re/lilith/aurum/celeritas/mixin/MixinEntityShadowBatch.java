package re.lilith.aurum.celeritas.mixin;

import dev.rdh.argentum.impl.render.entity.EntityShadowBatch;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import re.lilith.aurum.Aurum;

@Mixin(EntityShadowBatch.class)
public class MixinEntityShadowBatch {
    @Inject(method = "record", at = @At("HEAD"), cancellable = true)
    private void aurum$declineWhileShaderPackLoaded(World world, Entity entity, double dx, double dy, double dz,
                                                    float opacity, float tickDelta, float shadowSize,
                                                    CallbackInfoReturnable<Boolean> cir) {
        if (Aurum.getCurrentPack().isPresent()) {
            cir.setReturnValue(false);
        }
    }
}
