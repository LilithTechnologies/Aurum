package re.lilith.aurum.pipeline;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL20C;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.pipeline.impl.FixedFunctionWorldRenderingPipeline;
import re.lilith.aurum.shaderpack.DimensionId;
import re.lilith.aurum.uniforms.SystemTimeUniforms;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class PipelineManager {
    private final Function<DimensionId, WorldRenderingPipeline> pipelineFactory;
    private final Map<DimensionId, WorldRenderingPipeline> pipelinesPerDimension = new HashMap<>();
    private WorldRenderingPipeline pipeline = new FixedFunctionWorldRenderingPipeline();
    private int versionCounterForCeleritasShaderReload = 0;

    public PipelineManager(Function<DimensionId, WorldRenderingPipeline> pipelineFactory) {
        this.pipelineFactory = pipelineFactory;
    }

    public WorldRenderingPipeline preparePipeline(DimensionId currentDimension) {
        if (!pipelinesPerDimension.containsKey(currentDimension)) {
            SystemTimeUniforms.COUNTER.reset();
            SystemTimeUniforms.TIMER.reset();

            Aurum.LOGGER.info("Creating pipeline for dimension {}", currentDimension);
            pipeline = pipelineFactory.apply(currentDimension);
            pipelinesPerDimension.put(currentDimension, pipeline);

            if (MinecraftClient.getInstance().worldRenderer != null) {
                MinecraftClient.getInstance().worldRenderer.reload();
            }
        } else {
            pipeline = pipelinesPerDimension.get(currentDimension);
        }

        return pipeline;
    }

    public WorldRenderingPipeline getPipelineNullable() {
        return pipeline;
    }

    public Optional<WorldRenderingPipeline> getPipeline() {
        return Optional.ofNullable(pipeline);
    }

    /**
     * In AurumChunkProgramOverrides#getProgramOverride,
     * it uses version counter to check whether to reload Celeritas shaders.
     * This fixes a compat issue with Immersive Portals(#1188).
     * Immersive Portals may load multiple client dimensions at the same time,
     * and every dimension corresponds to a AurumChunkProgramOverrides object.
     * Multiple dimensions (mod dimensions that fallback to overworld shaders) may use the same pipeline.
     * This ensures that the Celeritas shader for each dimension will get properly reloaded.
     */
    public int getVersionCounterForCeleritasShaderReload() {
        return versionCounterForCeleritasShaderReload;
    }

    /**
     * Destroys all the current pipelines.
     *
     * <p>This method is <b>EXTREMELY DANGEROUS!</b> It is a huge potential source of hard-to-trace inconsistencies
     * in program state. You must make sure that you <i>immediately</i> re-prepare the pipeline after destroying
     * it to prevent the program from falling into an inconsistent state.</p>
     *
     * <p>In particular, </p>
     *
     * @see <a href="https://github.com/IrisShaders/Iris/issues/1330">this GitHub issue</a>
     */
    public void destroyPipeline() {
        pipelinesPerDimension.forEach((dimensionId, pipeline) -> {
            Aurum.LOGGER.info("Destroying pipeline {}", dimensionId);
            resetTextureState();
            pipeline.destroy();
        });

        pipelinesPerDimension.clear();
        pipeline = null;
        versionCounterForCeleritasShaderReload++;
    }

    private void resetTextureState() {
        // Unbind all textures
        //
        // This is necessary because we don't want destroyed render target textures to remain bound to certain texture
        // units. Vanilla appears to properly rebind all textures as needed, and we do so too, so this does not cause
        // issues elsewhere.
        //
        // Without this code, there will be weird issues when reloading certain shaderpacks.
        for (int i = 0; i < 16; i++) {
            GlStateManager.activeTexture(GL20C.GL_TEXTURE0 + i);
            GlStateManager.bindTexture(0);
        }

        // Set the active texture unit to unit 0
        //
        // This seems to be what most code expects. It's a sane default in any case.
        GlStateManager.activeTexture(GL20C.GL_TEXTURE0);
    }
}
