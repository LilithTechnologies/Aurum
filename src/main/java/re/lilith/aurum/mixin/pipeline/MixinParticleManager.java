package re.lilith.aurum.mixin.pipeline;

import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.pipeline.WorldRenderingPhase;

/**
 * Ensures that all particles are rendered with the textured_lit shader program.
 */
@Mixin(ParticleManager.class)
public class MixinParticleManager {
    @Inject(method = "renderParticles", at = @At("HEAD"))
    private void aurum$beginDrawingParticles(Entity entity, float tickDelta, CallbackInfo ci) {
        Aurum.getPipelineManager().getPipeline().ifPresent(pipeline -> pipeline.setPhase(WorldRenderingPhase.PARTICLES));
    }

    @Inject(method = "renderParticles", at = @At("RETURN"))
    private void aurum$finishDrawingParticles(Entity entity, float tickDelta, CallbackInfo ci) {
        Aurum.getPipelineManager().getPipeline().ifPresent(pipeline -> pipeline.setPhase(WorldRenderingPhase.NONE));
    }
}
