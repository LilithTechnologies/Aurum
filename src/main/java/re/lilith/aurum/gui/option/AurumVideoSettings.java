package re.lilith.aurum.gui.option;

import re.lilith.aurum.Aurum;

public class AurumVideoSettings {
    public static int shadowDistance = 32;

    public static int getOverriddenShadowDistance(int base) {
        return Aurum.getPipelineManager().getPipeline()
                .map(pipeline -> pipeline.getForcedShadowRenderDistanceChunksForDisplay().orElse(base))
                .orElse(base);
    }

    public static boolean isShadowDistanceSliderEnabled() {
        return Aurum.getPipelineManager().getPipeline()
                .map(pipeline -> pipeline.getForcedShadowRenderDistanceChunksForDisplay().isEmpty())
                .orElse(true);
    }
}
