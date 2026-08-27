package re.lilith.aurum.celeritas.terrain;

import org.embeddedt.embeddium.impl.gl.shader.GlProgram;
import org.embeddedt.embeddium.impl.gl.shader.GlShader;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderInterface;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.jetbrains.annotations.Nullable;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.gbuffer.BlockRenderingSettings;
import re.lilith.aurum.gl.program.ProgramImages;
import re.lilith.aurum.gl.program.ProgramSamplers;
import re.lilith.aurum.gl.program.ProgramUniforms;
import re.lilith.aurum.pipeline.WorldRenderingPipeline;
import re.lilith.aurum.pipeline.impl.celeritas.CeleritasTerrainPipeline;
import re.lilith.aurum.pipeline.pathways.shadows.ShadowRenderer;

import java.util.*;

public class AurumChunkProgramOverrides {
    private static final String TRANSLUCENT_PASS_NAME = "translucent";

    private final EnumMap<AurumTerrainPass, GlProgram<ChunkShaderInterface>> programs = new EnumMap<>(AurumTerrainPass.class);

    private int cachedPipelineVersion = -1;
    private boolean programsValid = false;

    @Nullable
    private static GlShader createShader(ShaderType type, AurumTerrainPass pass, Optional<String> source) {
        String text = source.orElse(null);

        if (text == null) {
            return null;
        }

        String name = "aurum:terrain-" + pass.toString().toLowerCase(Locale.ROOT) + "." + type.fileExtension;

        return new GlShader(type, name, text);
    }

    private static Optional<String> getVertexSource(AurumTerrainPass pass, CeleritasTerrainPipeline pipeline) {
        return switch (pass) {
            case SHADOW -> pipeline.getShadowVertexShaderSource();
            case GBUFFER_SOLID -> pipeline.getTerrainVertexShaderSource();
            case GBUFFER_TRANSLUCENT -> pipeline.getTranslucentVertexShaderSource();
        };
    }

    private static Optional<String> getGeometrySource(AurumTerrainPass pass, CeleritasTerrainPipeline pipeline) {
        return switch (pass) {
            case SHADOW -> pipeline.getShadowGeometryShaderSource();
            case GBUFFER_SOLID -> pipeline.getTerrainGeometryShaderSource();
            case GBUFFER_TRANSLUCENT -> pipeline.getTranslucentGeometryShaderSource();
        };
    }

    private static Optional<String> getFragmentSource(AurumTerrainPass pass, CeleritasTerrainPipeline pipeline) {
        return switch (pass) {
            case SHADOW -> pipeline.getShadowFragmentShaderSource();
            case GBUFFER_SOLID -> pipeline.getTerrainFragmentShaderSource();
            case GBUFFER_TRANSLUCENT -> pipeline.getTranslucentFragmentShaderSource();
        };
    }

    @Nullable
    private GlProgram<ChunkShaderInterface> createProgram(AurumTerrainPass pass, CeleritasTerrainPipeline pipeline) {
        GlShader vertex = createShader(ShaderType.VERTEX, pass, getVertexSource(pass, pipeline));
        GlShader geometry = createShader(ShaderType.GEOM, pass, getGeometrySource(pass, pipeline));
        GlShader fragment = createShader(ShaderType.FRAGMENT, pass, getFragmentSource(pass, pipeline));

        List<GlShader> attached = new ArrayList<>(3);

        if (vertex != null) {
            attached.add(vertex);
        }

        if (geometry != null) {
            attached.add(geometry);
        }

        if (fragment != null) {
            attached.add(fragment);
        }

        try {
            if (vertex == null || fragment == null) {
                return null;
            }

            GlProgram.Builder builder = GlProgram.builder("aurum:chunk_shader_for_" + pass.getName());
            attached.forEach(builder::attachShader);

            int index = 0;

            for (var attribute : AurumChunkVertexType.VERTEX_FORMAT.getAttributes()) {
                builder.bindAttribute(attribute.getName(), index++);
            }

            return builder.link(context -> {
                // The program is linked before the interface factory runs, so the handle is available here.
                int programId = ((GlProgram<?>) context).handle();

                ProgramUniforms uniforms = pipeline.initUniforms(programId);
                ProgramSamplers samplers;
                ProgramImages images;

                if (pass == AurumTerrainPass.SHADOW) {
                    samplers = pipeline.initShadowSamplers(programId);
                    images = pipeline.initShadowImages(programId);
                } else {
                    samplers = pipeline.initTerrainSamplers(programId);
                    images = pipeline.initTerrainImages(programId);
                }

                return new AurumChunkShaderInterface(context, programId, pass, uniforms, samplers, images);
            });
        } catch (RuntimeException e) {
            Aurum.LOGGER.error("Failed to create the terrain program for {}", pass.getName(), e);
            return null;
        } finally {
            attached.forEach(GlShader::delete);
        }
    }

    private void createPrograms(@Nullable CeleritasTerrainPipeline pipeline) {
        if (pipeline == null) {
            Aurum.LOGGER.info("Terrain programs not built: the active pipeline supplies none");
            return;
        }

        for (AurumTerrainPass pass : AurumTerrainPass.values()) {
            if (pass == AurumTerrainPass.SHADOW && !pipeline.hasShadowPass()) {
                continue;
            }

            GlProgram<ChunkShaderInterface> program = this.createProgram(pass, pipeline);

            if (program != null) {
                this.programs.put(pass, program);
            }
        }

        Aurum.LOGGER.info("Built {} terrain programs for Argentum", this.programs.size());
    }

    /**
     * The pipeline is never null, because Aurum falls back to a fixed function pipeline with no shader pack. Only a
     * pack that supplies terrain programs may take terrain rendering over from Celeritas.
     *
     * @return true while a shader pack drives terrain rendering
     */
    public static boolean isShaderPackDrivingTerrain() {
        return BlockRenderingSettings.INSTANCE.shouldUseExtendedVertexFormat();
    }

    public boolean isActive() {
        return isShaderPackDrivingTerrain();
    }

    /**
     * @return the Aurum program for this pass, or null if the pack has no usable program for it
     */
    @Nullable
    public GlProgram<ChunkShaderInterface> getProgramOverride(TerrainRenderPass pass) {
        WorldRenderingPipeline worldRenderingPipeline = Aurum.getPipelineManager().getPipelineNullable();
        CeleritasTerrainPipeline pipeline = worldRenderingPipeline == null ? null : worldRenderingPipeline.getCeleritasTerrainPipeline();

        int version = Aurum.getPipelineManager().getVersionCounterForCeleritasShaderReload();

        if (version != this.cachedPipelineVersion) {
            this.cachedPipelineVersion = version;
            this.programsValid = false;
        }

        if (!this.programsValid) {
            this.deletePrograms();
            this.createPrograms(pipeline);
            this.programsValid = true;
        }

        if (pipeline == null) {
            return null;
        }

        if (ShadowRenderer.ACTIVE) {
            return this.programs.get(AurumTerrainPass.SHADOW);
        }

        return this.programs.get(isTranslucent(pass) ? AurumTerrainPass.GBUFFER_TRANSLUCENT : AurumTerrainPass.GBUFFER_SOLID);
    }

    private static boolean isTranslucent(TerrainRenderPass pass) {
        return TRANSLUCENT_PASS_NAME.equals(pass.name());
    }

    public void deletePrograms() {
        this.programs.values().forEach(GlProgram::delete);
        this.programs.clear();
        this.programsValid = false;
    }
}
