package re.lilith.aurum.pipeline.pathways.shadows;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.ARBTextureSwizzle;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.shaderpack.PackShadowDirectives;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies a shadow pack's requested depth/color sampler settings (filtering, hardware comparison,
 * mipmapping) to a {@link ShadowRenderTargets}, and regenerates mipmaps for any texture that asked for them.
 */
class ShadowSamplingConfigurer {
    private final ShadowRenderTargets targets;
    private final int resolution;
    private final float halfPlaneLength;
    private final List<MipmapPass> mipmapPasses = new ArrayList<>();

    ShadowSamplingConfigurer(ShadowRenderTargets targets, int resolution, float halfPlaneLength) {
        this.targets = targets;
        this.resolution = resolution;
        this.halfPlaneLength = halfPlaneLength;
    }

    void configure(PackShadowDirectives shadowDirectives) {
        final ImmutableList<PackShadowDirectives.DepthSamplingSettings> depthSamplingSettings =
                shadowDirectives.getDepthSamplingSettings();

        final ImmutableList<PackShadowDirectives.SamplingSettings> colorSamplingSettings =
                shadowDirectives.getColorSamplingSettings();

        Aurum.LOGGER.info("Shadow map: {}x{} halfPlane={} depth0={} depth1={} color={}", resolution, resolution, halfPlaneLength, depthSamplingSettings.get(0), depthSamplingSettings.get(1), colorSamplingSettings);

        GlStateManager.activeTexture(GL20C.GL_TEXTURE4);

        configureDepthSampler(targets.getDepthTexture().getTextureId(), depthSamplingSettings.get(0));
        configureDepthSampler(targets.getDepthTextureNoTranslucents().getTextureId(), depthSamplingSettings.get(1));

        for (int i = 0; i < colorSamplingSettings.size(); i++) {
            int glTextureId = targets.get(i).getMainTexture();

            configureSampler(glTextureId, colorSamplingSettings.get(i));
        }

        GlStateManager.activeTexture(GL20C.GL_TEXTURE0);
    }

    private void configureDepthSampler(int glTextureId, PackShadowDirectives.DepthSamplingSettings settings) {
        if (settings.getHardwareFiltering()) {
            // We have to do this or else shadow hardware filtering breaks entirely!
            AurumRenderSystem.texParameteri(glTextureId, GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_COMPARE_MODE, GL30C.GL_COMPARE_REF_TO_TEXTURE);
        }

        // Workaround for issues with old shader packs like Chocapic v4.
        // They expected the driver to put the depth value in z, but it's supposed to only
        // be available in r. So we set up the swizzle to fix that.
        AurumRenderSystem.texParameteriv(glTextureId, GL20C.GL_TEXTURE_2D, ARBTextureSwizzle.GL_TEXTURE_SWIZZLE_RGBA,
                new int[]{GL30C.GL_RED, GL30C.GL_RED, GL30C.GL_RED, GL30C.GL_ONE});

        configureSampler(glTextureId, settings);
    }

    private void configureSampler(int glTextureId, PackShadowDirectives.SamplingSettings settings) {
        if (settings.getMipmap()) {
            int filteringMode = settings.getNearest() ? GL20C.GL_NEAREST_MIPMAP_NEAREST : GL20C.GL_LINEAR_MIPMAP_LINEAR;
            mipmapPasses.add(new MipmapPass(glTextureId, filteringMode));
        }

        if (!settings.getNearest()) {
            // Make sure that things are smoothed
            AurumRenderSystem.texParameteri(glTextureId, GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MIN_FILTER, GL20C.GL_LINEAR);
            AurumRenderSystem.texParameteri(glTextureId, GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MAG_FILTER, GL20C.GL_LINEAR);
        } else {
            AurumRenderSystem.texParameteri(glTextureId, GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MIN_FILTER, GL20C.GL_NEAREST);
            AurumRenderSystem.texParameteri(glTextureId, GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MAG_FILTER, GL20C.GL_NEAREST);
        }
    }

    void generateMipmaps() {
        GlStateManager.activeTexture(GL20C.GL_TEXTURE4);

        for (MipmapPass mipmapPass : mipmapPasses) {
            setupMipmappingForTexture(mipmapPass.texture(), mipmapPass.targetFilteringMode());
        }

        GlStateManager.activeTexture(GL20C.GL_TEXTURE0);
    }

    private void setupMipmappingForTexture(int texture, int filteringMode) {
        AurumRenderSystem.generateMipmaps(texture, GL20C.GL_TEXTURE_2D);
        AurumRenderSystem.texParameteri(texture, GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MIN_FILTER, filteringMode);
    }

    private record MipmapPass(int texture, int targetFilteringMode) {
    }
}
