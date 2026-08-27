package re.lilith.aurum.gl.image;

import re.lilith.aurum.gl.AurumRenderSystem;

public class ImageLimits {
    private final int maxImageUnits;
    private static ImageLimits instance;

    private ImageLimits() {
        this.maxImageUnits = AurumRenderSystem.getMaxImageUnits();
    }

    public int getMaxImageUnits() {
        return maxImageUnits;
    }

    public static ImageLimits get() {
        if (instance == null) {
            instance = new ImageLimits();
        }

        return instance;
    }
}
