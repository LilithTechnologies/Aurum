package re.lilith.aurum.celeritas;

import dev.rdh.argentum.impl.Argentum;
import dev.rdh.argentum.impl.config.ArgentumConfig;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.pipeline.WorldRenderingPipeline;

// todo(aurum): implement support for argentum's fast paths
public final class ArgentumFastPaths {
    private static boolean overridden;
    private static boolean userEntityInstancing;
    private static boolean userFontBatching;
    private static boolean userFasterClouds;

    private ArgentumFastPaths() {
    }

    public static void update() {
        if (isShaderPackLoaded()) {
            apply();
        } else {
            restore();
        }
    }

    private static boolean isShaderPackLoaded() {
        WorldRenderingPipeline pipeline = Aurum.getPipelineManager().getPipelineNullable();

        return pipeline != null && pipeline.getCeleritasTerrainPipeline() != null;
    }

    private static void apply() {
        ArgentumConfig config = Argentum.CONFIG;

        if (!overridden) {
            userEntityInstancing = config.entityInstancing;
            userFontBatching = config.fontBatching;
            userFasterClouds = config.fasterClouds;
            overridden = true;
        }

        config.entityInstancing = false;
        config.fontBatching = false;
        config.fasterClouds = false;
    }

    private static void restore() {
        if (!overridden) {
            return;
        }

        ArgentumConfig config = Argentum.CONFIG;
        config.entityInstancing = userEntityInstancing;
        config.fontBatching = userFontBatching;
        config.fasterClouds = userFasterClouds;
        overridden = false;
    }
}
