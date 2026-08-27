package re.lilith.aurum.targets.texture;

import re.lilith.aurum.texture.DynamicTexture;
import re.lilith.aurum.texture.util.NativeImage;

public class NativeImageBackedSingleColorTexture extends DynamicTexture {
    public NativeImageBackedSingleColorTexture(int red, int green, int blue, int alpha) {
        super(create(NativeImage.combine(alpha, blue, green, red)));
    }

    public NativeImageBackedSingleColorTexture(int rgba) {
        this(rgba >> 24 & 0xFF, rgba >> 16 & 0xFF, rgba >> 8 & 0xFF, rgba & 0xFF);
    }

    private static NativeImage create(int color) {
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, 1, 1, false);
        image.setPixelRGBA(0, 0, color);
        return image;
    }
}
