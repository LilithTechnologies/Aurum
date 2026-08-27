package re.lilith.aurum.pipeline.pathways.postprocess;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import re.lilith.aurum.expression.CustomUniforms;
import re.lilith.aurum.gl.image.GlImage;
import re.lilith.aurum.gl.program.ComputeProgram;
import re.lilith.aurum.gl.program.Program;
import re.lilith.aurum.gl.program.ProgramBuilder;
import re.lilith.aurum.gl.program.ProgramSamplers;
import re.lilith.aurum.pipeline.pathways.shadows.ShadowRenderTargets;
import re.lilith.aurum.pipeline.samplers.AurumImages;
import re.lilith.aurum.pipeline.samplers.AurumSamplers;
import re.lilith.aurum.pipeline.transform.ShaderTransformer;
import re.lilith.aurum.pipeline.transform.patch.PatchShaderType;
import re.lilith.aurum.pipeline.transform.patch.PatchedShaderPrinter;
import re.lilith.aurum.shaderpack.ComputeSource;
import re.lilith.aurum.shaderpack.program.ProgramSource;
import re.lilith.aurum.targets.render.RenderTargets;
import re.lilith.aurum.uniforms.CommonUniforms;
import re.lilith.aurum.uniforms.utility.FrameUpdateNotifier;

import java.util.Map;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class PostProcessRenderer {
    protected RenderTargets renderTargets;
    protected GlImage[] customImages;
    protected IntSupplier noiseTexture;
    protected FrameUpdateNotifier updateNotifier;
    protected CenterDepthSampler centerDepthSampler;
    protected Object2ObjectMap<String, IntSupplier> customTextureIds;
    protected CustomUniforms customUniforms;

    protected Program createProgram(ProgramSource source, ImmutableSet<Integer> flipped, ImmutableSet<Integer> flippedAtLeastOnceSnapshot,
                                    Supplier<ShadowRenderTargets> shadowTargetsSupplier) {
        // TODO: Properly handle empty shaders
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
            throw new RuntimeException("Shader compilation failed!", e);
        }

        ProgramSamplers.CustomTextureSamplerInterceptor customTextureSamplerInterceptor = ProgramSamplers.customTextureSamplerInterceptor(builder, customTextureIds, flippedAtLeastOnceSnapshot);

        CommonUniforms.addCommonUniforms(builder, source.getParent().getPack().getIdMap(), source.getParent().getPackDirectives(), updateNotifier);
        customUniforms.assignTo(builder);
        AurumSamplers.addRenderTargetSamplers(customTextureSamplerInterceptor, () -> flipped, renderTargets, true);
        AurumImages.addRenderTargetImages(builder, () -> flipped, renderTargets);
        AurumImages.addCustomImages(builder, customImages);
        AurumSamplers.addCustomImageSamplers(customTextureSamplerInterceptor, customImages);
        AurumSamplers.addNoiseSampler(customTextureSamplerInterceptor, noiseTexture);
        AurumSamplers.addCompositeSamplers(customTextureSamplerInterceptor, renderTargets);

        if (AurumSamplers.hasShadowSamplers(customTextureSamplerInterceptor)) {
            AurumSamplers.addShadowSamplers(customTextureSamplerInterceptor, shadowTargetsSupplier.get());
            AurumImages.addShadowColorImages(builder, shadowTargetsSupplier.get());
        }

        // TODO: Don't duplicate this with CompositeRenderer
        centerDepthSampler.setUsage(builder.addDynamicSampler(centerDepthSampler::getCenterDepthTexture, "aurum_centerDepthSmooth"));

        Program program = builder.build();
        customUniforms.mapholderToPass(builder, program);
        return program;
    }

    protected ComputeProgram[] createComputes(ComputeSource[] compute, ImmutableSet<Integer> flipped, ImmutableSet<Integer> flippedAtLeastOnceSnapshot, Supplier<ShadowRenderTargets> shadowTargetsSupplier) {
        ComputeProgram[] programs = new ComputeProgram[compute.length];
        for (int i = 0; i < programs.length; i++) {
            ComputeSource source = compute[i];
            if (source == null || source.getSource().isEmpty()) {
                // TODO: Properly handle empty shaders
                continue;
            }
            Objects.requireNonNull(flipped);
            ProgramBuilder builder;

            try {
                builder = ProgramBuilder.beginCompute(source.getName(), source.getSource().orElse(null), AurumSamplers.COMPOSITE_RESERVED_TEXTURE_UNITS);
            } catch (RuntimeException e) {
                // TODO: Better error handling
                throw new RuntimeException("Shader compilation failed!", e);
            }

            ProgramSamplers.CustomTextureSamplerInterceptor customTextureSamplerInterceptor = ProgramSamplers.customTextureSamplerInterceptor(builder, customTextureIds, flippedAtLeastOnceSnapshot);

            CommonUniforms.addCommonUniforms(builder, source.getParent().getPack().getIdMap(), source.getParent().getPackDirectives(), updateNotifier);
            AurumSamplers.addRenderTargetSamplers(customTextureSamplerInterceptor, () -> flipped, renderTargets, true);
            AurumImages.addRenderTargetImages(builder, () -> flipped, renderTargets);
            AurumImages.addCustomImages(builder, customImages);
            AurumSamplers.addCustomImageSamplers(customTextureSamplerInterceptor, customImages);

            AurumSamplers.addNoiseSampler(customTextureSamplerInterceptor, noiseTexture);
            AurumSamplers.addCompositeSamplers(customTextureSamplerInterceptor, renderTargets);

            if (AurumSamplers.hasShadowSamplers(customTextureSamplerInterceptor)) {
                AurumSamplers.addShadowSamplers(customTextureSamplerInterceptor, shadowTargetsSupplier.get());
                AurumImages.addShadowColorImages(builder, shadowTargetsSupplier.get());
            }

            // TODO: Don't duplicate this with FinalPassRenderer
            centerDepthSampler.setUsage(builder.addDynamicSampler(centerDepthSampler::getCenterDepthTexture, "aurum_centerDepthSmooth"));

            programs[i] = builder.buildCompute();

            programs[i].setWorkGroupInfo(source.getWorkGroupRelative(), source.getWorkGroups(), null);
        }

        return programs;
    }
}
