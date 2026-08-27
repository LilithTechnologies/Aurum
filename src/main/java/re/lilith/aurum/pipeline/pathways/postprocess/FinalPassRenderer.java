package re.lilith.aurum.pipeline.pathways.postprocess;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.platform.GlStateManager;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.*;
import re.lilith.aurum.expression.CustomUniforms;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.GlFramebuffer;
import re.lilith.aurum.gl.image.GlImage;
import re.lilith.aurum.gl.program.ComputeProgram;
import re.lilith.aurum.gl.program.Program;
import re.lilith.aurum.gl.program.ProgramSamplers;
import re.lilith.aurum.gl.program.ProgramUniforms;
import re.lilith.aurum.gl.sampler.SamplerLimits;
import re.lilith.aurum.pipeline.pathways.shadows.ShadowRenderTargets;
import re.lilith.aurum.shaderpack.program.ProgramDirectives;
import re.lilith.aurum.shaderpack.program.ProgramSet;
import re.lilith.aurum.targets.depth.DepthAttachedFramebuffer;
import re.lilith.aurum.targets.render.RenderTarget;
import re.lilith.aurum.targets.render.RenderTargets;
import re.lilith.aurum.uniforms.utility.FrameUpdateNotifier;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class FinalPassRenderer extends PostProcessRenderer {
    @Nullable
    private final Pass finalPass;
    private final ImmutableList<SwapPass> swapPasses;
    private final GlFramebuffer baseline;
    private final GlFramebuffer colorHolder;
    private int lastColorTextureId;
    private int lastColorTextureVersion;

    // TODO: The length of this argument list is getting a bit ridiculous
    public FinalPassRenderer(ProgramSet pack, RenderTargets renderTargets, IntSupplier noiseTexture,
                             FrameUpdateNotifier updateNotifier, ImmutableSet<Integer> flippedBuffers,
                             CenterDepthSampler centerDepthSampler,
                             Supplier<ShadowRenderTargets> shadowTargetsSupplier,
                             Object2ObjectMap<String, IntSupplier> customTextureIds,
                             ImmutableSet<Integer> flippedAtLeastOnce,
                             CustomUniforms customUniforms, GlImage[] customImages) {
        this.updateNotifier = updateNotifier;
        this.centerDepthSampler = centerDepthSampler;
        this.customTextureIds = customTextureIds;
        this.customUniforms = customUniforms;
        this.customImages = customImages;

        this.noiseTexture = noiseTexture;
        this.renderTargets = renderTargets;
        this.finalPass = pack.getCompositeFinal().map(source -> {
            Pass pass = new Pass();
            ProgramDirectives directives = source.getDirectives();

            pass.program = createProgram(source, flippedBuffers, flippedAtLeastOnce, shadowTargetsSupplier);
            pass.computes = createComputes(pack.getFinalCompute(), flippedBuffers, flippedAtLeastOnce, shadowTargetsSupplier);
            pass.stageReadsFromAlt = flippedBuffers;
            pass.mipmappedBuffers = directives.getMipmappedBuffers();

            return pass;
        }).orElse(null);

        IntList buffersToBeCleared = pack.getPackDirectives().getRenderTargetDirectives().getBuffersToBeCleared();

        // The name of this method might seem a bit odd here, but we want a framebuffer with color attachments that line
        // up with whatever was written last (since we're reading from these framebuffers) instead of trying to create
        // a framebuffer with color attachments different from what was written last (as we do with normal composite
        // passes that write to framebuffers).
        this.baseline = renderTargets.createGbufferFramebuffer(flippedBuffers, new int[]{0});
        this.colorHolder = new GlFramebuffer();
        this.lastColorTextureId = MinecraftClient.getInstance().getFramebuffer().colorAttachment;
        this.lastColorTextureVersion = ((DepthAttachedFramebuffer) MinecraftClient.getInstance().getFramebuffer()).aurum$getColorBufferVersion();
        this.colorHolder.addColorAttachment(0, lastColorTextureId);

        // TODO: We don't actually fully swap the content, we merely copy it from alt to main
        // This works for the most part, but it's not perfect. A better approach would be creating secondary
        // framebuffers for every other frame, but that would be a lot more complex...
        ImmutableList.Builder<SwapPass> swapPasses = ImmutableList.builder();

        flippedBuffers.forEach((i) -> {
            int target = i;

            if (buffersToBeCleared.contains(target)) {
                return;
            }

            SwapPass swap = new SwapPass();
            RenderTarget target1 = renderTargets.get(target);
            swap.target = target;
            swap.width = target1.getWidth();
            swap.height = target1.getHeight();
            swap.from = renderTargets.createColorFramebuffer(ImmutableSet.of(), new int[]{target});
            // NB: This is handled in RenderTargets now.
            //swap.from.readBuffer(target);
            swap.targetTexture = renderTargets.get(target).getMainTexture();

            swapPasses.add(swap);
        });

        this.swapPasses = swapPasses.build();

        GL30.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, 0);
    }

    private static final class Pass {
        Program program;
        ComputeProgram[] computes;
        ImmutableSet<Integer> stageReadsFromAlt;
        ImmutableSet<Integer> mipmappedBuffers;

        private void destroy() {
            this.program.destroy();
        }
    }

    private static final class SwapPass {
        public int target;
        public int width;
        public int height;
        GlFramebuffer from;
        int targetTexture;
    }

    public void renderFinalPass() {
        GlStateManager.disableBlend();
        GlStateManager.disableAlphaTest();
        GlStateManager.depthMask(false);

        final Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
        final int baseWidth = main.viewportWidth;
        final int baseHeight = main.viewportHeight;

        // Note that since DeferredWorldRenderingPipeline uses the depth texture of the main Minecraft framebuffer,
        // we'll be writing to that depth buffer directly automatically and won't need to futz around with copying
        // depth buffer content.
        //
        // Previously, we had our own depth texture and then copied its content to the main Minecraft framebuffer.
        // This worked with vanilla, but broke with mods that used the stencil buffer.
        //
        // This approach is a fairly succinct solution to the issue of having to deal with the main Minecraft
        // framebuffer potentially having a depth-stencil buffer or similar - we'll automatically enable that to
        // work properly since we re-use the depth buffer instead of trying to make our own.
        //
        // This is not a concern for depthtex1 / depthtex2 since the copy call extracts the depth values, and the
        // shader pack only ever uses them to read the depth values.
        if (((DepthAttachedFramebuffer) main).aurum$getColorBufferVersion() != lastColorTextureVersion || main.colorAttachment != lastColorTextureId) {
            lastColorTextureVersion = ((DepthAttachedFramebuffer) main).aurum$getColorBufferVersion();
            this.lastColorTextureId = main.colorAttachment;
            colorHolder.addColorAttachment(0, lastColorTextureId);
        }

        if (this.finalPass != null) {
            // If there is a final pass, we use the shader-based full screen quad rendering pathway instead
            // of just copying the color buffer.

            colorHolder.bind();

            FullScreenQuadRenderer.INSTANCE.begin();

            for (ComputeProgram computeProgram : finalPass.computes) {
                if (computeProgram != null) {
                    computeProgram.dispatch(baseWidth, baseHeight);
                }
            }

            AurumRenderSystem.memoryBarrier(40);

            if (!finalPass.mipmappedBuffers.isEmpty()) {
                GlStateManager.activeTexture(GL15C.GL_TEXTURE0);

                for (int index : finalPass.mipmappedBuffers) {
                    setupMipmapping(renderTargets.get(index), finalPass.stageReadsFromAlt.contains(index));
                }
            }

            finalPass.program.use();
            customUniforms.push(finalPass.program);
            FullScreenQuadRenderer.INSTANCE.renderQuad();

            FullScreenQuadRenderer.end();
        } else {
            // If there are no passes, we somehow need to transfer the content of the Aurum color render targets into
            // the main Minecraft framebuffer.
            //
            // Thus, the following call transfers the content of colortex0 into the main Minecraft framebuffer.
            //
            // Note that glCopyTexSubImage2D is not as strict as glBlitFramebuffer, so we don't have to worry about
            // colortex0 having a weird format. This should just work.
            //
            // We could have used a shader here, but it should be about the same performance either way:
            // https://stackoverflow.com/a/23994979/18166885
            this.baseline.bindAsReadBuffer();

            AurumRenderSystem.copyTexSubImage2D(main.colorAttachment, GL11C.GL_TEXTURE_2D, 0, 0, 0, 0, 0, baseWidth, baseHeight);
        }

        GlStateManager.activeTexture(GL15C.GL_TEXTURE0);

        for (int i = 0; i < renderTargets.getRenderTargetCount(); i++) {
            // Reset mipmapping states at the end of the frame.
            resetRenderTarget(renderTargets.get(i));
        }

        for (SwapPass swapPass : swapPasses) {
            // NB: We need to use bind(), not bindAsReadBuffer()... Previously we used bindAsReadBuffer() here which
            //     broke TAA on many packs and on many drivers.
            //
            // Note that glCopyTexSubImage2D reads from the current GL_READ_BUFFER (given by glReadBuffer()) for the
            // current framebuffer bound to GL_FRAMEBUFFER, but that is distinct from the current GL_READ_FRAMEBUFFER,
            // which is what bindAsReadBuffer() binds.
            //
            // Also note that RenderTargets already calls readBuffer(0) for us.
            swapPass.from.bind();

            GlStateManager.bindTexture(swapPass.targetTexture);
            GL11.glCopyTexSubImage2D(GL20C.GL_TEXTURE_2D, 0, 0, 0, 0, 0, swapPass.width, swapPass.height);
        }

        // Make sure to reset the viewport to how it was before... Otherwise weird issues could occur.
        // Also bind the "main" framebuffer if it isn't already bound.
        main.bind(true);
        ProgramUniforms.clearActiveUniforms();
        ProgramSamplers.clearActiveSamplers();
        GL20.glUseProgram(0);

        for (int i = 0; i < SamplerLimits.get().getMaxTextureUnits(); i++) {
            // Unbind all textures that we may have used.
            // NB: This is necessary for shader pack reloading to work properly
            GlStateManager.activeTexture(GL15C.GL_TEXTURE0 + i);
            GlStateManager.bindTexture(0);
        }

        GlStateManager.activeTexture(GL15C.GL_TEXTURE0);
    }

    public void recalculateSwapPassSize() {
        for (SwapPass swapPass : swapPasses) {
            RenderTarget target = renderTargets.get(swapPass.target);
            renderTargets.destroyFramebuffer(swapPass.from);
            swapPass.from = renderTargets.createColorFramebuffer(ImmutableSet.of(), new int[]{swapPass.target});
            swapPass.width = target.getWidth();
            swapPass.height = target.getHeight();
            swapPass.targetTexture = target.getMainTexture();
        }
    }

    private static void setupMipmapping(RenderTarget target, boolean readFromAlt) {
        int texture = readFromAlt ? target.getAltTexture() : target.getMainTexture();

        // TODO: Only generate the mipmap if a valid mipmap hasn't been generated or if we've written to the buffer
        // (since the last mipmap was generated)
        //
        // NB: We leave mipmapping enabled even if the buffer is written to again, this appears to match the
        // behavior of ShadersMod/OptiFine, however I'm not sure if it's desired behavior. It's possible that a
        // program could use mipmapped sampling with a stale mipmap, which probably isn't great. However, the
        // sampling mode is always reset between frames, so this only persists after the first program to use
        // mipmapping on this buffer.
        //
        // Also note that this only applies to one of the two buffers in a render target buffer pair - making it
        // unlikely that this issue occurs in practice with most shader packs.
        AurumRenderSystem.generateMipmaps(texture, GL20C.GL_TEXTURE_2D);

        int filter = GL20C.GL_LINEAR_MIPMAP_LINEAR;
        if (target.getInternalFormat().getPixelFormat().isInteger()) {
            filter = GL20C.GL_NEAREST_MIPMAP_NEAREST;
        }

        AurumRenderSystem.texParameteri(texture, GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MIN_FILTER, filter);
    }

    private static void resetRenderTarget(RenderTarget target) {
        // Resets the sampling mode of the given render target and then unbinds it to prevent accidental sampling of it
        // elsewhere.
        int filter = GL20C.GL_LINEAR;
        if (target.getInternalFormat().getPixelFormat().isInteger()) {
            filter = GL20C.GL_NEAREST;
        }

        AurumRenderSystem.texParameteri(target.getMainTexture(), GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MIN_FILTER, filter);
        AurumRenderSystem.texParameteri(target.getAltTexture(), GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MIN_FILTER, filter);

        GlStateManager.bindTexture(0);
    }

    public void destroy() {
        if (finalPass != null) {
            finalPass.destroy();
        }
    }
}