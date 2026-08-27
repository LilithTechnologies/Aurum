package re.lilith.aurum.pipeline.pathways.shadows.frustum.fallback;

import net.minecraft.client.render.Frustum;

public class NonCullingFrustum extends Frustum {
    public NonCullingFrustum() {
    }

    @Override
    public boolean isInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return true;
    }
}
