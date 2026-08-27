package re.lilith.aurum.pipeline.pathways.shadows;

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
import re.lilith.aurum.gl.program.*;
import re.lilith.aurum.pipeline.pathways.postprocess.FullScreenQuadRenderer;
import re.lilith.aurum.pipeline.samplers.AurumImages;
import re.lilith.aurum.pipeline.samplers.AurumSamplers;
import re.lilith.aurum.pipeline.transform.ShaderTransformer;
import re.lilith.aurum.pipeline.transform.patch.PatchShaderType;
import re.lilith.aurum.pipeline.transform.patch.PatchedShaderPrinter;
import re.lilith.aurum.shaderpack.ComputeSource;
import re.lilith.aurum.shaderpack.PackDirectives;
import re.lilith.aurum.shaderpack.PackRenderTargetDirectives;
import re.lilith.aurum.shaderpack.program.ProgramDirectives;
import re.lilith.aurum.shaderpack.program.ProgramSource;
import re.lilith.aurum.targets.render.RenderTarget;
import re.lilith.aurum.uniforms.CommonUniforms;
import re.lilith.aurum.uniforms.utility.FrameUpdateNotifier;

import java.util.Map;
import java.util.Objects;
import java.util.function.IntSupplier;

public class ShadowCompositeRenderer {
    private final ShadowRenderTargets renderTargets;

    private final ImmutableList<Pass> passes;
    private final IntSupplier noiseTexture;
    private final FrameUpdateNotifier updateNotifier;
    private final Object2ObjectMap<String, IntSupplier> customTextureIds;
    private final ImmutableSet<Integer> flippedAtLeastOnceFinal;
    private final CustomUniforms customUniforms;
    private final GlImage[] customImages;

    public ShadowCompositeRenderer(PackDirectives packDirectives, ProgramSource[] sources, ComputeSource[][] computes,
                                   ShadowRenderTargets renderTargets, IntSupplier noiseTexture, FrameUpdateNotifier updateNotifier,
                                   Object2ObjectMap<String, IntSupplier> customTextureIds, ImmutableMap<Integer, Boolean> explicitPreFlips,
                                   CustomUniforms customUniforms, GlImage[] customImages) {
        this.noiseTexture = noiseTexture;
        this.updateNotifier = updateNotifier;
        this.renderTargets = renderTargets;
        this.customTextureIds = customTextureIds;
        this.customUniforms = customUniforms;
        this.customImages = customImages;

        final PackRenderTargetDirectives renderTargetDirectives = packDirectives.getRenderTargetDirectives();
        final Map<Integer, PackRenderTargetDirectives.RenderTargetSettings> renderTargetSettings =
                renderTargetDirectives.getRenderTargetSettings();

        final ImmutableList.Builder<Pass> passes = ImmutableList.builder();
        final ImmutableSet.Builder<Integer> flippedAtLeastOnce = new ImmutableSet.Builder<>();

        explicitPreFlips.forEach((buffer, shouldFlip) -> {
            if (shouldFlip) {
                renderTargets.flip(buffer);
                // NB: Flipping deferred_pre or composite_pre does NOT cause the "flippedAtLeastOnce" flag to trigger
            }
        });

        for (int i = 0; i < sources.length; i++) {
            ProgramSource source = sources[i];

            ImmutableSet<Integer> flipped = renderTargets.snapshot();
            ImmutableSet<Integer> flippedAtLeastOnceSnapshot = flippedAtLeastOnce.build();

            if (source == null || !source.isValid()) {
                if (computes[i] != null) {
                    ComputeOnlyPass pass = new ComputeOnlyPass();
                    pass.computes = createComputes(computes[i], flipped, flippedAtLeastOnceSnapshot);
                    passes.add(pass);
                }
                continue;
            }

            Pass pass = new Pass();
            ProgramDirectives directives = source.getDirectives();

            pass.program = createProgram(source, flipped, flippedAtLeastOnceSnapshot);
            pass.computes = createComputes(computes[i], flipped, flippedAtLeastOnceSnapshot);
            int[] drawBuffers = directives.getDrawBuffers();

            GlFramebuffer framebuffer = renderTargets.createColorFramebuffer(flipped, drawBuffers);

            pass.stageReadsFromAlt = flipped;
            pass.framebuffer = framebuffer;
            pass.viewportScale = directives.getViewportScale();
            pass.mipmappedBuffers = directives.getMipmappedBuffers();
            pass.flippedAtLeastOnce = flippedAtLeastOnceSnapshot;

            passes.add(pass);

            ImmutableMap<Integer, Boolean> explicitFlips = directives.getExplicitFlips();

            // Flip the buffers that this shader wrote to
            for (int buffer : drawBuffers) {
                // compare with boxed Boolean objects to avoid NPEs
                if (explicitFlips.get(buffer) == Boolean.FALSE) {
                    continue;
                }

                renderTargets.flip(buffer);
                flippedAtLeastOnce.add(buffer);
            }

            explicitFlips.forEach((buffer, shouldFlip) -> {
                if (shouldFlip) {
                    renderTargets.flip(buffer);
                    flippedAtLeastOnce.add(buffer);
                }
            });
        }

        this.passes = passes.build();
        this.flippedAtLeastOnceFinal = flippedAtLeastOnce.build();

        GL30.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, 0);
    }

    public ImmutableSet<Integer> getFlippedAtLeastOnceFinal() {
        return this.flippedAtLeastOnceFinal;
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
                    setupMipmapping(renderTargets.get(index), renderPass.stageReadsFromAlt.contains(index));
                }
            }

            float scaledWidth = renderTargets.getResolution() * renderPass.viewportScale;
            float scaledHeight = renderTargets.getResolution() * renderPass.viewportScale;
            GlStateManager.viewport(0, 0, (int) scaledWidth, (int) scaledHeight);

            renderPass.framebuffer.bind();
            renderPass.program.use();
            customUniforms.push(renderPass.program);

            FullScreenQuadRenderer.INSTANCE.renderQuad();
        }

        FullScreenQuadRenderer.end();

        // Make sure to reset the viewport to how it was before... Otherwise, weird issues could occur.
        // Also bind the "main" framebuffer if it isn't already bound.
        MinecraftClient.getInstance().getFramebuffer().bind(true);
        ProgramUniforms.clearActiveUniforms();
        ProgramSamplers.clearActiveSamplers();
        GL20.glUseProgram(0);

        for (int i = 0; i < renderTargets.getRenderTargetCount(); i++) {
            // Reset mipmapping states at the end of the frame.
            if (renderTargets.get(i) != null) {
                resetRenderTarget(renderTargets.get(i));
            }
        }

        GlStateManager.activeTexture(GL15C.GL_TEXTURE0);
    }

    private static void setupMipmapping(RenderTarget target, boolean readFromAlt) {
        if (target == null) {
            return;
        }

        int texture = readFromAlt ? target.getAltTexture() : target.getMainTexture();

        // TODO: Only generate the mipmap if a valid mipmap hasn't been generated or if we've written to the buffer
        // (since the last mipmap was generated)
        AurumRenderSystem.generateMipmaps(texture, GL20C.GL_TEXTURE_2D);

        int filter = GL20C.GL_LINEAR_MIPMAP_LINEAR;
        if (target.getInternalFormat().getPixelFormat().isInteger()) {
            filter = GL20C.GL_NEAREST_MIPMAP_NEAREST;
        }

        AurumRenderSystem.texParameteri(texture, GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MIN_FILTER, filter);
    }

    private static void resetRenderTarget(RenderTarget target) {
        // Resets the sampling mode of the given render target and then unbinds it to prevent accidental sampling of
        // it elsewhere.
        int filter = GL20C.GL_LINEAR;
        if (target.getInternalFormat().getPixelFormat().isInteger()) {
            filter = GL20C.GL_NEAREST;
        }

        AurumRenderSystem.texParameteri(target.getMainTexture(), GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MIN_FILTER, filter);
        AurumRenderSystem.texParameteri(target.getAltTexture(), GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MIN_FILTER, filter);
    }

    // TODO: Don't just copy this from DeferredWorldRenderingPipeline / CompositeRenderer
    private Program createProgram(ProgramSource source, ImmutableSet<Integer> flipped, ImmutableSet<Integer> flippedAtLeastOnceSnapshot) {
        Map<PatchShaderType, String> transformed = ShaderTransformer.patchComposite(
                source.getVertexSource().orElseThrow(NullPointerException::new),
                source.getGeometrySource().orElse(null),
                source.getFragmentSource().orElseThrow(NullPointerException::new));
        String vertex = transformed.get(PatchShaderType.VERTEX);
        String geometry = transformed.get(PatchShaderType.GEOMETRY);
        String fragment = transformed.get(PatchShaderType.FRAGMENT);
        PatchedShaderPrinter.debugPatchedShaders(source.getName(), vertex, geometry, fragment);

        Objects.requireNonNull(flipped);
        ProgramBuilder builder;

        try {
            builder = ProgramBuilder.begin(source.getName(), vertex, geometry, fragment,
                    AurumSamplers.COMPOSITE_RESERVED_TEXTURE_UNITS);
        } catch (RuntimeException e) {
            // TODO: Better error handling
            throw new RuntimeException("Shader compilation failed for shadow composite " + source.getName() + "!", e);
        }

        ProgramSamplers.CustomTextureSamplerInterceptor customTextureSamplerInterceptor =
                ProgramSamplers.customTextureSamplerInterceptor(builder, customTextureIds, flippedAtLeastOnceSnapshot);

        CommonUniforms.addCommonUniforms(builder, source.getParent().getPack().getIdMap(), source.getParent().getPackDirectives(), updateNotifier);
        customUniforms.assignTo(builder);

        AurumSamplers.addNoiseSampler(customTextureSamplerInterceptor, noiseTexture);

        AurumSamplers.addShadowSamplers(customTextureSamplerInterceptor, renderTargets);
        AurumImages.addShadowColorImages(builder, renderTargets);
        AurumImages.addCustomImages(builder, customImages);
        AurumSamplers.addCustomImageSamplers(customTextureSamplerInterceptor, customImages);

        Program build = builder.build();
        customUniforms.mapholderToPass(builder, build);

        return build;
    }

    private ComputeProgram[] createComputes(ComputeSource[] sources, ImmutableSet<Integer> flipped, ImmutableSet<Integer> flippedAtLeastOnceSnapshot) {
        if (sources == null) {
            return new ComputeProgram[0];
        }

        ComputeProgram[] programs = new ComputeProgram[sources.length];
        for (int i = 0; i < programs.length; i++) {
            ComputeSource source = sources[i];
            if (source == null || source.getSource().isEmpty()) {
                continue;
            }
            Objects.requireNonNull(flipped);
            ProgramBuilder builder;

            try {
                builder = ProgramBuilder.beginCompute(source.getName(), source.getSource().orElse(null), AurumSamplers.COMPOSITE_RESERVED_TEXTURE_UNITS);
            } catch (RuntimeException e) {
                // TODO: Better error handling
                throw new RuntimeException("Shader compilation failed for shadowcomp compute " + source.getName() + "!", e);
            }

            ProgramSamplers.CustomTextureSamplerInterceptor customTextureSamplerInterceptor =
                    ProgramSamplers.customTextureSamplerInterceptor(builder, customTextureIds, flippedAtLeastOnceSnapshot);

            CommonUniforms.addCommonUniforms(builder, source.getParent().getPack().getIdMap(), source.getParent().getPackDirectives(), updateNotifier);
            customUniforms.assignTo(builder);
            AurumSamplers.addNoiseSampler(customTextureSamplerInterceptor, noiseTexture);

            AurumSamplers.addShadowSamplers(customTextureSamplerInterceptor, renderTargets);
            AurumImages.addShadowColorImages(builder, renderTargets);
            AurumImages.addCustomImages(builder, customImages);
            AurumSamplers.addCustomImageSamplers(customTextureSamplerInterceptor, customImages);

            programs[i] = builder.buildCompute();

            customUniforms.mapholderToPass(builder, programs[i]);

            programs[i].setWorkGroupInfo(source.getWorkGroupRelative(), source.getWorkGroups(), null);
        }

        return programs;
    }

    public void destroy() {
        for (Pass renderPass : passes) {
            renderPass.destroy();
        }
    }

    private static class Pass {
        Program program;
        GlFramebuffer framebuffer;
        ImmutableSet<Integer> flippedAtLeastOnce;
        ImmutableSet<Integer> stageReadsFromAlt;
        ImmutableSet<Integer> mipmappedBuffers;
        float viewportScale;
        ComputeProgram[] computes;

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
}
