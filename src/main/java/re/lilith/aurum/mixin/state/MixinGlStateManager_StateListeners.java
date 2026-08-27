package re.lilith.aurum.mixin.state;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.gl.state.StateUpdateNotifiers;

@Mixin(GlStateManager.class)
public class MixinGlStateManager_StateListeners {
    @Unique
    private static Runnable fogToggleListener;
    @Unique
    private static Runnable fogModeListener;
    @Unique
    private static Runnable fogStartListener;
    @Unique
    private static Runnable fogEndListener;
    @Unique
    private static Runnable fogDensityListener;
    @Unique
    private static Runnable alphaTestListener;
    @Unique
    private static Runnable alphaFuncListener;
    @Unique
    private static Runnable blendFuncListener;
    @Unique
    private static Runnable colorModulatorListener;

    @Inject(method = {"enableFog", "disableFog()V"}, at = @At("RETURN"))
    private static void aurum$onFogToggle(CallbackInfo ci) {
        if (fogToggleListener != null) {
            fogToggleListener.run();
        }
    }

    @Inject(method = "fogMode(I)V", at = @At(value = "FIELD", target = "com/mojang/blaze3d/platform/GlStateManager$FogState.mode : I", shift = At.Shift.AFTER))
    private static void aurum$onFogMode(int mode, CallbackInfo ci) {
        if (fogModeListener != null) {
            fogModeListener.run();
        }
    }

    @Inject(method = "fogDensity(F)V", at = @At(value = "FIELD", target = "com/mojang/blaze3d/platform/GlStateManager$FogState.density : F", shift = At.Shift.AFTER))
    private static void aurum$onFogDensity(float density, CallbackInfo ci) {
        if (fogDensityListener != null) {
            fogDensityListener.run();
        }
    }

    @Inject(method = "fogStart(F)V", at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/platform/GlStateManager$FogState;start:F", shift = At.Shift.AFTER))
    private static void aurum$onFogStart(float density, CallbackInfo ci) {
        if (fogStartListener != null) {
            fogStartListener.run();
        }
    }

    @Inject(method = "fogEnd(F)V", at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/platform/GlStateManager$FogState;end:F", shift = At.Shift.AFTER))
    private static void aurum$onFogEnd(float density, CallbackInfo ci) {
        if (fogEndListener != null) {
            fogEndListener.run();
        }
    }

    @Inject(method = {"enableAlphaTest", "disableAlphaTest"}, at = @At("RETURN"))
    private static void aurum$onAlphaTestToggle(CallbackInfo ci) {
        if (alphaTestListener != null) {
            alphaTestListener.run();
        }

        if (alphaFuncListener != null) {
            alphaFuncListener.run();
        }
    }

    @Inject(method = "alphaFunc", at = @At("RETURN"))
    private static void aurum$onAlphaFunc(int function, float reference, CallbackInfo ci) {
        if (alphaTestListener != null) {
            alphaTestListener.run();
        }

        if (alphaFuncListener != null) {
            alphaFuncListener.run();
        }
    }

    @Inject(method = "blendFunc", at = @At("RETURN"))
    private static void aurum$onBlendFunc(int srcRgb, int dstRgb, CallbackInfo ci) {
        if (blendFuncListener != null) {
            blendFuncListener.run();
        }
    }

    @Inject(method = "blendFuncSeparate", at = @At("RETURN"))
    private static void aurum$onBlendFuncSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha, CallbackInfo ci) {
        if (blendFuncListener != null) {
            blendFuncListener.run();
        }
    }

    @Inject(method = "color(FFFF)V", at = @At("RETURN"))
    private static void aurum$onColor(float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (colorModulatorListener != null) {
            colorModulatorListener.run();
        }
    }

    static {
        StateUpdateNotifiers.fogToggleNotifier = listener -> fogToggleListener = listener;
        StateUpdateNotifiers.fogModeNotifier = listener -> fogModeListener = listener;
        StateUpdateNotifiers.fogStartNotifier = listener -> fogStartListener = listener;
        StateUpdateNotifiers.fogEndNotifier = listener -> fogEndListener = listener;
        StateUpdateNotifiers.fogDensityNotifier = listener -> fogDensityListener = listener;
        StateUpdateNotifiers.alphaTestNotifier = listener -> alphaTestListener = listener;
        StateUpdateNotifiers.alphaFuncNotifier = listener -> alphaFuncListener = listener;
        StateUpdateNotifiers.blendFuncNotifier = listener -> blendFuncListener = listener;
        StateUpdateNotifiers.colorModulatorNotifier = listener -> colorModulatorListener = listener;
    }
}
