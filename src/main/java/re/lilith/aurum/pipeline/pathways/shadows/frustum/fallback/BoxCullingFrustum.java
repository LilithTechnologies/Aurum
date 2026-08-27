package re.lilith.aurum.pipeline.pathways.shadows.frustum.fallback;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import re.lilith.aurum.pipeline.pathways.shadows.frustum.BoxCuller;

public class BoxCullingFrustum extends Frustum {
    private final BoxCuller boxCuller;

    public BoxCullingFrustum(BoxCuller boxCuller) {
        this.boxCuller = boxCuller;
    }

    @Override
    public void start() {
        double cameraX = MinecraftClient.getInstance().getCameraEntity().getPos().x;
        double cameraY = MinecraftClient.getInstance().getCameraEntity().getPos().y;
        double cameraZ = MinecraftClient.getInstance().getCameraEntity().getPos().z;
        boxCuller.setPosition(cameraX, cameraY, cameraZ);
    }

    @Override
    public boolean isInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return !boxCuller.isCulled((float) minX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) maxZ);
    }
}
