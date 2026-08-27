package re.lilith.aurum.mixin.pipeline;

import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.gbuffer.state.StateTracker;
import re.lilith.aurum.pipeline.WorldRenderingPhase;
import re.lilith.aurum.pipeline.WorldRenderingPipeline;
import re.lilith.aurum.pipeline.state.CapturedRenderingState;
import re.lilith.aurum.uniforms.utility.EntityColorState;
import re.lilith.aurum.uniforms.utility.EntityIdHelper;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {
    @Inject(method = "method_6915", at = @At("HEAD"))
    private void aurum$beginEntity(Entity entity, float tickDelta, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        aurum$setPhase(WorldRenderingPhase.ENTITIES);

        StateTracker.INSTANCE.overlaySampler = true;
        aurum$refreshInputs();

        CapturedRenderingState.INSTANCE.setCurrentEntity(EntityIdHelper.getEntityId(entity));
    }

    @Inject(method = "method_6915", at = @At("RETURN"))
    private void aurum$endEntity(Entity entity, float tickDelta, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        aurum$setPhase(WorldRenderingPhase.NONE);

        EntityColorState.reset();
        CapturedRenderingState.INSTANCE.setCurrentEntity(-1);

        StateTracker.INSTANCE.overlaySampler = false;
        aurum$refreshInputs();
    }

    @Unique
    private static void aurum$refreshInputs() {
        WorldRenderingPipeline pipeline = Aurum.getPipelineManager().getPipelineNullable();

        if (pipeline != null) {
            pipeline.setInputs(StateTracker.INSTANCE.getInputs());
        }
    }

    @Unique
    private static void aurum$setPhase(WorldRenderingPhase phase) {
        WorldRenderingPipeline pipeline = Aurum.getPipelineManager().getPipelineNullable();

        if (pipeline != null) {
            pipeline.setPhase(phase);
        }
    }

    @Inject(method = "shouldRenderShadows", at = @At("RETURN"), cancellable = true)
    private void aurum$shouldRenderShadows(CallbackInfoReturnable<Boolean> cir) {
        WorldRenderingPipeline pipeline = Aurum.getPipelineManager().getPipelineNullable();

        if (pipeline != null && pipeline.shouldDisableVanillaEntityShadows()) {
            cir.setReturnValue(false);
        }
    }
}
