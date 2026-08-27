package re.lilith.aurum.pipeline.pathways.pass;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.jetbrains.annotations.Nullable;
import re.lilith.aurum.gl.GlFramebuffer;
import re.lilith.aurum.gl.blending.AlphaTestOverride;
import re.lilith.aurum.gl.blending.BlendModeOverride;
import re.lilith.aurum.gl.blending.BufferBlendOverride;
import re.lilith.aurum.gl.blending.DepthColorStorage;
import re.lilith.aurum.gl.program.Program;
import re.lilith.aurum.pipeline.impl.DeferredWorldRenderingPipeline;

import java.util.List;

public final class Pass {
    private final DeferredWorldRenderingPipeline pipeline;
    @Nullable
    private final Program program;
    private final GlFramebuffer framebufferBeforeTranslucents;
    private final GlFramebuffer framebufferAfterTranslucents;
    @Nullable
    private final AlphaTestOverride alphaTestOverride;
    @Nullable
    private final BlendModeOverride blendModeOverride;
    @Nullable
    private final List<BufferBlendOverride> bufferBlendOverrides;
    private final boolean shadowViewport;

    public Pass(DeferredWorldRenderingPipeline pipeline, @Nullable Program program, GlFramebuffer framebufferBeforeTranslucents, GlFramebuffer framebufferAfterTranslucents,
                @Nullable AlphaTestOverride alphaTestOverride, @Nullable BlendModeOverride blendModeOverride, @Nullable List<BufferBlendOverride> bufferBlendOverrides, boolean shadowViewport) {
        this.pipeline = pipeline;
        this.program = program;
        this.framebufferBeforeTranslucents = framebufferBeforeTranslucents;
        this.framebufferAfterTranslucents = framebufferAfterTranslucents;
        this.alphaTestOverride = alphaTestOverride;
        this.blendModeOverride = blendModeOverride;
        this.bufferBlendOverrides = bufferBlendOverrides;
        this.shadowViewport = shadowViewport;
    }

    public void use() {
        DepthColorStorage.unlockDepthColor();

        if (pipeline.isBeforeTranslucent) {
            framebufferBeforeTranslucents.bind();
        } else {
            framebufferAfterTranslucents.bind();
        }

        if (shadowViewport) {
            GlStateManager.viewport(0, 0, pipeline.shadowMapResolution, pipeline.shadowMapResolution);
        } else {
            Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
            GlStateManager.viewport(0, 0, main.viewportWidth, main.viewportHeight);
        }

        if (!pipeline.celeritasTerrainRendering) {
            if (program != null) {
                program.use();
                pipeline.customUniforms.push(program);
            } else {
                Program.unbind();
            }
        }

        if (alphaTestOverride != null) {
            alphaTestOverride.apply();
        } else {
            AlphaTestOverride.restore();
        }

        if (blendModeOverride != null) {
            blendModeOverride.apply();
        } else {
            BlendModeOverride.restore();
        }

        if (bufferBlendOverrides != null && !bufferBlendOverrides.isEmpty()) {
            bufferBlendOverrides.forEach(BufferBlendOverride::apply);
        }
    }

    public void stopUsing() {
        DepthColorStorage.unlockDepthColor();

        if (alphaTestOverride != null) {
            AlphaTestOverride.restore();
        }

        if (blendModeOverride != null || (bufferBlendOverrides != null && !bufferBlendOverrides.isEmpty())) {
            BlendModeOverride.restore();
        }
    }

    @Nullable
    public Program getProgram() {
        return program;
    }

    public void destroy() {
        if (this.program != null) {
            this.program.destroy();
        }
    }
}
