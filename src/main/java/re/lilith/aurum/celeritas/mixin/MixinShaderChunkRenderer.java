package re.lilith.aurum.celeritas.mixin;

import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.shader.GlProgram;
import org.embeddedt.embeddium.impl.render.chunk.ShaderChunkRenderer;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderInterface;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.celeritas.terrain.AurumChunkProgramOverrides;
import re.lilith.aurum.pipeline.WorldRenderingPhase;
import re.lilith.aurum.pipeline.WorldRenderingPipeline;

@Mixin(ShaderChunkRenderer.class)
public abstract class MixinShaderChunkRenderer {
    @Shadow
    protected GlProgram<ChunkShaderInterface> activeProgram;

    @Unique
    private static final AurumChunkProgramOverrides AURUM$PROGRAM_OVERRIDES = new AurumChunkProgramOverrides();

    @Inject(method = "begin", at = @At("HEAD"), cancellable = true)
    private void aurum$beginWithShaderPack(TerrainRenderPass pass, CallbackInfo ci) {

        if (!AURUM$PROGRAM_OVERRIDES.isActive()) {
            return;
        }

        ci.cancel();

        WorldRenderingPipeline pipeline = Aurum.getPipelineManager().getPipelineNullable();

        if (pipeline != null) {
            pipeline.setPhase(aurum$phaseOf(pass));
            pipeline.beginCeleritasTerrainRendering();
        }

        pass.startDrawing();

        GlProgram<ChunkShaderInterface> program = AURUM$PROGRAM_OVERRIDES.getProgramOverride(pass);
        this.activeProgram = program;


        if (program != null) {
            program.bind();
            program.getInterface().setupState(pass);
        }
    }

    @Inject(method = "end", at = @At("RETURN"))
    private void aurum$endWithShaderPack(TerrainRenderPass pass, CallbackInfo ci) {
        if (!AURUM$PROGRAM_OVERRIDES.isActive()) {
            return;
        }

        WorldRenderingPipeline pipeline = Aurum.getPipelineManager().getPipelineNullable();

        if (pipeline != null) {
            pipeline.endCeleritasTerrainRendering();
            pipeline.setPhase(WorldRenderingPhase.NONE);
        }
    }

    @Unique
    private static WorldRenderingPhase aurum$phaseOf(TerrainRenderPass pass) {
        return switch (pass.name()) {
            case "translucent" -> WorldRenderingPhase.TERRAIN_TRANSLUCENT;
            case "cutout_mipped" -> WorldRenderingPhase.TERRAIN_CUTOUT_MIPPED;
            default -> WorldRenderingPhase.TERRAIN_SOLID;
        };
    }

    @Inject(method = "delete", at = @At("HEAD"))
    private void aurum$deletePrograms(CommandList commandList, CallbackInfo ci) {
        AURUM$PROGRAM_OVERRIDES.deletePrograms();
    }
}
