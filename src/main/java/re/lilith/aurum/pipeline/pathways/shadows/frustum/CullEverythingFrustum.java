package re.lilith.aurum.pipeline.pathways.shadows.frustum;

import net.minecraft.client.render.Frustum;

public class CullEverythingFrustum extends Frustum {
    public CullEverythingFrustum() {
    }

    @Override
    public boolean isInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return false;
    }
}
