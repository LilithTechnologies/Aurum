package re.lilith.aurum.pipeline.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL21C;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.expression.CustomUniforms;
import re.lilith.aurum.gbuffer.GbufferPrograms;
import re.lilith.aurum.gbuffer.matching.InputAvailability;
import re.lilith.aurum.gbuffer.matching.ProgramTable;
import re.lilith.aurum.gbuffer.matching.RenderCondition;
import re.lilith.aurum.gbuffer.matching.SpecialCondition;
import re.lilith.aurum.gbuffer.state.RenderTargetStateListener;
import re.lilith.aurum.gbuffer.state.StateTracker;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.GlFramebuffer;
import re.lilith.aurum.gl.blending.BlendModeOverride;
import re.lilith.aurum.gl.buffer.ShaderStorageBufferHolder;
import re.lilith.aurum.gl.image.GlImage;
import re.lilith.aurum.gl.program.ComputeProgram;
import re.lilith.aurum.gl.program.Program;
import re.lilith.aurum.gl.texture.DepthBufferFormat;
import re.lilith.aurum.mixin.access.WorldRendererAccessor;
import re.lilith.aurum.pipeline.PipelineConstruction;
import re.lilith.aurum.pipeline.PipelineTeardown;
import re.lilith.aurum.pipeline.WorldRenderingPhase;
import re.lilith.aurum.pipeline.WorldRenderingPipeline;
import re.lilith.aurum.pipeline.impl.celeritas.CeleritasTerrainPipeline;
import re.lilith.aurum.pipeline.impl.celeritas.CeleritasTerrainPipelineFactory;
import re.lilith.aurum.pipeline.pathways.HorizonRenderer;
import re.lilith.aurum.pipeline.pathways.clear.ClearPass;
import re.lilith.aurum.pipeline.pathways.clear.ClearPassCreator;
import re.lilith.aurum.pipeline.pathways.pass.Pass;
import re.lilith.aurum.pipeline.pathways.pass.PassFactory;
import re.lilith.aurum.pipeline.pathways.postprocess.BufferFlipper;
import re.lilith.aurum.pipeline.pathways.postprocess.CenterDepthSampler;
import re.lilith.aurum.pipeline.pathways.postprocess.CompositeRenderer;
import re.lilith.aurum.pipeline.pathways.postprocess.FinalPassRenderer;
import re.lilith.aurum.pipeline.pathways.shadows.ShadowCompositeRenderer;
import re.lilith.aurum.pipeline.pathways.shadows.ShadowComputeFactory;
import re.lilith.aurum.pipeline.pathways.shadows.ShadowRenderTargets;
import re.lilith.aurum.pipeline.pathways.shadows.ShadowRenderer;
import re.lilith.aurum.pipeline.state.CapturedRenderingState;
import re.lilith.aurum.pipeline.transform.patch.PatchedShaderPrinter;
import re.lilith.aurum.shaderpack.CloudSetting;
import re.lilith.aurum.shaderpack.OptionalBoolean;
import re.lilith.aurum.shaderpack.PackDirectives;
import re.lilith.aurum.shaderpack.PackShadowDirectives;
import re.lilith.aurum.shaderpack.program.ProgramFallbackResolver;
import re.lilith.aurum.shaderpack.program.ProgramId;
import re.lilith.aurum.shaderpack.program.ProgramSet;
import re.lilith.aurum.shaderpack.program.ProgramSource;
import re.lilith.aurum.shaderpack.texture.TextureStage;
import re.lilith.aurum.targets.depth.DepthAttachedFramebuffer;
import re.lilith.aurum.targets.render.RenderTargets;
import re.lilith.aurum.targets.texture.NativeImageBackedSingleColorTexture;
import re.lilith.aurum.texture.CustomTextureManager;
import re.lilith.aurum.texture.TextureInfoCache;
import re.lilith.aurum.texture.format.TextureFormat;
import re.lilith.aurum.texture.format.TextureFormatLoader;
import re.lilith.aurum.texture.pbr.PBRTextureHolder;
import re.lilith.aurum.texture.pbr.PBRTextureManager;
import re.lilith.aurum.texture.pbr.PBRType;
import re.lilith.aurum.uniforms.CommonUniforms;
import re.lilith.aurum.uniforms.utility.EntityColorState;
import re.lilith.aurum.uniforms.utility.FrameUpdateNotifier;

import java.util.*;
import java.util.function.Supplier;

public class DeferredWorldRenderingPipeline implements WorldRenderingPipeline, RenderTargetStateListener {
    public final RenderTargets renderTargets;

    @Nullable
    public ShadowRenderTargets shadowRenderTargets;
    @Nullable
    private final ComputeProgram[] shadowComputes;
    public final Supplier<ShadowRenderTargets> shadowTargetsSupplier;

    public final ProgramTable<Pass> table;
    private final PassFactory passFactory = new PassFactory(this);

    private ImmutableList<ClearPass> clearPassesFull;
    private ImmutableList<ClearPass> clearPasses;
    private ImmutableList<ClearPass> shadowClearPasses;
    private ImmutableList<ClearPass> shadowClearPassesFull;

    private final CompositeRenderer prepareRenderer;

    @Nullable
    public final ShadowRenderer shadowRenderer;

    @Nullable
    public final ShadowCompositeRenderer shadowCompositeRenderer;

    public final int shadowMapResolution;
    public final CompositeRenderer deferredRenderer;
    public final CompositeRenderer compositeRenderer;
    public final FinalPassRenderer finalPassRenderer;
    public final CustomTextureManager customTextureManager;
    public final AbstractTexture whitePixel;
    public final FrameUpdateNotifier updateNotifier;
    public final CustomUniforms customUniforms;
    public final CenterDepthSampler centerDepthSampler;
    @Nullable
    public final ShaderStorageBufferHolder ssboHolder;
    public final GlImage[] customImages;
    private final GlImage[] imagesToClear;

    public final ImmutableSet<Integer> flippedBeforeShadow;
    public final ImmutableSet<Integer> flippedAfterPrepare;
    public final ImmutableSet<Integer> flippedAfterTranslucent;

    private final CeleritasTerrainPipeline celeritasTerrainPipeline;

    public final HorizonRenderer horizonRenderer = new HorizonRenderer();

    private final float sunPathRotation;
    private final CloudSetting cloudSetting;
    private final boolean shouldRenderUnderwaterOverlay;
    private final boolean shouldRenderVignette;
    private final boolean shouldRenderSun;
    private final boolean shouldRenderMoon;
    private final boolean shouldRenderWeather;
    private final boolean shouldRenderWeatherParticles;
    private final boolean shouldWriteRainAndSnowToDepthBuffer;
    private final boolean shouldRenderParticlesBeforeDeferred;
    public final boolean shouldRenderPrepareBeforeShadow;
    private final boolean oldLighting;
    private final boolean allowConcurrentCompute;
    private final @Nullable Integer forcedShadowRenderDistanceChunks;

    private Pass current = null;

    private WorldRenderingPhase overridePhase = null;
    private WorldRenderingPhase phase = WorldRenderingPhase.NONE;
    public boolean isBeforeTranslucent;
    public boolean isRenderingShadow = false;
    private InputAvailability inputs = new InputAvailability(false, false, false);
    public SpecialCondition special = null;

    public boolean shouldBindPBR;
    private int currentNormalTexture;
    private int currentSpecularTexture;
    private final PackDirectives packDirectives;

    public DeferredWorldRenderingPipeline(ProgramSet programs) {
        Objects.requireNonNull(programs);

        this.cloudSetting = programs.getPackDirectives().getCloudSetting();
        this.shouldRenderUnderwaterOverlay = programs.getPackDirectives().underwaterOverlay();
        this.shouldRenderVignette = programs.getPackDirectives().vignette();
        this.shouldRenderSun = programs.getPackDirectives().shouldRenderSun();
        this.shouldRenderMoon = programs.getPackDirectives().shouldRenderMoon();
        this.shouldRenderWeather = programs.getPackDirectives().shouldRenderWeather();
        this.shouldRenderWeatherParticles = programs.getPackDirectives().shouldRenderWeatherParticles();
        this.shouldWriteRainAndSnowToDepthBuffer = programs.getPackDirectives().rainDepth();
        this.shouldRenderParticlesBeforeDeferred = programs.getPackDirectives().areParticlesBeforeDeferred();
        this.allowConcurrentCompute = programs.getPackDirectives().getConcurrentCompute();
        this.shouldRenderPrepareBeforeShadow = programs.getPackDirectives().isPrepareBeforeShadow();
        this.oldLighting = programs.getPackDirectives().isOldLighting();
        this.updateNotifier = new FrameUpdateNotifier();

        CustomUniforms.Builder packCustomUniforms = programs.getPack().getCustomUniforms();
        this.customUniforms = packCustomUniforms.build(
                holder -> CommonUniforms.addNonDynamicUniforms(holder, programs.getPackDirectives(), this.updateNotifier,
                        packCustomUniforms::hasVariable)
        );

        this.packDirectives = programs.getPackDirectives();

        Framebuffer mainTarget = MinecraftClient.getInstance().getFramebuffer();

        this.renderTargets = PipelineConstruction.createRenderTargets(mainTarget, programs.getPackDirectives());
        this.ssboHolder = PipelineConstruction.createSsboHolder(programs.getPack().getBufferObjects(), mainTarget);

        PipelineConstruction.ImageLists images = PipelineConstruction.buildCustomImages(programs, mainTarget.viewportWidth, mainTarget.viewportHeight);
        this.customImages = images.all();
        this.imagesToClear = images.toClear();

        this.sunPathRotation = programs.getPackDirectives().getSunPathRotation();

        PackShadowDirectives shadowDirectives = programs.getPackDirectives().getShadowDirectives();
        this.forcedShadowRenderDistanceChunks = PipelineConstruction.computeForcedShadowRenderDistanceChunks(shadowDirectives);

        PipelineConstruction.applyBlockRenderingSettings(programs, shouldDisableDirectionalShading());

        // Don't clobber anything in texture unit 0. It probably won't cause issues, but we're just being cautious here.
        GlStateManager.activeTexture(GL20C.GL_TEXTURE2);

        customTextureManager = new CustomTextureManager(programs.getPackDirectives(), programs.getPack().getCustomTextureDataMap(), programs.getPack().getCustomNoiseTexture().orElse(null));

        whitePixel = new NativeImageBackedSingleColorTexture(255, 255, 255, 255);

        GlStateManager.activeTexture(GL20C.GL_TEXTURE0);

        this.flippedBeforeShadow = ImmutableSet.of();

        BufferFlipper flipper = new BufferFlipper();

        this.centerDepthSampler = new CenterDepthSampler(() -> getRenderTargets().getDepthTexture(), programs.getPackDirectives().getCenterDepthHalfLife());

        this.shadowMapResolution = programs.getPackDirectives().getShadowDirectives().getResolution();

        this.shadowTargetsSupplier = () -> {
            if (shadowRenderTargets == null) {
                this.shadowRenderTargets = new ShadowRenderTargets(shadowMapResolution, shadowDirectives);
            }

            return shadowRenderTargets;
        };

        PatchedShaderPrinter.resetPrintState();

        CompositeStages stages = buildCompositeStages(programs, flipper);
        this.prepareRenderer = stages.prepare();
        this.flippedAfterPrepare = stages.flippedAfterPrepare();
        this.deferredRenderer = stages.deferred();
        this.flippedAfterTranslucent = stages.flippedAfterTranslucent();
        this.compositeRenderer = stages.composite();
        this.finalPassRenderer = stages.finalPass();

        ProgramId[] ids = PipelineConstruction.createProgramIdTable();

        ProgramFallbackResolver resolver = new ProgramFallbackResolver(programs);

        Map<Pair<ProgramId, InputAvailability>, Pass> cachedPasses = new HashMap<>();

        this.shadowComputes = new ShadowComputeFactory(this).create(programs.getShadowCompute(), programs);

        this.table = new ProgramTable<>((condition, availability) -> {
            int idx;

            if (availability.texture() && availability.lightmap()) {
                idx = 2;
            } else if (availability.texture()) {
                idx = 1;
            } else {
                idx = 0;
            }

            ProgramId id = ids[condition.ordinal() * 3 + idx];

            if (id == null) {
                id = ids[idx];
            }

            ProgramId finalId = id;

            return cachedPasses.computeIfAbsent(new Pair<>(id, availability), p -> {
                ProgramSource source = resolver.resolveNullable(p.getLeft());

                if (condition == RenderCondition.SHADOW) {
                    if (!shadowDirectives.isShadowEnabled().orElse(shadowRenderTargets != null)) {
                        // shadow is not used
                        return null;
                    } else if (source == null) {
                        // still need the custom framebuffer, viewport, and blend mode behavior
                        assert shadowRenderTargets != null;
                        GlFramebuffer shadowFb =
                                shadowTargetsSupplier.get().createShadowFramebuffer(shadowRenderTargets.snapshot(), new int[]{0});
                        return new Pass(this, null, shadowFb, shadowFb, null,
                                BlendModeOverride.OFF, Collections.emptyList(), true);
                    }
                }

                if (source == null) {
                    return passFactory.createDefaultPass();
                }

                try {
                    return passFactory.createPass(source, availability, condition == RenderCondition.SHADOW, finalId);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create pass for " + source.getName() + " for rendering condition "
                            + condition + " specialized to input availability " + availability, e);
                }
            });
        });

        ShadowRenderingResult shadowSetup = initShadowRendering(programs, shadowDirectives);
        this.shadowCompositeRenderer = shadowSetup.compositeRenderer();
        this.shadowRenderer = shadowSetup.renderer();

        this.clearPassesFull = ClearPassCreator.createClearPasses(renderTargets, true,
                programs.getPackDirectives().getRenderTargetDirectives());
        this.clearPasses = ClearPassCreator.createClearPasses(renderTargets, false,
                programs.getPackDirectives().getRenderTargetDirectives());

        this.celeritasTerrainPipeline = new CeleritasTerrainPipelineFactory(this).create(programs);
    }

    private record CompositeStages(CompositeRenderer prepare, ImmutableSet<Integer> flippedAfterPrepare,
                                   CompositeRenderer deferred, ImmutableSet<Integer> flippedAfterTranslucent,
                                   CompositeRenderer composite, FinalPassRenderer finalPass) {
    }

    private CompositeStages buildCompositeStages(ProgramSet programs, BufferFlipper flipper) {
        CompositeRenderer prepare = new CompositeRenderer(programs.getPrepare(), programs.getPrepareCompute(), renderTargets,
                customTextureManager.getNoiseTexture(), updateNotifier, centerDepthSampler, flipper, shadowTargetsSupplier,
                customTextureManager.getCustomTextureIdMap(TextureStage.PREPARE),
                programs.getPackDirectives().getExplicitFlips("prepare_pre"), customUniforms, customImages);

        ImmutableSet<Integer> flippedAfterPrepare = flipper.snapshot();

        CompositeRenderer deferred = new CompositeRenderer(programs.getDeferred(), programs.getDeferredCompute(), renderTargets,
                customTextureManager.getNoiseTexture(), updateNotifier, centerDepthSampler, flipper, shadowTargetsSupplier,
                customTextureManager.getCustomTextureIdMap(TextureStage.DEFERRED),
                programs.getPackDirectives().getExplicitFlips("deferred_pre"), customUniforms, customImages);

        ImmutableSet<Integer> flippedAfterTranslucent = flipper.snapshot();

        CompositeRenderer composite = new CompositeRenderer(programs.getComposite(), programs.getCompositeCompute(), renderTargets,
                customTextureManager.getNoiseTexture(), updateNotifier, centerDepthSampler, flipper, shadowTargetsSupplier,
                customTextureManager.getCustomTextureIdMap(TextureStage.COMPOSITE_AND_FINAL),
                programs.getPackDirectives().getExplicitFlips("composite_pre"), customUniforms, customImages);

        FinalPassRenderer finalPass = new FinalPassRenderer(programs, renderTargets, customTextureManager.getNoiseTexture(), updateNotifier, flipper.snapshot(),
                centerDepthSampler, shadowTargetsSupplier,
                customTextureManager.getCustomTextureIdMap(TextureStage.COMPOSITE_AND_FINAL),
                composite.getFlippedAtLeastOnceFinal(), customUniforms, customImages);

        return new CompositeStages(prepare, flippedAfterPrepare, deferred, flippedAfterTranslucent, composite, finalPass);
    }

    private record ShadowRenderingResult(@Nullable ShadowCompositeRenderer compositeRenderer,
                                         @Nullable ShadowRenderer renderer) {
    }

    private ShadowRenderingResult initShadowRendering(ProgramSet programs, PackShadowDirectives shadowDirectives) {
        if (shadowRenderTargets == null && shadowDirectives.isShadowEnabled() == OptionalBoolean.TRUE) {
            shadowRenderTargets = new ShadowRenderTargets(shadowMapResolution, shadowDirectives);
        }

        if (shadowRenderTargets == null) {
            this.shadowClearPasses = ImmutableList.of();
            this.shadowClearPassesFull = ImmutableList.of();
            return new ShadowRenderingResult(null, null);
        }

        this.shadowClearPasses = ClearPassCreator.createShadowClearPasses(shadowRenderTargets, false, shadowDirectives);
        this.shadowClearPassesFull = ClearPassCreator.createShadowClearPasses(shadowRenderTargets, true, shadowDirectives);

        ShadowCompositeRenderer compositeRenderer = new ShadowCompositeRenderer(programs.getPackDirectives(),
                programs.getShadowComposite(), programs.getShadowCompCompute(), shadowRenderTargets,
                customTextureManager.getNoiseTexture(), updateNotifier,
                customTextureManager.getCustomTextureIdMap(TextureStage.SHADOWCOMP),
                programs.getPackDirectives().getExplicitFlips("shadowcomp_pre"),
                customUniforms, customImages);

        ShadowRenderer renderer = null;
        if (programs.getPackDirectives().getShadowDirectives().isShadowEnabled().orElse(true)) {
            renderer = new ShadowRenderer(programs.getShadow().orElse(null),
                    programs.getPackDirectives(), shadowRenderTargets, compositeRenderer);
            Program shadowProgram = table.match(RenderCondition.SHADOW, new InputAvailability(true, true, true)).getProgram();
            renderer.setUsesImages(shadowProgram != null && shadowProgram.getActiveImages() > 0);
        }

        return new ShadowRenderingResult(compositeRenderer, renderer);
    }

    private RenderTargets getRenderTargets() {
        return renderTargets;
    }

    private void checkWorld() {
        // If we're not in a world, then obviously we cannot possibly be rendering a world
        if (MinecraftClient.getInstance().world == null) {
            isRenderingWorld = false;
            current = null;
        }
    }

    @Override
    public boolean shouldDisableVanillaEntityShadows() {
        // OptiFine seems to disable vanilla shadows when the shaderpack uses shadow mapping?
        return shadowRenderer != null;
    }

    @Override
    public boolean shouldDisableDirectionalShading() {
        return !oldLighting;
    }

    @Override
    public CloudSetting getCloudSetting() {
        return cloudSetting;
    }

    @Override
    public boolean shouldRenderUnderwaterOverlay() {
        return shouldRenderUnderwaterOverlay;
    }

    @Override
    public boolean shouldRenderVignette() {
        return shouldRenderVignette;
    }

    @Override
    public boolean shouldRenderSun() {
        return shouldRenderSun;
    }

    @Override
    public boolean shouldRenderMoon() {
        return shouldRenderMoon;
    }

    @Override
    public boolean shouldRenderWeather() {
        return shouldRenderWeather;
    }

    @Override
    public boolean shouldRenderWeatherParticles() {
        return shouldRenderWeatherParticles;
    }

    @Override
    public boolean shouldWriteRainAndSnowToDepthBuffer() {
        return shouldWriteRainAndSnowToDepthBuffer;
    }

    @Override
    public boolean shouldRenderParticlesBeforeDeferred() {
        return shouldRenderParticlesBeforeDeferred;
    }

    @Override
    public boolean allowConcurrentCompute() {
        return allowConcurrentCompute;
    }

    @Override
    public float getSunPathRotation() {
        return sunPathRotation;
    }

    private void matchPass() {
        if (!isRenderingWorld || isRenderingFullScreenPass || isPostChain || !isMainBound) {
            return;
        }

        if (StateTracker.INSTANCE.compilingDisplayList) {
            return;
        }

        if (celeritasTerrainRendering) {
            beginPass(table.match(passFactory.getCondition(getPhase()), new InputAvailability(true, true, false)));
            return;
        }

        beginPass(table.match(passFactory.getCondition(getPhase()), inputs));
    }

    public void beginPass(Pass pass) {
        if (current == pass) {
            return;
        }

        if (current != null) {
            current.stopUsing();
        }

        current = pass;

        if (pass != null) {
            pass.use();

            EntityColorState.upload();
        } else {
            Program.unbind();
        }
    }


    private boolean isPostChain;
    private boolean isMainBound = true;

    @Override
    public void setIsMainBound(boolean bound) {
        isMainBound = bound;

        if (!isRenderingWorld || isRenderingFullScreenPass || isPostChain) {
            return;
        }

        if (bound) {
            // force refresh
            current = null;
        } else {
            beginPass(null);
        }
    }

    @Override
    public void destroy() {
        PipelineTeardown.destroy(this);
    }

    private void prepareRenderTargets() {
        // Make sure we're using texture unit 0 for this.
        GlStateManager.activeTexture(GL15C.GL_TEXTURE0);
        Vector4f emptyClearColor = new Vector4f(1.0F);

        if (shadowRenderTargets != null) {
            if (packDirectives.getShadowDirectives().isShadowEnabled() == OptionalBoolean.FALSE) {
                if (shadowRenderTargets.isFullClearRequired()) {
                    shadowRenderTargets.onFullClear();
                    for (ClearPass clearPass : shadowClearPassesFull) {
                        clearPass.execute(emptyClearColor);
                    }
                }
            } else {
                // Clear depth first, regardless of any color clearing.
                shadowRenderTargets.getDepthSourceFb().bind();
                GlStateManager.clear(GL21C.GL_DEPTH_BUFFER_BIT);

                ImmutableList<ClearPass> passes;

                boolean ranShadowCompute = false;
                for (ComputeProgram computeProgram : shadowComputes) {
                    if (computeProgram != null) {
                        ranShadowCompute = true;
                        computeProgram.dispatch(shadowMapResolution, shadowMapResolution);
                    }
                }

                if (ranShadowCompute) {
                    // Without this, the compute-only program stays current (glUseProgram) for the rest of
                    // the frame - a compute-only program bound during a Draw command is invalid per spec,
                    // so every subsequent glDrawElements/glCallList this frame would raise
                    // GL_INVALID_OPERATION. See the equivalent unbind after dispatch in CompositeRenderer,
                    // ShadowCompositeRenderer, and FinalPassRenderer.
                    AurumRenderSystem.memoryBarrier(40);
                    Program.unbind();
                }

                if (shadowRenderTargets.isFullClearRequired()) {
                    passes = shadowClearPassesFull;
                    shadowRenderTargets.onFullClear();
                } else {
                    passes = shadowClearPasses;
                }

                for (ClearPass clearPass : passes) {
                    clearPass.execute(emptyClearColor);
                }
            }
        }

        Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
        DepthAttachedFramebuffer mainExt = (DepthAttachedFramebuffer) main;

        int depthTextureId = mainExt.getAurum$depthTextureId();
        int internalFormat = TextureInfoCache.INSTANCE.getInfo(depthTextureId).getInternalFormat();
        DepthBufferFormat depthBufferFormat = DepthBufferFormat.fromGlEnumOrDefault(internalFormat);

        boolean changed = renderTargets.resizeIfNeeded(mainExt.aurum$getDepthBufferVersion(), depthTextureId, main.viewportWidth,
                main.viewportHeight, depthBufferFormat, packDirectives);

        if (ssboHolder != null) {
            ssboHolder.hasResizedScreen(main.viewportWidth, main.viewportHeight);
        }

        for (GlImage image : customImages) {
            image.updateNewSize(main.viewportWidth, main.viewportHeight);
        }

        if (changed) {

            prepareRenderer.recalculateSizes();
            deferredRenderer.recalculateSizes();
            compositeRenderer.recalculateSizes();
            finalPassRenderer.recalculateSwapPassSize();

            this.clearPassesFull.forEach(clearPass -> renderTargets.destroyFramebuffer(clearPass.getFramebuffer()));
            this.clearPasses.forEach(clearPass -> renderTargets.destroyFramebuffer(clearPass.getFramebuffer()));

            this.clearPassesFull = ClearPassCreator.createClearPasses(renderTargets, true,
                    packDirectives.getRenderTargetDirectives());
            this.clearPasses = ClearPassCreator.createClearPasses(renderTargets, false,
                    packDirectives.getRenderTargetDirectives());
        }

        final ImmutableList<ClearPass> passes;

        if (renderTargets.isFullClearRequired()) {
            renderTargets.onFullClear();
            passes = clearPassesFull;
        } else {
            passes = clearPasses;
        }

        Vector3d fogColor3 = CapturedRenderingState.INSTANCE.getFogColor();

        // NB: The alpha value must be 1.0 here, or else you will get a bunch of bugs. Sildur's Vibrant Shaders
        //     will give you pink reflections and other weirdness if this is zero.
        Vector4f fogColor = new Vector4f((float) fogColor3.x, (float) fogColor3.y, (float) fogColor3.z, 1.0F);

        for (ClearPass clearPass : passes) {
            clearPass.execute(fogColor);
        }

        renderTargets.clearDepth();

        // Reset framebuffer and viewport
        MinecraftClient.getInstance().getFramebuffer().bind(true);
    }

    @Override
    public void beginHand() {
        // We need to copy the current depth texture so that depthtex2 can contain the depth values for
        // all non-translucent content without the hand, as required.
        renderTargets.copyPreHandDepth();
    }

    @Override
    public void beginTranslucents() {
        isBeforeTranslucent = false;

        // We need to copy the current depth texture so that depthtex1 can contain the depth values for
        // all non-translucent content, as required.
        renderTargets.copyPreTranslucentDepth();


        // needed to remove blend mode overrides and similar
        beginPass(null);

        isRenderingFullScreenPass = true;

        deferredRenderer.renderAll();

        GlStateManager.enableBlend();
        GlStateManager.enableAlphaTest();

        // note: we are careful not to touch the lightmap texture unit or overlay color texture unit here,
        // so we don't need to do anything to restore them if needed.
        //
        // Previous versions of the code tried to "restore" things by enabling the lightmap & overlay color
        // but that actually broke rendering of clouds and rain by making them appear red in the case of
        // a pack not overriding those shader programs.
        //
        // Not good!

        isRenderingFullScreenPass = false;
    }

    @Override
    public void renderShadows(WorldRendererAccessor levelRenderer) {
        if (shouldRenderPrepareBeforeShadow) {
            isRenderingFullScreenPass = true;

            prepareRenderer.renderAll();

            isRenderingFullScreenPass = false;
        }

        if (shadowRenderer != null) {
            isRenderingShadow = true;

            try {
                shadowRenderer.renderShadows(levelRenderer);
            } finally {
                // If renderShadows throws partway through, leaving this true would make every subsequent
                // terrain/entity draw this frame (and every frame after, until the pipeline gets recreated)
                // silently pick RenderCondition.SHADOW instead of its real condition in getCondition().
                isRenderingShadow = false;

                // needed to remove blend mode overrides and similar
                beginPass(null);
            }
        }

        if (!shouldRenderPrepareBeforeShadow) {
            isRenderingFullScreenPass = true;

            prepareRenderer.renderAll();

            isRenderingFullScreenPass = false;
        }
    }

    @Override
    public void addDebugText(List<String> messages) {
        messages.add("");

        if (shadowRenderer != null) {
            shadowRenderer.addDebugText(messages);
        } else {
            messages.add("[Aurum] Shadow Maps: not used by shader pack");
        }
    }

    @Override
    public OptionalInt getForcedShadowRenderDistanceChunksForDisplay() {
        if (forcedShadowRenderDistanceChunks != null) return OptionalInt.of(forcedShadowRenderDistanceChunks);
        return OptionalInt.empty();
    }

    private boolean isRenderingWorld = false;
    private boolean isRenderingFullScreenPass = false;

    @Override
    public void beginLevelRendering() {
        isRenderingFullScreenPass = false;
        isRenderingWorld = true;
        isBeforeTranslucent = true;
        isMainBound = true;
        isPostChain = false;
        phase = WorldRenderingPhase.NONE;
        overridePhase = null;

        checkWorld();

        if (!isRenderingWorld) {
            Aurum.LOGGER.warn("beginWorldRender was called but we are not currently rendering a world?");
            return;
        }

        if (current != null) {
            throw new IllegalStateException("Called beginLevelRendering but level rendering appears to still be in progress?");
        }

        updateNotifier.onNewFrame();
        customUniforms.update();

        if (ssboHolder != null) {
            ssboHolder.setupBuffers();
        }

        for (GlImage image : imagesToClear) {
            image.clear();
        }

        // Get ready for world rendering
        prepareRenderTargets();

        setPhase(WorldRenderingPhase.SKY);
    }

    @Override
    public void renderHorizonBox() {
        checkWorld();

        if (!isRenderingWorld) {
            return;
        }

        setPhase(WorldRenderingPhase.SKY);

        // Render our horizon box before actual sky rendering to avoid being broken by mods that do weird things
        // while rendering the sky.
        //
        // A lot of dimension mods touch sky rendering, FabricSkyboxes injects at HEAD and cancels, etc.
        GlStateManager.disableTexture();
        GlStateManager.depthMask(false);

        Vector3d fogColor = CapturedRenderingState.INSTANCE.getFogColor();
        GlStateManager.color((float) fogColor.x, (float) fogColor.y, (float) fogColor.z);

        horizonRenderer.renderHorizon();

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture();
    }

    @Override
    public void finalizeLevelRendering() {
        checkWorld();

        if (!isRenderingWorld) {
            Aurum.LOGGER.warn("finalizeWorldRendering was called but we are not currently rendering a world?");
            return;
        }

        beginPass(null);


        isRenderingWorld = false;
        phase = WorldRenderingPhase.NONE;
        overridePhase = null;

        isRenderingFullScreenPass = true;

        centerDepthSampler.sampleCenterDepth();

        compositeRenderer.renderAll();
        finalPassRenderer.renderFinalPass();

        isRenderingFullScreenPass = false;
    }

    @Override
    public CeleritasTerrainPipeline getCeleritasTerrainPipeline() {
        return celeritasTerrainPipeline;
    }

    @Override
    public FrameUpdateNotifier getFrameUpdateNotifier() {
        return updateNotifier;
    }

    @Override
    public WorldRenderingPhase getPhase() {
        if (overridePhase != null) {
            return overridePhase;
        }

        return phase;
    }

    public boolean celeritasTerrainRendering = false;

    @Override
    public void syncProgram() {
        matchPass();
    }

    @Override
    public void beginCeleritasTerrainRendering() {
        celeritasTerrainRendering = true;
        syncProgram();
    }

    @Override
    public void endCeleritasTerrainRendering() {
        celeritasTerrainRendering = false;
        current = null;
        syncProgram();
    }

    @Override
    public void setOverridePhase(WorldRenderingPhase phase) {
        this.overridePhase = phase;

        GbufferPrograms.runPhaseChangeNotifier();
    }

    @Override
    public void setPhase(WorldRenderingPhase phase) {
        this.phase = phase;

        GbufferPrograms.runPhaseChangeNotifier();
    }

    @Override
    public void setInputs(InputAvailability availability) {
        this.inputs = availability;
    }

    @Override
    public void setSpecialCondition(SpecialCondition special) {
        this.special = special;

        GbufferPrograms.runPhaseChangeNotifier();
    }

    @Override
    public RenderTargetStateListener getRenderTargetStateListener() {
        return this;
    }

    @Override
    public int getCurrentNormalTexture() {
        return currentNormalTexture;
    }

    @Override
    public int getCurrentSpecularTexture() {
        return currentSpecularTexture;
    }

    @Override
    public void onBindTexture(int id) {
        if (shouldBindPBR && isRenderingWorld) {
            PBRTextureHolder pbrHolder = PBRTextureManager.INSTANCE.getOrLoadHolder(id);
            currentNormalTexture = pbrHolder.normalTexture().getGlId();
            currentSpecularTexture = pbrHolder.specularTexture().getGlId();

            TextureFormat textureFormat = TextureFormatLoader.getFormat();
            if (textureFormat != null) {
                textureFormat.setupTextureParameters(PBRType.NORMAL, pbrHolder.normalTexture());
                textureFormat.setupTextureParameters(PBRType.SPECULAR, pbrHolder.specularTexture());
            }

            PBRTextureManager.notifyPBRTexturesChanged();
        }
    }
}