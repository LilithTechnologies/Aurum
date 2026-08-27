package re.lilith.aurum.pipeline.pathways.shadows;

import com.google.common.collect.ImmutableSet;
import re.lilith.aurum.gbuffer.matching.InputAvailability;
import re.lilith.aurum.gl.program.ComputeProgram;
import re.lilith.aurum.gl.program.ProgramBuilder;
import re.lilith.aurum.gl.program.ProgramSamplers;
import re.lilith.aurum.pipeline.impl.DeferredWorldRenderingPipeline;
import re.lilith.aurum.pipeline.samplers.AurumImages;
import re.lilith.aurum.pipeline.samplers.AurumSamplers;
import re.lilith.aurum.shaderpack.ComputeSource;
import re.lilith.aurum.shaderpack.program.ProgramSet;
import re.lilith.aurum.shaderpack.texture.TextureStage;
import re.lilith.aurum.uniforms.CommonUniforms;

import java.util.function.Supplier;

public final class ShadowComputeFactory {
    private final DeferredWorldRenderingPipeline pipeline;

    public ShadowComputeFactory(DeferredWorldRenderingPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public ComputeProgram[] create(ComputeSource[] compute, ProgramSet programSet) {
        ComputeProgram[] programs = new ComputeProgram[compute.length];
        for (int i = 0; i < programs.length; i++) {
            ComputeSource source = compute[i];
            if (source == null || !source.getSource().isPresent()) {
                continue;
            } else {
                ProgramBuilder builder;

                try {
                    builder = ProgramBuilder.beginCompute(source.getName(), source.getSource().orElse(null), AurumSamplers.WORLD_RESERVED_TEXTURE_UNITS);
                } catch (RuntimeException e) {
                    // TODO: Better error handling
                    throw new RuntimeException("Shader compilation failed!", e);
                }

                CommonUniforms.addCommonUniforms(builder, programSet.getPack().getIdMap(), programSet.getPackDirectives(), pipeline.updateNotifier);

                Supplier<ImmutableSet<Integer>> flipped;

                flipped = () -> pipeline.flippedBeforeShadow;

                TextureStage textureStage = TextureStage.GBUFFERS_AND_SHADOW;

                ProgramSamplers.CustomTextureSamplerInterceptor customTextureSamplerInterceptor =
                        ProgramSamplers.customTextureSamplerInterceptor(builder,
                                pipeline.customTextureManager.getCustomTextureIdMap(textureStage));

                AurumSamplers.addRenderTargetSamplers(customTextureSamplerInterceptor, flipped, pipeline.renderTargets, false);
                AurumImages.addRenderTargetImages(builder, flipped, pipeline.renderTargets);
                AurumImages.addCustomImages(builder, pipeline.customImages);
                AurumSamplers.addCustomImageSamplers(customTextureSamplerInterceptor, pipeline.customImages);

                AurumSamplers.addLevelSamplers(customTextureSamplerInterceptor, pipeline, pipeline.whitePixel, new InputAvailability(true, true, false));

                AurumSamplers.addNoiseSampler(customTextureSamplerInterceptor, pipeline.customTextureManager.getNoiseTexture());

                if (AurumSamplers.hasShadowSamplers(customTextureSamplerInterceptor)) {
                    if (pipeline.shadowRenderTargets != null) {
                        AurumSamplers.addShadowSamplers(customTextureSamplerInterceptor, pipeline.shadowRenderTargets);
                        AurumImages.addShadowColorImages(builder, pipeline.shadowRenderTargets);
                    }
                }

                programs[i] = builder.buildCompute();

                programs[i].setWorkGroupInfo(source.getWorkGroupRelative(), source.getWorkGroups(), null);
            }
        }

        return programs;
    }
}
