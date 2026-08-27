package re.lilith.aurum.pipeline.impl.celeritas;

import com.google.common.collect.ImmutableSet;
import re.lilith.aurum.gbuffer.matching.InputAvailability;
import re.lilith.aurum.gl.program.ProgramImages;
import re.lilith.aurum.gl.program.ProgramSamplers;
import re.lilith.aurum.pipeline.impl.DeferredWorldRenderingPipeline;
import re.lilith.aurum.pipeline.samplers.AurumImages;
import re.lilith.aurum.pipeline.samplers.AurumSamplers;
import re.lilith.aurum.shaderpack.program.ProgramSet;
import re.lilith.aurum.shaderpack.texture.TextureStage;

import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public final class CeleritasTerrainPipelineFactory {
    private final DeferredWorldRenderingPipeline pipeline;

    public CeleritasTerrainPipelineFactory(DeferredWorldRenderingPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public CeleritasTerrainPipeline create(ProgramSet programs) {
        Supplier<ImmutableSet<Integer>> flipped =
                () -> pipeline.isBeforeTranslucent ? pipeline.flippedAfterPrepare : pipeline.flippedAfterTranslucent;

        IntFunction<ProgramSamplers> createTerrainSamplers = (programId) -> {
            ProgramSamplers.Builder builder = ProgramSamplers.builder(programId, AurumSamplers.WORLD_RESERVED_TEXTURE_UNITS);
            ProgramSamplers.CustomTextureSamplerInterceptor customTextureSamplerInterceptor = ProgramSamplers.customTextureSamplerInterceptor(builder, pipeline.customTextureManager.getCustomTextureIdMap(TextureStage.GBUFFERS_AND_SHADOW));

            AurumSamplers.addRenderTargetSamplers(customTextureSamplerInterceptor, flipped, pipeline.renderTargets, false);
            AurumSamplers.addLevelSamplers(customTextureSamplerInterceptor, pipeline, pipeline.whitePixel, new InputAvailability(true, true, false));
            AurumSamplers.addWorldDepthSamplers(customTextureSamplerInterceptor, pipeline.renderTargets);
            AurumSamplers.addNoiseSampler(customTextureSamplerInterceptor, pipeline.customTextureManager.getNoiseTexture());
            AurumSamplers.addCustomImageSamplers(customTextureSamplerInterceptor, pipeline.customImages);

            if (AurumSamplers.hasShadowSamplers(customTextureSamplerInterceptor)) {
                AurumSamplers.addShadowSamplers(customTextureSamplerInterceptor, Objects.requireNonNull(pipeline.shadowRenderTargets));
            }

            return builder.build();
        };

        IntFunction<ProgramImages> createTerrainImages = (programId) -> {
            ProgramImages.Builder builder = ProgramImages.builder(programId);

            AurumImages.addRenderTargetImages(builder, flipped, pipeline.renderTargets);
            AurumImages.addCustomImages(builder, pipeline.customImages);

            if (AurumImages.hasShadowImages(builder)) {
                AurumImages.addShadowColorImages(builder, Objects.requireNonNull(pipeline.shadowRenderTargets));
            }

            return builder.build();
        };

        IntFunction<ProgramSamplers> createShadowTerrainSamplers = (programId) -> {
            ProgramSamplers.Builder builder = ProgramSamplers.builder(programId, AurumSamplers.WORLD_RESERVED_TEXTURE_UNITS);
            ProgramSamplers.CustomTextureSamplerInterceptor customTextureSamplerInterceptor = ProgramSamplers.customTextureSamplerInterceptor(builder, pipeline.customTextureManager.getCustomTextureIdMap(TextureStage.GBUFFERS_AND_SHADOW));

            AurumSamplers.addRenderTargetSamplers(customTextureSamplerInterceptor, () -> pipeline.flippedAfterPrepare, pipeline.renderTargets, false);
            AurumSamplers.addLevelSamplers(customTextureSamplerInterceptor, pipeline, pipeline.whitePixel, new InputAvailability(true, true, false));
            AurumSamplers.addNoiseSampler(customTextureSamplerInterceptor, pipeline.customTextureManager.getNoiseTexture());
            AurumSamplers.addCustomImageSamplers(customTextureSamplerInterceptor, pipeline.customImages);

            // Only initialize these samplers if the shadow map renderer exists.
            // Otherwise, this program shouldn't be used at all?
            if (AurumSamplers.hasShadowSamplers(customTextureSamplerInterceptor)) {
                AurumSamplers.addShadowSamplers(customTextureSamplerInterceptor, Objects.requireNonNull(pipeline.shadowRenderTargets));
            }

            return builder.build();
        };

        IntFunction<ProgramImages> createShadowTerrainImages = (programId) -> {
            ProgramImages.Builder builder = ProgramImages.builder(programId);

            AurumImages.addRenderTargetImages(builder, () -> pipeline.flippedAfterPrepare, pipeline.renderTargets);
            AurumImages.addCustomImages(builder, pipeline.customImages);

            if (AurumImages.hasShadowImages(builder)) {
                AurumImages.addShadowColorImages(builder, Objects.requireNonNull(pipeline.shadowRenderTargets));
            }

            return builder.build();
        };

        return new CeleritasTerrainPipeline(pipeline, programs, createTerrainSamplers,
                pipeline.shadowRenderer == null ? null : createShadowTerrainSamplers, createTerrainImages,
                pipeline.shadowRenderer == null ? null : createShadowTerrainImages,
                pipeline.renderTargets, pipeline.flippedAfterPrepare, pipeline.flippedAfterTranslucent);
    }
}
