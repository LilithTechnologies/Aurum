package re.lilith.aurum.gl.blending;

import com.mojang.blaze3d.platform.GlStateManager;
import re.lilith.aurum.mixin.access.GlStateManagerAccessor;

public class DepthColorStorage {
    private static boolean originalDepthEnable;
    private static ColorMask originalColor;
    private static boolean depthColorLocked;

    private static Boolean deferredDepthEnable;
    private static ColorMask deferredColor;

    public static boolean isDepthColorLocked() {
        return depthColorLocked;
    }

    public static void disableDepthColor() {
        if (!depthColorLocked) {
            // Only save the previous state if the depth and color mask wasn't already locked
            GlStateManager.ColorMask colorMask = GlStateManagerAccessor.getCOLOR_MASK();
            GlStateManager.DepthTestState depthState = GlStateManagerAccessor.getDEPTH();

            originalDepthEnable = depthState.mask;
            originalColor = new ColorMask(colorMask.red, colorMask.green, colorMask.blue, colorMask.alpha);
            deferredDepthEnable = null;
            deferredColor = null;
        }

        depthColorLocked = false;

        GlStateManager.depthMask(false);
        GlStateManager.colorMask(false, false, false, false);

        depthColorLocked = true;
    }

    public static void deferDepthEnable(boolean enabled) {
        deferredDepthEnable = enabled;
    }

    public static void deferColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        deferredColor = new ColorMask(red, green, blue, alpha);
    }

    public static void unlockDepthColor() {
        if (!depthColorLocked) {
            return;
        }

        depthColorLocked = false;

        boolean depthEnable = deferredDepthEnable != null ? deferredDepthEnable : originalDepthEnable;
        ColorMask color = deferredColor != null ? deferredColor : originalColor;

        GlStateManager.depthMask(depthEnable);
        GlStateManager.colorMask(color.red(), color.green(), color.blue(), color.alpha());
    }
}
