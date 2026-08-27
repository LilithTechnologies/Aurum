package re.lilith.aurum.pipeline.pathways.postprocess;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.platform.GlStateManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
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
import re.lilith.aurum.shaderpack.ComputeSource;
import re.lilith.aurum.shaderpack.program.ProgramDirectives;
import re.lilith.aurum.shaderpack.program.ProgramSource;
import re.lilith.aurum.targets.render.RenderTarget;
import re.lilith.aurum.targets.render.RenderTargets;
import re.lilith.aurum.uniforms.utility.FrameUpdateNotifier;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class CompositeRenderer extends PostProcessRenderer {
    private final ImmutableList<Pass> passes;
    private final ImmutableSet<Integer> flippedAtLeastOnceFinal;

    public CompositeRenderer(ProgramSource[] sources, ComputeSource[][] computes, RenderTargets renderTargets,
                             IntSupplier noiseTexture, FrameUpdateNotifier updateNotifier,
                             CenterDepthSampler centerDepthSampler, BufferFlipper bufferFlipper,
                             Supplier<ShadowRenderTargets> shadowTargetsSupplier,
                             Object2ObjectMap<String, IntSupplier> customTextureIds, ImmutableMap<Integer, Boolean> explicitPreFlips,
                             CustomUniforms customUniforms, GlImage[] customImages) {
        this.noiseTexture = noiseTexture;
        this.updateNotifier = updateNotifier;
        this.centerDepthSampler = centerDepthSampler;
        this.renderTargets = renderTargets;
        this.customTextureIds = customTextureIds;
        this.customUniforms = customUniforms;
        this.customImages = customImages;

        final ImmutableList.Builder<Pass> passes = ImmutableList.builder();
        final ImmutableSet.Builder<Integer> flippedAtLeastOnce = new ImmutableSet.Builder<>();

        explicitPreFlips.forEach((buffer, shouldFlip) -> {
            if (shouldFlip) {
                bufferFlipper.flip(buffer);
                // NB: Flipping deferred_pre or composite_pre does NOT cause the "flippedAtLeastOnce" flag to trigger
            }
        });

        for (int i = 0; i < sources.length; i++) {
            ProgramSource source = sources[i];

            ImmutableSet<Integer> flipped = bufferFlipper.snapshot();
            ImmutableSet<Integer> flippedAtLeastOnceSnapshot = flippedAtLeastOnce.build();

            if (source == null || !source.isValid()) {
                if (computes[i] != null) {
                    ComputeOnlyPass pass = new ComputeOnlyPass();
                    pass.computes = createComputes(computes[i], flipped, flippedAtLeastOnceSnapshot, shadowTargetsSupplier);
                    passes.add(pass);
                }
                continue;
            }

            Pass pass = new Pass();
            ProgramDirectives directives = source.getDirectives();

            pass.program = createProgram(source, flipped, flippedAtLeastOnceSnapshot, shadowTargetsSupplier);
            pass.computes = createComputes(computes[i], flipped, flippedAtLeastOnceSnapshot, shadowTargetsSupplier);
            int[] drawBuffers = directives.getDrawBuffers();

            GlFramebuffer framebuffer = renderTargets.createColorFramebuffer(flipped, drawBuffers);

            int passWidth = 0, passHeight = 0;

            // Flip the buffers that this shader wrote to, and set pass width and height
            ImmutableMap<Integer, Boolean> explicitFlips = directives.getExplicitFlips();

            for (int buffer : drawBuffers) {
                RenderTarget target = renderTargets.get(buffer);
                if ((passWidth > 0 && passWidth != target.getWidth()) || (passHeight > 0 && passHeight != target.getHeight())) {
                    throw new IllegalStateException("Pass widths must match");
                }
                passWidth = target.getWidth();
                passHeight = target.getHeight();

                // compare with boxed Boolean objects to avoid NPEs
                if (explicitFlips.get(buffer) == Boolean.FALSE) {
                    continue;
                }

                bufferFlipper.flip(buffer);
                flippedAtLeastOnce.add(buffer);
            }

            explicitFlips.forEach((buffer, shouldFlip) -> {
                if (shouldFlip) {
                    bufferFlipper.flip(buffer);
                    flippedAtLeastOnce.add(buffer);
                }
            });

            pass.drawBuffers = directives.getDrawBuffers();
            pass.viewWidth = passWidth;
            pass.viewHeight = passHeight;
            pass.stageReadsFromAlt = flipped;
            pass.framebuffer = framebuffer;
            pass.viewportScale = directives.getViewportScale();
            pass.mipmappedBuffers = directives.getMipmappedBuffers();
            pass.flippedAtLeastOnce = flippedAtLeastOnceSnapshot;

            passes.add(pass);
        }

        this.passes = passes.build();
        this.flippedAtLeastOnceFinal = flippedAtLeastOnce.build();

        GL30.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, 0);
    }

    public ImmutableSet<Integer> getFlippedAtLeastOnceFinal() {
        return this.flippedAtLeastOnceFinal;
    }

    public void recalculateSizes() {
        for (Pass pass : passes) {
            if (pass instanceof ComputeOnlyPass) {
                continue;
            }
            int passWidth = 0, passHeight = 0;
            for (int buffer : pass.drawBuffers) {
                RenderTarget target = renderTargets.get(buffer);
                if ((passWidth > 0 && passWidth != target.getWidth()) || (passHeight > 0 && passHeight != target.getHeight())) {
                    throw new IllegalStateException("Pass widths must match");
                }
                passWidth = target.getWidth();
                passHeight = target.getHeight();
            }
            renderTargets.destroyFramebuffer(pass.framebuffer);
            pass.framebuffer = renderTargets.createColorFramebuffer(pass.stageReadsFromAlt, pass.drawBuffers);
            pass.viewWidth = passWidth;
            pass.viewHeight = passHeight;
        }
    }

    private static class Pass {
        int[] drawBuffers;
        int viewWidth;
        int viewHeight;
        Program program;
        ComputeProgram[] computes;
        GlFramebuffer framebuffer;
        ImmutableSet<Integer> flippedAtLeastOnce;
        ImmutableSet<Integer> stageReadsFromAlt;
        ImmutableSet<Integer> mipmappedBuffers;
        float viewportScale;

        protected void destroy() {
            this.program.destroy();
            for (ComputeProgram compute : this.computes) {
                if (compute != null) {
                    compute.destroy();
                }
            }
        }
    }

    private static class ComputeOnlyPass extends Pass {
        @Override
        protected void destroy() {
            for (ComputeProgram compute : this.computes) {
                if (compute != null) {
                    compute.destroy();
                }
            }
        }
    }

    public void renderAll() {
        GlStateManager.disableBlend();
        GlStateManager.disableAlphaTest();

        FullScreenQuadRenderer.INSTANCE.begin();

        for (Pass renderPass : passes) {
            boolean ranCompute = false;
            for (ComputeProgram computeProgram : renderPass.computes) {
                if (computeProgram != null) {
                    ranCompute = true;
                    Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
                    computeProgram.dispatch(main.viewportWidth, main.viewportHeight);
                }
            }

            if (ranCompute) {
                AurumRenderSystem.memoryBarrier(40);
            }

            Program.unbind();

            if (renderPass instanceof ComputeOnlyPass) {
                continue;
            }

            if (!renderPass.mipmappedBuffers.isEmpty()) {
                GlStateManager.activeTexture(GL15C.GL_TEXTURE0);

                for (int index : renderPass.mipmappedBuffers) {
                    setupMipmapping(CompositeRenderer.this.renderTargets.get(index), renderPass.stageReadsFromAlt.contains(index));
                }
            }

            float scaledWidth = renderPass.viewWidth * renderPass.viewportScale;
            float scaledHeight = renderPass.viewHeight * renderPass.viewportScale;
            GlStateManager.viewport(0, 0, (int) scaledWidth, (int) scaledHeight);

            renderPass.framebuffer.bind();
            renderPass.program.use();
            customUniforms.push(renderPass.program);

            FullScreenQuadRenderer.INSTANCE.renderQuad();
        }

        FullScreenQuadRenderer.end();

        // Make sure to reset the viewport to how it was before... Otherwise weird issues could occur.
        // Also bind the "main" framebuffer if it isn't already bound.
        MinecraftClient.getInstance().getFramebuffer().bind(true);
        ProgramUniforms.clearActiveUniforms();
        ProgramSamplers.clearActiveSamplers();
        GL20.glUseProgram(0);

        // NB: Unbinding all of these textures is necessary for proper shaderpack reloading.
        for (int i = 0; i < SamplerLimits.get().getMaxTextureUnits(); i++) {
            // Unbind all textures that we may have used.
            // NB: This is necessary for shader pack reloading to work propely
            GlStateManager.activeTexture(GL15C.GL_TEXTURE0 + i);
            GlStateManager.bindTexture(0);
        }

        GlStateManager.activeTexture(GL15C.GL_TEXTURE0);
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

    public void destroy() {
        for (Pass renderPass : passes) {
            renderPass.destroy();
        }
    }
}