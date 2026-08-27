package re.lilith.aurum.pipeline.impl.celeritas;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.Nullable;
import re.lilith.aurum.celeritas.terrain.AurumTerrainPass;
import re.lilith.aurum.gl.GlFramebuffer;
import re.lilith.aurum.gl.program.ProgramImages;
import re.lilith.aurum.gl.program.ProgramSamplers;
import re.lilith.aurum.gl.program.ProgramUniforms;
import re.lilith.aurum.pipeline.WorldRenderingPipeline;
import re.lilith.aurum.pipeline.transform.ShaderTransformer;
import re.lilith.aurum.pipeline.transform.patch.PatchShaderType;
import re.lilith.aurum.pipeline.transform.patch.PatchedShaderPrinter;
import re.lilith.aurum.shaderpack.program.ProgramSet;
import re.lilith.aurum.shaderpack.program.ProgramSource;
import re.lilith.aurum.targets.render.RenderTargets;
import re.lilith.aurum.uniforms.CommonUniforms;
import re.lilith.aurum.uniforms.builtin.BuiltinReplacementUniforms;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntFunction;

public class CeleritasTerrainPipeline {
    @Nullable String terrainVertex = null;
    @Nullable String terrainGeometry = null;
    @Nullable String terrainFragment = null;
    @Nullable String translucentVertex = null;
    @Nullable String translucentGeometry = null;
    @Nullable String translucentFragment = null;
    @Nullable String shadowVertex = null;
    @Nullable String shadowGeometry = null;
    @Nullable String shadowFragment = null;
    private final EnumMap<AurumTerrainPass, GlFramebuffer> framebuffers = new EnumMap<>(AurumTerrainPass.class);
    ProgramSet programSet;

    private final WorldRenderingPipeline parent;

    private final IntFunction<ProgramSamplers> createTerrainSamplers;
    private final IntFunction<ProgramSamplers> createShadowSamplers;

    private final IntFunction<ProgramImages> createTerrainImages;
    private final IntFunction<ProgramImages> createShadowImages;

    public CeleritasTerrainPipeline(WorldRenderingPipeline parent,
                                    ProgramSet programSet, IntFunction<ProgramSamplers> createTerrainSamplers,
                                    IntFunction<ProgramSamplers> createShadowSamplers,
                                    IntFunction<ProgramImages> createTerrainImages,
                                    IntFunction<ProgramImages> createShadowImages,
                                    RenderTargets renderTargets,
                                    ImmutableSet<Integer> flippedAfterPrepare,
                                    ImmutableSet<Integer> flippedAfterTranslucent) {
        this.parent = Objects.requireNonNull(parent);

        Optional<ProgramSource> terrainSource = first(programSet.getGbuffersTerrain(), programSet.getGbuffersTexturedLit(), programSet.getGbuffersTextured(), programSet.getGbuffersBasic());
        Optional<ProgramSource> translucentSource = first(programSet.getGbuffersWater(), terrainSource);
        Optional<ProgramSource> shadowSource = programSet.getShadow();

        this.programSet = programSet;

        terrainSource.ifPresent(sources -> {
            Map<PatchShaderType, String> result = ShaderTransformer.patchCeleritasTerrain(
                    sources.getVertexSource().orElse(null),
                    sources.getGeometrySource().orElse(null),
                    sources.getFragmentSource().orElse(null));

            if (result == null) throw new IllegalStateException("Celeritas Terrain failed to patch!");

            terrainVertex = result.get(PatchShaderType.VERTEX);
            terrainGeometry = result.get(PatchShaderType.GEOMETRY);
            terrainFragment = result.get(PatchShaderType.FRAGMENT);

            PatchedShaderPrinter.debugPatchedShaders(sources.getName() + "_celeritas",
                    terrainVertex, terrainGeometry, terrainFragment);
        });

        translucentSource.ifPresent(sources -> {
            Map<PatchShaderType, String> result = ShaderTransformer.patchCeleritasTerrain(
                    sources.getVertexSource().orElse(null),
                    sources.getGeometrySource().orElse(null),
                    sources.getFragmentSource().orElse(null));

            if (result == null) throw new IllegalStateException("Celeritas Terrain failed to patch!");

            translucentVertex = result.get(PatchShaderType.VERTEX);
            translucentGeometry = result.get(PatchShaderType.GEOMETRY);
            translucentFragment = result.get(PatchShaderType.FRAGMENT);

            PatchedShaderPrinter.debugPatchedShaders(sources.getName() + "_celeritas",
                    translucentVertex, translucentGeometry, translucentFragment);
        });

        shadowSource.ifPresent(sources -> {
            Map<PatchShaderType, String> result = ShaderTransformer.patchCeleritasTerrain(
                    sources.getVertexSource().orElse(null),
                    sources.getGeometrySource().orElse(null),
                    sources.getFragmentSource().orElse(null));

            if (result == null) throw new IllegalStateException("Celeritas Terrain failed to patch!");

            shadowVertex = result.get(PatchShaderType.VERTEX);
            shadowGeometry = result.get(PatchShaderType.GEOMETRY);
            shadowFragment = result.get(PatchShaderType.FRAGMENT);

            PatchedShaderPrinter.debugPatchedShaders(sources.getName() + "_celeritas",
                    shadowVertex, shadowGeometry, shadowFragment);
        });

        this.createTerrainSamplers = createTerrainSamplers;
        this.createShadowSamplers = createShadowSamplers;
        this.createTerrainImages = createTerrainImages;
        this.createShadowImages = createShadowImages;


        if (renderTargets != null) {
            terrainSource.ifPresent(source -> this.framebuffers.put(AurumTerrainPass.GBUFFER_SOLID,
                    renderTargets.createGbufferFramebuffer(flippedAfterPrepare, source.getDirectives().getDrawBuffers())));
            translucentSource.ifPresent(source -> this.framebuffers.put(AurumTerrainPass.GBUFFER_TRANSLUCENT,
                    renderTargets.createGbufferFramebuffer(flippedAfterTranslucent, source.getDirectives().getDrawBuffers())));
        }
    }

    public Optional<String> getTerrainVertexShaderSource() {
        return Optional.ofNullable(terrainVertex);
    }

    public Optional<String> getTerrainGeometryShaderSource() {
        return Optional.ofNullable(terrainGeometry);
    }

    public Optional<String> getTerrainFragmentShaderSource() {
        return Optional.ofNullable(terrainFragment);
    }

    public Optional<String> getTranslucentVertexShaderSource() {
        return Optional.ofNullable(translucentVertex);
    }

    public Optional<String> getTranslucentGeometryShaderSource() {
        return Optional.ofNullable(translucentGeometry);
    }

    public Optional<String> getTranslucentFragmentShaderSource() {
        return Optional.ofNullable(translucentFragment);
    }

    public Optional<String> getShadowVertexShaderSource() {
        return Optional.ofNullable(shadowVertex);
    }

    public Optional<String> getShadowGeometryShaderSource() {
        return Optional.ofNullable(shadowGeometry);
    }

    public Optional<String> getShadowFragmentShaderSource() {
        return Optional.ofNullable(shadowFragment);
    }

    public ProgramUniforms initUniforms(int programId) {
        ProgramUniforms.Builder uniforms = ProgramUniforms.builder("<celeritas shaders>", programId);

        CommonUniforms.addCommonUniforms(uniforms, programSet.getPack().getIdMap(), programSet.getPackDirectives(), parent.getFrameUpdateNotifier());
        BuiltinReplacementUniforms.addBuiltinReplacementUniforms(uniforms);

        return uniforms.buildUniforms();
    }

    public boolean hasShadowPass() {
        return createShadowSamplers != null;
    }

    public ProgramSamplers initTerrainSamplers(int programId) {
        return createTerrainSamplers.apply(programId);
    }

    public ProgramSamplers initShadowSamplers(int programId) {
        return createShadowSamplers.apply(programId);
    }

    public ProgramImages initTerrainImages(int programId) {
        return createTerrainImages.apply(programId);
    }

    public ProgramImages initShadowImages(int programId) {
        return createShadowImages.apply(programId);
    }

    @SafeVarargs
    private static <T> Optional<T> first(Optional<T>... candidates) {
        for (Optional<T> candidate : candidates) {
            if (candidate.isPresent()) {
                return candidate;
            }
        }

        return Optional.empty();
    }
}
