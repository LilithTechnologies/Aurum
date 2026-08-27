package re.lilith.aurum.mixin.pipeline;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.targets.depth.DepthAttachedFramebuffer;

import java.nio.IntBuffer;

/**
 * Allows Aurum to detect when the depth texture was re-created, so we can re-attach it
 * to the shader framebuffers. See DeferredWorldRenderingPipeline and RenderTargets.
 */
@Mixin(Framebuffer.class)
public class MixinFramebuffer implements DepthAttachedFramebuffer {
    @Shadow
    public boolean useDepthAttachment;

    @Unique
    public int aurum$depthTextureId = -1;

    @Unique
    private int aurum$depthBufferVersion;

    @Unique
    private int aurum$colorBufferVersion;


    @Inject(method = "delete()V", at = @At("HEAD"))
    private void aurum$onDestroyBuffers(CallbackInfo ci) {
        aurum$depthBufferVersion++;
        aurum$colorBufferVersion++;
    }

    @Override
    public int aurum$getDepthBufferVersion() {
        return aurum$depthBufferVersion;
    }

    @Override
    public int aurum$getColorBufferVersion() {
        return aurum$colorBufferVersion;
    }

    @Override
    public int getAurum$depthTextureId() {
        return this.aurum$depthTextureId;
    }

    @Inject(method = "delete()V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gl/Framebuffer;depthAttachment:I", shift = At.Shift.BEFORE, ordinal = 0, opcode = Opcodes.GETFIELD), remap = false)
    private void aurum$deleteDepthBuffer(CallbackInfo ci) {
        if (this.aurum$depthTextureId > -1) {
            GlStateManager.deleteTexture(this.aurum$depthTextureId);
            this.aurum$depthTextureId = -1;
        }
    }

    @Inject(method = "attachTexture(II)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gl/Framebuffer;depthAttachment:I", shift = At.Shift.BEFORE, ordinal = 0, opcode = Opcodes.PUTFIELD))
    private void aurum$createDepthTextureID(int width, int height, CallbackInfo ci) {
        if (this.useDepthAttachment) {
            this.aurum$depthTextureId = GL11.glGenTextures();
        }
    }

    @Inject(method = "attachTexture", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gl/Framebuffer;useDepthAttachment:Z", shift = At.Shift.BEFORE, ordinal = 1, opcode = Opcodes.GETFIELD))
    private void aurum$createDepthTexture(int width, int height, CallbackInfo ci) {
        if (this.useDepthAttachment) {
            if (this.aurum$depthTextureId == -1) {
                this.aurum$depthTextureId = GL11.glGenTextures();
            }
            GlStateManager.bindTexture(this.aurum$depthTextureId);

            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, 0);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_DEPTH_COMPONENT, width, height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (IntBuffer) null);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, this.aurum$depthTextureId, 0);
        }
    }
}
