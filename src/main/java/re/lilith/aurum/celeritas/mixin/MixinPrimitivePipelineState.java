package re.lilith.aurum.celeritas.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "dev.rdh.argentum.impl.render.terrain.PrimitiveRenderPassConfigurationBuilder$PrimitivePipelineState")
public class MixinPrimitivePipelineState {
    @Redirect(method = "setup", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDisable(I)V"))
    private void aurum$routeAlphaTestDisable(int cap) {
        if (cap == GL11.GL_ALPHA_TEST) {
            GlStateManager.disableAlphaTest();
        } else {
            GL11.glDisable(cap);
        }
    }

    @Redirect(method = "clear", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glEnable(I)V"))
    private void aurum$routeAlphaTestEnable(int target) {
        if (target == GL11.GL_ALPHA_TEST) {
            GlStateManager.enableAlphaTest();
        } else {
            GL11.glEnable(target);
        }
    }
}
