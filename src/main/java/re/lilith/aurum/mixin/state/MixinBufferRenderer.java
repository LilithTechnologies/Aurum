package re.lilith.aurum.mixin.state;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.pipeline.WorldRenderingPipeline;

@Mixin(BufferRenderer.class)
public class MixinBufferRenderer {
    @Inject(method = "draw", at = @At("HEAD"))
    private void aurum$beforeDrawArrays(BufferBuilder builder, CallbackInfo ci) {
        Aurum.getPipelineManager().getPipeline().ifPresent(WorldRenderingPipeline::syncProgram);
    }
}
