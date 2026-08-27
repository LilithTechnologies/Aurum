package re.lilith.aurum.mixin.pipeline;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.gl.blending.BlendModeStorage;

@Mixin(GlStateManager.class)
public class MixinGlStateManager_BlendOverride {
    @Inject(method = "disableBlend", at = @At("HEAD"), cancellable = true)
    private static void aurum$blendDisableLock(CallbackInfo ci) {
        if (BlendModeStorage.isBlendLocked()) {
            BlendModeStorage.deferBlendModeToggle(false);
            ci.cancel();
        }
    }

    @Inject(method = "enableBlend", at = @At("HEAD"), cancellable = true)
    private static void aurum$blendEnableLock(CallbackInfo ci) {
        if (BlendModeStorage.isBlendLocked()) {
            BlendModeStorage.deferBlendModeToggle(true);
            ci.cancel();
        }
    }

    @Inject(method = "blendFunc", at = @At("HEAD"), cancellable = true)
    private static void aurum$blendFuncLock(int srcFactor, int dstFactor, CallbackInfo ci) {
        if (BlendModeStorage.isBlendLocked()) {
            BlendModeStorage.deferBlendFunc(srcFactor, dstFactor, srcFactor, dstFactor);
            ci.cancel();
        }
    }

    @Inject(method = "blendFuncSeparate", at = @At("HEAD"), cancellable = true)
    private static void aurum$blendFuncSeparateLock(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha, CallbackInfo ci) {
        if (BlendModeStorage.isBlendLocked()) {
            BlendModeStorage.deferBlendFunc(srcRgb, dstRgb, srcAlpha, dstAlpha);
            ci.cancel();
        }
    }
}
