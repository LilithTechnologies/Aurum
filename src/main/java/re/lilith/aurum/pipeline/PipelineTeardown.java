package re.lilith.aurum.pipeline;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL30C;
import re.lilith.aurum.gbuffer.matching.ProgramTable;
import re.lilith.aurum.gl.blending.AlphaTestOverride;
import re.lilith.aurum.gl.blending.BlendModeOverride;
import re.lilith.aurum.gl.image.GlImage;
import re.lilith.aurum.pipeline.impl.DeferredWorldRenderingPipeline;
import re.lilith.aurum.pipeline.pathways.pass.Pass;
import re.lilith.aurum.uniforms.utility.EntityColorState;

import java.util.HashSet;
import java.util.Set;

public final class PipelineTeardown {
    private PipelineTeardown() {
    }

    public static void destroy(DeferredWorldRenderingPipeline pipeline) {
        BlendModeOverride.restore();
        AlphaTestOverride.restore();

        // Cached uniform locations belong to programs that are about to be deleted.
        EntityColorState.clearCache();

        if (pipeline.ssboHolder != null) {
            pipeline.ssboHolder.destroyBuffers();
        }

        for (GlImage image : pipeline.customImages) {
            image.destroy();
        }

        destroyPasses(pipeline.table);

        // Destroy the composite rendering pipeline
        //
        // This destroys all the loaded composite programs as well.
        pipeline.compositeRenderer.destroy();
        pipeline.deferredRenderer.destroy();
        pipeline.finalPassRenderer.destroy();
        pipeline.centerDepthSampler.destroy();

        if (pipeline.shadowCompositeRenderer != null) {
            pipeline.shadowCompositeRenderer.destroy();
        }

        pipeline.horizonRenderer.destroy();

        // Make sure that any custom framebuffers are not bound before destroying render targets
        GL30.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, 0);
        GL30.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, 0);
        GL30.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, 0);

        MinecraftClient.getInstance().getFramebuffer().bind(false);

        // Destroy our render targets
        //
        // While it's possible to just clear them instead and reuse them, we'd need to investigate whether or not this
        // would help performance.
        pipeline.renderTargets.destroy();

        // destroy the shadow render targets
        if (pipeline.shadowRenderTargets != null) {
            pipeline.shadowRenderTargets.destroy();
        }

        // Destroy custom textures and the static samplers (normals, specular, and noise)
        pipeline.customTextureManager.destroy();
        pipeline.whitePixel.clearGlId();
    }

    private static void destroyPasses(ProgramTable<Pass> table) {
        Set<Pass> destroyed = new HashSet<>();

        table.forEach(pass -> {
            if (pass == null) {
                return;
            }

            if (destroyed.contains(pass)) {
                return;
            }

            pass.destroy();
            destroyed.add(pass);
        });
    }
}
