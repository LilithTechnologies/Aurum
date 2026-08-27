package re.lilith.aurum.mixin.vertices;

import net.minecraft.client.font.TextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.pipeline.WorldRenderingPipeline;

@Mixin(TextRenderer.class)
public class MixinTextRenderer {
    @Inject(method = "draw(Ljava/lang/String;III)I", at = @At("HEAD"))
    private void aurum$syncBeforeDraw(String text, int x, int y, int color, CallbackInfoReturnable<Integer> cir) {
        aurum$sync();
    }

    @Inject(method = "drawWithShadow(Ljava/lang/String;FFI)I", at = @At("HEAD"))
    private void aurum$syncBeforeDrawWithShadow(String text, float x, float y, int color, CallbackInfoReturnable<Integer> cir) {
        aurum$sync();
    }

    @Unique
    private static void aurum$sync() {
        Aurum.getPipelineManager().getPipeline().ifPresent(WorldRenderingPipeline::syncProgram);
        AurumRenderSystem.resetPackGenericAttributes();
    }
}
