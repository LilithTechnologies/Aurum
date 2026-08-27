package re.lilith.aurum.mixin;

import com.mojang.blaze3d.platform.GLX;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.GlDebug;

@Mixin(GLX.class)
public class MixinGLX {
    @Inject(method = "createContext", at = @At("RETURN"))
    private static void aurum$onRendererInit(CallbackInfo ci) {
        GlDebug.initRenderer();
        AurumRenderSystem.initRenderer();
        Aurum.onRenderSystemInit();
    }
}
