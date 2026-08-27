package re.lilith.aurum.mixin.texture;

import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.pipeline.WorldRenderingPipeline;
import re.lilith.aurum.texture.format.TextureFormatLoader;
import re.lilith.aurum.texture.pbr.PBRTextureManager;

@Mixin(TextureManager.class)
public class MixinTextureManager {
    @Inject(method = "reload", at = @At("TAIL"))
    private void aurum$onTailReloadLambda(ResourceManager resourceManager, CallbackInfo ci) {
        TextureFormatLoader.reload(resourceManager);
        PBRTextureManager.INSTANCE.clear();
    }

    @Inject(method = "bindTexture", at = @At("HEAD"))
    private void aurum$syncBeforeBindTexture(Identifier id, CallbackInfo ci) {
        Aurum.getPipelineManager().getPipeline().ifPresent(WorldRenderingPipeline::syncProgram);
    }
}
