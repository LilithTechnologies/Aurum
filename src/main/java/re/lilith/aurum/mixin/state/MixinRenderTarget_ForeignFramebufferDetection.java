package re.lilith.aurum.mixin.state;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.Aurum;

@Mixin(Framebuffer.class)
public class MixinRenderTarget_ForeignFramebufferDetection {
    @Inject(method = "bind", at = @At("RETURN"))
    private void aurum$onBindFramebuffer(boolean bl, CallbackInfo ci) {
        boolean mainBound = this == (Object) MinecraftClient.getInstance().getFramebuffer();

        Aurum.getPipelineManager().getPipeline()
                .ifPresent(pipeline -> pipeline.getRenderTargetStateListener().setIsMainBound(mainBound));
    }
}
