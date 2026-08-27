package re.lilith.aurum.gl.blending;

import com.mojang.blaze3d.platform.GlStateManager;
import re.lilith.aurum.mixin.access.GlStateManagerAccessor;
import re.lilith.aurum.mixin.state.BooleanStateAccessor;

public class AlphaTestStorage {
    private static boolean originalAlphaTestEnable;
    private static AlphaTest originalAlphaTest;
    private static boolean alphaTestLocked;

    private static Boolean deferredAlphaTestEnable;
    private static AlphaTest deferredAlphaTest;

    public static boolean isAlphaTestLocked() {
        return alphaTestLocked;
    }

    public static void overrideAlphaTest(AlphaTest override) {
        if (!alphaTestLocked) {
            // Only save the previous state if the alpha test wasn't already locked
            GlStateManager.AlphaTestState alphaState = GlStateManagerAccessor.getALPHA_TEST();

            originalAlphaTestEnable = ((BooleanStateAccessor) alphaState.capState).isEnabled();
            originalAlphaTest = new AlphaTest(AlphaTestFunction.fromGlId(alphaState.func).get(), alphaState.ref);
            deferredAlphaTestEnable = null;
            deferredAlphaTest = null;
        }

        alphaTestLocked = false;

        if (override == null) {
            GlStateManager.disableAlphaTest();
        } else {
            GlStateManager.enableAlphaTest();
            GlStateManager.alphaFunc(override.getFunction().getGlId(), override.getReference());
        }

        alphaTestLocked = true;
    }

    public static void deferAlphaTestToggle(boolean enabled) {
        deferredAlphaTestEnable = enabled;
    }

    public static void deferAlphaFunc(int function, float reference) {
        deferredAlphaTest = new AlphaTest(AlphaTestFunction.fromGlId(function).get(), reference);
    }

    public static void restoreAlphaTest() {
        if (!alphaTestLocked) {
            return;
        }

        alphaTestLocked = false;

        boolean enable = deferredAlphaTestEnable != null ? deferredAlphaTestEnable : originalAlphaTestEnable;
        AlphaTest alphaTest = deferredAlphaTest != null ? deferredAlphaTest : originalAlphaTest;

        if (enable) {
            GlStateManager.enableAlphaTest();
        } else {
            GlStateManager.disableAlphaTest();
        }

        GlStateManager.alphaFunc(alphaTest.getFunction().getGlId(), alphaTest.getReference());
    }
}
