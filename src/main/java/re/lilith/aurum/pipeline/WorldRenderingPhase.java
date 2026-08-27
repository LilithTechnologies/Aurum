package re.lilith.aurum.pipeline;

import net.minecraft.client.render.RenderLayer;

public enum WorldRenderingPhase {
    NONE,
    SKY,
    SUNSET,
    CUSTOM_SKY, // Unused, just here to match OptiFine ordinals
    SUN,
    MOON,
    STARS,
    VOID,
    TERRAIN_SOLID,
    TERRAIN_CUTOUT_MIPPED,
    TERRAIN_CUTOUT,
    ENTITIES,
    BLOCK_ENTITIES,
    DESTROY,
    OUTLINE,
    DEBUG,
    HAND_SOLID,
    TERRAIN_TRANSLUCENT,
    TRIPWIRE,
    PARTICLES,
    CLOUDS,
    RAIN_SNOW,
    WORLD_BORDER,
    HAND_TRANSLUCENT;

    public static WorldRenderingPhase fromTerrainRenderType(RenderLayer renderType) {
        if (renderType == RenderLayer.SOLID) {
            return WorldRenderingPhase.TERRAIN_SOLID;
        } else if (renderType == RenderLayer.CUTOUT) {
            return WorldRenderingPhase.TERRAIN_CUTOUT;
        } else if (renderType == RenderLayer.CUTOUT_MIPPED) {
            return WorldRenderingPhase.TERRAIN_CUTOUT_MIPPED;
        } else if (renderType == RenderLayer.TRANSLUCENT) {
            return WorldRenderingPhase.TERRAIN_TRANSLUCENT;
        } else {
            throw new IllegalStateException("Illegal render type!");
        }
    }
}
