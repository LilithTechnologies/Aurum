package re.lilith.aurum.pipeline.pathways.shadows.frustum.advanced;

import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import re.lilith.aurum.pipeline.pathways.shadows.frustum.BoxCuller;

public class SafeZoneCullingFrustum extends AdvancedShadowCullingFrustum {
    private final BoxCuller distanceCuller;

    public SafeZoneCullingFrustum(Matrix4f playerView, Matrix4f playerProjection, Vector3f shadowLightVector, BoxCuller voxelCuller, BoxCuller distanceCuller) {
        this(playerView, playerProjection, shadowLightVector, voxelCuller, distanceCuller, null);
    }

    public SafeZoneCullingFrustum(Matrix4f playerView, Matrix4f playerProjection, Vector3f shadowLightVector, BoxCuller voxelCuller,
                                  BoxCuller distanceCuller, boolean @Nullable [] previousBackPlaneClassification) {
        super(playerView, playerProjection, shadowLightVector, voxelCuller, previousBackPlaneClassification);
        this.distanceCuller = distanceCuller;
    }

    @Override
    public void start() {
        super.start();

        if (this.distanceCuller != null) {
            var cameraPos = MinecraftClient.getInstance().getCameraEntity().getPos();
            this.distanceCuller.setPosition(cameraPos.x, cameraPos.y, cameraPos.z);
        }
    }

    @Override
    public boolean isInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        // Cull if outside the overall distance limit
        if (distanceCuller != null && distanceCuller.isCulled((float) minX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) maxZ)) {
            return false;
        }

        // If within the voxel safe zone, always render
        if (boxCuller != null && !boxCuller.isCulled((float) minX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) maxZ)) {
            return true;
        }

        // Otherwise fall through to advanced frustum culling
        return isVisible(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
