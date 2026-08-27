package re.lilith.aurum.targets.texture;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.shaderpack.texture.CustomTextureData;
import re.lilith.aurum.texture.DynamicTexture;
import re.lilith.aurum.texture.util.NativeImage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class NativeImageBackedCustomTexture extends DynamicTexture {
    public NativeImageBackedCustomTexture(CustomTextureData.PngData textureData) throws IOException {
        super(create(textureData.getContent()));

        // By default, images are unblurred and not clamped.

        if (textureData.getFilteringData().shouldBlur()) {
            AurumRenderSystem.texParameteri(getGlId(), GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_LINEAR);
            AurumRenderSystem.texParameteri(getGlId(), GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_LINEAR);
        }

        if (textureData.getFilteringData().shouldClamp()) {
            AurumRenderSystem.texParameteri(getGlId(), GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, GL13C.GL_CLAMP_TO_EDGE);
            AurumRenderSystem.texParameteri(getGlId(), GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, GL13C.GL_CLAMP_TO_EDGE);
        }
    }

    private static NativeImage create(byte[] content) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(content.length);
        buffer.put(content);
        buffer.flip();

        return NativeImage.read(buffer);
    }

    @Override
    public void upload() {
        NativeImage image = Objects.requireNonNull(getPixels());

        bind();
        image.upload(0, 0, 0, 0, 0, image.getWidth(), image.getHeight(), false, false, false, false);
    }
}
