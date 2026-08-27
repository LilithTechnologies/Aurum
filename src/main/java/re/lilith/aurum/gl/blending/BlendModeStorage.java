package re.lilith.aurum.gl.blending;

import com.mojang.blaze3d.platform.GlStateManager;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.mixin.access.GlStateManagerAccessor;
import re.lilith.aurum.mixin.state.BooleanStateAccessor;

public class BlendModeStorage {
    private static boolean originalBlendEnable;
    private static BlendMode originalBlend;
    private static boolean blendLocked;

    private static Boolean deferredBlendEnable;
    private static BlendMode deferredBlend;

    public static boolean isBlendLocked() {
        return blendLocked;
    }

    public static void overrideBlend(BlendMode override) {
        if (!blendLocked) {
            saveOriginalState();
        }

        blendLocked = false;

        if (override == null) {
            GlStateManager.disableBlend();
        } else {
            GlStateManager.enableBlend();
            GlStateManager.blendFuncSeparate(override.srcRgb(), override.dstRgb(), override.srcAlpha(), override.dstAlpha());
        }

        blendLocked = true;
    }

    public static void overrideBufferBlend(int index, BlendMode override) {
        if (!blendLocked) {
            saveOriginalState();
        }

        if (override == null) {
            AurumRenderSystem.disableBufferBlend(index);
        } else {
            AurumRenderSystem.enableBufferBlend(index);
            AurumRenderSystem.blendFuncSeparatei(index, override.srcRgb(), override.dstRgb(), override.srcAlpha(), override.dstAlpha());
        }

        blendLocked = true;
    }

    private static void saveOriginalState() {
        GlStateManager.BlendFuncState blendState = GlStateManagerAccessor.getBLEND();

        originalBlendEnable = ((BooleanStateAccessor) blendState.capState).isEnabled();
        originalBlend = new BlendMode(blendState.srcFactorRGB, blendState.dstFactorRGB, blendState.srcFactorAlpha, blendState.dstFactorAlpha);
        deferredBlendEnable = null;
        deferredBlend = null;
    }

    public static void deferBlendModeToggle(boolean enabled) {
        deferredBlendEnable = enabled;
    }

    public static void deferBlendFunc(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
        deferredBlend = new BlendMode(srcRgb, dstRgb, srcAlpha, dstAlpha);
    }

    public static void restoreBlend() {
        if (!blendLocked) {
            return;
        }

        blendLocked = false;

        boolean enable = deferredBlendEnable != null ? deferredBlendEnable : originalBlendEnable;
        BlendMode blend = deferredBlend != null ? deferredBlend : originalBlend;

        if (enable) {
            GlStateManager.enableBlend();
        } else {
            GlStateManager.disableBlend();
        }

        GlStateManager.blendFuncSeparate(blend.srcRgb(), blend.dstRgb(), blend.srcAlpha(), blend.dstAlpha());
    }
}
