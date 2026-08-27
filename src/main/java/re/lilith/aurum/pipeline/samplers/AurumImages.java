package re.lilith.aurum.pipeline.samplers;

import com.google.common.collect.ImmutableSet;
import re.lilith.aurum.gl.image.GlImage;
import re.lilith.aurum.gl.image.ImageHolder;
import re.lilith.aurum.gl.texture.InternalTextureFormat;
import re.lilith.aurum.pipeline.pathways.shadows.ShadowRenderTargets;
import re.lilith.aurum.targets.render.RenderTarget;
import re.lilith.aurum.targets.render.RenderTargets;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class AurumImages {
    public static void addRenderTargetImages(ImageHolder images, Supplier<ImmutableSet<Integer>> flipped,
                                             RenderTargets renderTargets) {
        for (int i = 0; i < renderTargets.getRenderTargetCount(); i++) {
            final int index = i;

            // Note: image bindings *are* impacted by buffer flips.
            IntSupplier textureID = () -> {
                ImmutableSet<Integer> flippedBuffers = flipped.get();
                RenderTarget target = renderTargets.get(index);

                if (flippedBuffers.contains(index)) {
                    return target.getAltTexture();
                } else {
                    return target.getMainTexture();
                }
            };

            final InternalTextureFormat internalFormat = renderTargets.get(i).getInternalFormat();
            final String name = "colorimg" + i;

            images.addTextureImage(textureID, internalFormat, name);
        }
    }

    public static void addCustomImages(ImageHolder images, GlImage[] customImages) {
        for (GlImage image : customImages) {
            images.addTextureImage(image::getId, image.getInternalFormat(), image.getName());
        }
    }

    public static boolean hasShadowImages(ImageHolder images) {
        // TODO: Generalize
        return images.hasImage("shadowcolorimg0") || images.hasImage("shadowcolorimg1");
    }

    public static void addShadowColorImages(ImageHolder images, ShadowRenderTargets shadowRenderTargets) {
        for (int i = 0; i < shadowRenderTargets.getNumColorTextures(); i++) {
            final int index = i;

            IntSupplier textureID = () -> shadowRenderTargets.getColorTextureId(index);
            InternalTextureFormat format = shadowRenderTargets.getColorTextureFormat(index);

            images.addTextureImage(textureID, format, "shadowcolorimg" + i);
        }
    }
}
