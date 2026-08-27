package re.lilith.aurum.mixin.pipeline;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.gl.blending.AlphaTestStorage;

@Mixin(GlStateManager.class)
public class MixinGlStateManager_AlphaTestOverride {
    @Inject(method = "disableAlphaTest", at = @At("HEAD"), cancellable = true)
    private static void aurum$alphaTestDisableLock(CallbackInfo ci) {
        if (AlphaTestStorage.isAlphaTestLocked()) {
            AlphaTestStorage.deferAlphaTestToggle(false);
            ci.cancel();
        }
    }

    @Inject(method = "enableAlphaTest", at = @At("HEAD"), cancellable = true)
    private static void aurum$alphaTestEnableLock(CallbackInfo ci) {
        if (AlphaTestStorage.isAlphaTestLocked()) {
            AlphaTestStorage.deferAlphaTestToggle(true);
            ci.cancel();
        }
    }

    @Inject(method = "alphaFunc", at = @At("HEAD"), cancellable = true)
    private static void aurum$alphaFuncLock(int function, float reference, CallbackInfo ci) {
        if (AlphaTestStorage.isAlphaTestLocked()) {
            AlphaTestStorage.deferAlphaFunc(function, reference);
            ci.cancel();
        }
    }
}
