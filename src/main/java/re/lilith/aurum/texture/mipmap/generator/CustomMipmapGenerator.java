package re.lilith.aurum.texture.mipmap.generator;

import org.jetbrains.annotations.Nullable;
import re.lilith.aurum.texture.pbr.loader.SpriteInfo;
import re.lilith.aurum.texture.util.NativeImage;

public interface CustomMipmapGenerator {
    NativeImage[] generateMipLevels(NativeImage image, int mipLevel);

    interface Provider {
        @Nullable
        CustomMipmapGenerator getMipmapGenerator(SpriteInfo info, int atlasWidth, int atlasHeight);
    }
}
