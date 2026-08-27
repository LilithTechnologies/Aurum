package re.lilith.aurum.texture;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.resource.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import re.lilith.aurum.texture.util.NativeImage;

public class DynamicTexture
        extends AbstractTexture {
    private static final Logger LOGGER = LogManager.getLogger();
    @Nullable
    private NativeImage pixels;

    public DynamicTexture(@Nullable NativeImage nativeImage) {
        this.pixels = nativeImage;
        TextureUtil.prepareImage(this.getGlId(), this.pixels.getWidth(), this.pixels.getHeight());
        this.upload();
    }

    @Override
    public void load(ResourceManager resourceManager) {
    }

    public void upload() {
        if (this.pixels != null) {
            this.bind();
            this.pixels.upload(0, 0, 0, false);
        } else {
            LOGGER.warn("Trying to upload disposed texture {}", this.getGlId());
        }
    }

    @Nullable
    public NativeImage getPixels() {
        return this.pixels;
    }

    public void bind() {
        GlStateManager.bindTexture(getGlId());
    }

    public void clearGlId() {
        if (this.pixels != null) {
            this.pixels.close();
            super.clearGlId();
            this.pixels = null;
        }
    }
}

