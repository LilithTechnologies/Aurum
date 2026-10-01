package re.lilith.aurum.celeritas;

import dev.rdh.argentum.impl.Argentum;
import dev.rdh.argentum.impl.config.ArgentumConfig;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.pipeline.WorldRenderingPipeline;

// todo(aurum): implement support for argentum's fast paths
public final class ArgentumFastPaths {
    private static boolean[] saved;

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

        if (saved == null) {
            saved = new boolean[] {
                    config.entityInstancing,
                    config.fontBatching,
                    config.fasterClouds,
                    config.fasterWeather,
                    config.bakeBlockEntities,
                    config.nameTagBatching,
                    config.guiItemAtlas,
            };
        }

        config.entityInstancing = false;
        config.fontBatching = false;
        config.fasterClouds = false;
        config.fasterWeather = false;
        config.bakeBlockEntities = false;
        config.nameTagBatching = false;
        config.guiItemAtlas = false;
    }

    private static void restore() {
        if (saved == null) {
            return;
        }

        ArgentumConfig config = Argentum.CONFIG;
        config.entityInstancing = saved[0];
        config.fontBatching = saved[1];
        config.fasterClouds = saved[2];
        config.fasterWeather = saved[3];
        config.bakeBlockEntities = saved[4];
        config.nameTagBatching = saved[5];
        config.guiItemAtlas = saved[6];
        saved = null;
    }
}
