package re.lilith.aurum.pipeline.pathways.pass;

import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import re.lilith.aurum.gbuffer.matching.InputAvailability;
import re.lilith.aurum.gbuffer.matching.RenderCondition;
import re.lilith.aurum.gbuffer.matching.SpecialCondition;
import re.lilith.aurum.gl.GlFramebuffer;
import re.lilith.aurum.gl.blending.AlphaTestOverride;
import re.lilith.aurum.gl.blending.BufferBlendOverride;
import re.lilith.aurum.gl.program.Program;
import re.lilith.aurum.gl.program.ProgramBuilder;
import re.lilith.aurum.gl.program.ProgramSamplers;
import re.lilith.aurum.mixin.access.GlStateManagerAccessor;
import re.lilith.aurum.pipeline.WorldRenderingPhase;
import re.lilith.aurum.pipeline.impl.DeferredWorldRenderingPipeline;
import re.lilith.aurum.pipeline.samplers.AurumImages;
import re.lilith.aurum.pipeline.samplers.AurumSamplers;
import re.lilith.aurum.pipeline.transform.ShaderTransformer;
import re.lilith.aurum.pipeline.transform.impl.glint.GlintScrollInjector;
import re.lilith.aurum.pipeline.transform.patch.PatchShaderType;
import re.lilith.aurum.pipeline.transform.patch.PatchedShaderPrinter;
import re.lilith.aurum.shaderpack.IdMap;
import re.lilith.aurum.shaderpack.PackDirectives;
import re.lilith.aurum.shaderpack.program.ProgramDirectives;
import re.lilith.aurum.shaderpack.program.ProgramId;
import re.lilith.aurum.shaderpack.program.ProgramSource;
import re.lilith.aurum.shaderpack.texture.TextureStage;
import re.lilith.aurum.uniforms.CommonUniforms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Decides which {@link RenderCondition} applies to a given rendering phase, and compiles/wires up the
 * {@link Pass} (samplers, images, framebuffers, GL state overrides) for a resolved shader program source.
 */
public final class PassFactory {
    private final DeferredWorldRenderingPipeline pipeline;

    public PassFactory(DeferredWorldRenderingPipeline pipeline) {
        this.pipeline = pipeline;
    }

    private static boolean isBlendedForTranslucency() {
        GlStateManager.BlendFuncState blend = GlStateManagerAccessor.getBLEND();
        return blend.srcFactorRGB == GL11.GL_SRC_ALPHA && blend.dstFactorRGB == GL11.GL_ONE_MINUS_SRC_ALPHA
                && blend.srcFactorAlpha == GL11.GL_ONE && blend.dstFactorAlpha == GL11.GL_ONE_MINUS_SRC_ALPHA;
    }

    public RenderCondition getCondition(WorldRenderingPhase phase) {
        if (pipeline.isRenderingShadow) {
            return RenderCondition.SHADOW;
        }

        if (pipeline.special != null) {
            if (pipeline.special == SpecialCondition.BEACON_BEAM) {
                return RenderCondition.BEACON_BEAM;
            } else if (pipeline.special == SpecialCondition.ENTITY_EYES) {
                return RenderCondition.ENTITY_EYES;
            } else if (pipeline.special == SpecialCondition.GLINT) {
                return RenderCondition.GLINT;
            }
        }

        return switch (phase) {
            case NONE, OUTLINE, DEBUG, PARTICLES -> RenderCondition.DEFAULT;
            case SKY, SUNSET, CUSTOM_SKY, SUN, MOON, STARS, VOID -> RenderCondition.SKY;
            case TERRAIN_SOLID, TERRAIN_CUTOUT, TERRAIN_CUTOUT_MIPPED -> RenderCondition.TERRAIN_OPAQUE;
            case ENTITIES ->
                    isBlendedForTranslucency() ? RenderCondition.ENTITIES_TRANSLUCENT : RenderCondition.ENTITIES;
            case BLOCK_ENTITIES ->
                    isBlendedForTranslucency() ? RenderCondition.BLOCK_ENTITIES_TRANSLUCENT : RenderCondition.BLOCK_ENTITIES;
            case DESTROY -> RenderCondition.DESTROY;
            case HAND_SOLID -> RenderCondition.HAND_OPAQUE;
            case TERRAIN_TRANSLUCENT, TRIPWIRE -> RenderCondition.TERRAIN_TRANSLUCENT;
            case CLOUDS -> RenderCondition.CLOUDS;
            case RAIN_SNOW -> RenderCondition.RAIN_SNOW;
            case HAND_TRANSLUCENT -> RenderCondition.HAND_TRANSLUCENT;
            case WORLD_BORDER -> RenderCondition.WORLD_BORDER;
        };
    }

    public Pass createDefaultPass() {
        GlFramebuffer framebufferBeforeTranslucents;
        GlFramebuffer framebufferAfterTranslucents;

        framebufferBeforeTranslucents =
                pipeline.renderTargets.createGbufferFramebuffer(pipeline.flippedAfterPrepare, new int[]{0});
        framebufferAfterTranslucents =
                pipeline.renderTargets.createGbufferFramebuffer(pipeline.flippedAfterTranslucent, new int[]{0});

        return new Pass(pipeline, null, framebufferBeforeTranslucents, framebufferAfterTranslucents, null,
                null, Collections.emptyList(), false);
    }

    public Pass createPass(ProgramSource source, InputAvailability availability, boolean shadow, ProgramId id) {
        // TODO: Properly handle empty shaders?
        boolean scrollGlint = GlintScrollInjector.shouldInject(id, source);
        Map<PatchShaderType, String> transformed = ShaderTransformer.patchAttributes(
                source.getVertexSource().orElseThrow(NullPointerException::new),
                source.getGeometrySource().orElse(null),
                source.getFragmentSource().orElseThrow(NullPointerException::new),
                availability,
                scrollGlint);
        String vertex = transformed.get(PatchShaderType.VERTEX);
        String geometry = transformed.get(PatchShaderType.GEOMETRY);
        String fragment = transformed.get(PatchShaderType.FRAGMENT);

        PatchedShaderPrinter.debugPatchedShaders(source.getName(), vertex, geometry, fragment);

        ProgramBuilder builder = ProgramBuilder.begin(source.getName(), vertex, geometry, fragment,
                AurumSamplers.WORLD_RESERVED_TEXTURE_UNITS);

        return createPassInner(builder, source.getParent().getPack().getIdMap(), source.getDirectives(), source.getParent().getPackDirectives(), availability, shadow, id);
    }

    private Pass createPassInner(ProgramBuilder builder, IdMap map, ProgramDirectives programDirectives,
                                 PackDirectives packDirectives, InputAvailability availability, boolean shadow, ProgramId id) {

        CommonUniforms.addCommonUniforms(builder, map, packDirectives, pipeline.updateNotifier);
        pipeline.customUniforms.assignTo(builder);

        Supplier<ImmutableSet<Integer>> flipped;

        if (shadow) {
            flipped = () -> (pipeline.shouldRenderPrepareBeforeShadow ? pipeline.flippedAfterPrepare : pipeline.flippedBeforeShadow);
        } else {
            flipped = () -> pipeline.isBeforeTranslucent ? pipeline.flippedAfterPrepare : pipeline.flippedAfterTranslucent;
        }

        TextureStage textureStage = TextureStage.GBUFFERS_AND_SHADOW;

        ProgramSamplers.CustomTextureSamplerInterceptor customTextureSamplerInterceptor =
                ProgramSamplers.customTextureSamplerInterceptor(builder,
                        pipeline.customTextureManager.getCustomTextureIdMap(textureStage));

        AurumSamplers.addRenderTargetSamplers(customTextureSamplerInterceptor, flipped, pipeline.renderTargets, false);
        AurumImages.addRenderTargetImages(builder, flipped, pipeline.renderTargets);
        AurumImages.addCustomImages(builder, pipeline.customImages);
        AurumSamplers.addCustomImageSamplers(customTextureSamplerInterceptor, pipeline.customImages);

        if (!pipeline.shouldBindPBR) {
            pipeline.shouldBindPBR = AurumSamplers.hasPBRSamplers(customTextureSamplerInterceptor);
        }

        AurumSamplers.addLevelSamplers(customTextureSamplerInterceptor, pipeline, pipeline.whitePixel, availability);

        if (!shadow) {
            AurumSamplers.addWorldDepthSamplers(customTextureSamplerInterceptor, pipeline.renderTargets);
        }

        AurumSamplers.addNoiseSampler(customTextureSamplerInterceptor, pipeline.customTextureManager.getNoiseTexture());

        if (AurumSamplers.hasShadowSamplers(customTextureSamplerInterceptor)) {
            if (!shadow) {
                pipeline.shadowTargetsSupplier.get();
            }

            if (pipeline.shadowRenderTargets != null) {
                AurumSamplers.addShadowSamplers(customTextureSamplerInterceptor, pipeline.shadowRenderTargets);
                AurumImages.addShadowColorImages(builder, pipeline.shadowRenderTargets);
            }
        }

        GlFramebuffer framebufferBeforeTranslucents;
        GlFramebuffer framebufferAfterTranslucents;

        if (shadow) {
            // Always add both draw buffers on the shadow pass.
            assert pipeline.shadowRenderTargets != null;
            framebufferBeforeTranslucents =
                    pipeline.shadowTargetsSupplier.get().createShadowFramebuffer(pipeline.shadowRenderTargets.snapshot(), new int[]{0, 1});
            framebufferAfterTranslucents = framebufferBeforeTranslucents;
        } else {
            framebufferBeforeTranslucents =
                    pipeline.renderTargets.createGbufferFramebuffer(pipeline.flippedAfterPrepare, programDirectives.getDrawBuffers());
            framebufferAfterTranslucents =
                    pipeline.renderTargets.createGbufferFramebuffer(pipeline.flippedAfterTranslucent, programDirectives.getDrawBuffers());
        }

        builder.bindAttributeLocation(11, "mc_Entity");
        builder.bindAttributeLocation(12, "mc_midTexCoord");
        builder.bindAttributeLocation(13, "at_tangent");
        builder.bindAttributeLocation(14, "at_midBlock");

        AlphaTestOverride alphaTestOverride = programDirectives.getAlphaTestOverride().orElse(null);

        List<BufferBlendOverride> bufferOverrides = new ArrayList<>();

        programDirectives.getBufferBlendOverrides().forEach(information -> {
            int index = Ints.indexOf(programDirectives.getDrawBuffers(), information.getIndex());
            if (index > -1) {
                bufferOverrides.add(new BufferBlendOverride(index, information.getBlendMode()));
            }
        });

        Program program = builder.build();
        pipeline.customUniforms.mapholderToPass(builder, program);

        return new Pass(pipeline, program, framebufferBeforeTranslucents, framebufferAfterTranslucents, alphaTestOverride,
                programDirectives.getBlendModeOverride().orElse(id.getBlendModeOverride()), bufferOverrides, shadow);
    }
}
