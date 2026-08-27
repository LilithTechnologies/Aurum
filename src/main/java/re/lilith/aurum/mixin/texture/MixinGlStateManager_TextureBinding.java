package re.lilith.aurum.mixin.texture;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.texture.TextureInfoCache;
import re.lilith.aurum.texture.TextureTracker;
import re.lilith.aurum.texture.pbr.PBRTextureManager;

@Mixin(GlStateManager.class)
public class MixinGlStateManager_TextureBinding {
    @Inject(method = "bindTexture(I)V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glBindTexture(II)V", shift = Shift.AFTER, remap = false))
    private static void aurum$onBindTexture(int id, CallbackInfo ci) {
        TextureTracker.INSTANCE.onBindTexture(id);
    }

    @Inject(method = "deleteTexture(I)V", at = @At("TAIL"))
    private static void aurum$onDeleteTexture(int id, CallbackInfo ci) {
        aurum$onDeleteTexture(id);
    }

    @Unique
    private static void aurum$onDeleteTexture(int id) {
        TextureTracker.INSTANCE.onDeleteTexture(id);
        TextureInfoCache.INSTANCE.onDeleteTexture(id);
        PBRTextureManager.INSTANCE.onDeleteTexture(id);
    }
}
