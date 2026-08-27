package re.lilith.aurum.pipeline.pathways.shadows;

import net.minecraft.client.MinecraftClient;
import org.joml.Vector4f;
import re.lilith.aurum.gui.option.AurumVideoSettings;
import re.lilith.aurum.pipeline.pathways.shadows.frustum.BoxCuller;
import re.lilith.aurum.pipeline.pathways.shadows.frustum.CullEverythingFrustum;
import re.lilith.aurum.pipeline.pathways.shadows.frustum.FrustumHolder;
import re.lilith.aurum.pipeline.pathways.shadows.frustum.advanced.AdvancedShadowCullingFrustum;
import re.lilith.aurum.pipeline.pathways.shadows.frustum.advanced.SafeZoneCullingFrustum;
import re.lilith.aurum.pipeline.pathways.shadows.frustum.fallback.BoxCullingFrustum;
import re.lilith.aurum.pipeline.pathways.shadows.frustum.fallback.NonCullingFrustum;
import re.lilith.aurum.pipeline.state.CapturedRenderingState;
import re.lilith.aurum.shaderpack.ShadowCullState;
import re.lilith.aurum.uniforms.CelestialUniforms;

/**
 * Decides which shadow culling frustum (none, distance-only, advanced, or safe zone) applies this frame,
 * based on the shader pack's requested {@link ShadowCullState} and whether it appears to need full scene
 * data (voxelization).
 */
class ShadowFrustumFactory {
    private final ShadowCullState packCullingState;
    private final float halfPlaneLength;
    private final float voxelDistance;
    private final float sunPathRotation;
    private boolean packHasVoxelization;

    ShadowFrustumFactory(ShadowCullState packCullingState, boolean packHasVoxelization, float halfPlaneLength,
                         float voxelDistance, float sunPathRotation) {
        this.packCullingState = packCullingState;
        this.packHasVoxelization = packHasVoxelization;
        this.halfPlaneLength = halfPlaneLength;
        this.voxelDistance = voxelDistance;
        this.sunPathRotation = sunPathRotation;
    }

    void setUsesImages(boolean usesImages) {
        this.packHasVoxelization = packHasVoxelization || usesImages;
    }

    FrustumHolder create(float renderMultiplier, FrustumHolder holder) {
        // TODO: Cull entities / block entities with Advanced Frustum Culling even if voxelization is detected.
        String distanceInfo;
        String cullingInfo;
        if ((packCullingState == ShadowCullState.DISTANCE || packHasVoxelization)
                && packCullingState != ShadowCullState.ADVANCED && packCullingState != ShadowCullState.SAFE_ZONE) {
            double distance = halfPlaneLength * renderMultiplier;

            String reason;

            if (packCullingState == ShadowCullState.DISTANCE) {
                reason = "(set by shader pack)";
            } else /*if (packHasVoxelization)*/ {
                reason = "(voxelization detected)";
            }

            if (distance <= 0 || distance > MinecraftClient.getInstance().options.viewDistance * 16) {
                distanceInfo = MinecraftClient.getInstance().options.viewDistance * 16
                        + " blocks (capped by normal render distance)";
                cullingInfo = "disabled " + reason;
                return holder.setInfo(new NonCullingFrustum(), distanceInfo, cullingInfo);
            } else {
                distanceInfo = distance + " blocks (set by shader pack)";
                cullingInfo = "distance only " + reason;
                BoxCuller boxCuller = new BoxCuller(distance);
                holder.setInfo(new BoxCullingFrustum(boxCuller), distanceInfo, cullingInfo);
            }
        } else {
            BoxCuller boxCuller;

            boolean hasSafeZone = packCullingState == ShadowCullState.SAFE_ZONE;

            if (hasSafeZone && renderMultiplier < 0) {
                renderMultiplier = 1.0f;
            }

            double distance = (hasSafeZone ? voxelDistance : halfPlaneLength) * renderMultiplier;
            String setter = "(set by shader pack)";

            if (renderMultiplier < 0) {
                distance = AurumVideoSettings.shadowDistance * 16;
                setter = "(set by user)";
            }

            if (distance >= MinecraftClient.getInstance().options.viewDistance * 16 && !hasSafeZone) {
                distanceInfo = MinecraftClient.getInstance().options.viewDistance * 16
                        + " blocks (capped by normal render distance)";
                boxCuller = null;
            } else {
                distanceInfo = distance + " blocks " + setter;

                if (distance == 0.0 && !hasSafeZone) {
                    cullingInfo = "no shadows rendered";
                    return holder.setInfo(new CullEverythingFrustum(), distanceInfo, cullingInfo);
                }

                boxCuller = new BoxCuller(distance);
            }

            cullingInfo = (hasSafeZone ? "Safe Zone" : "Advanced") + " Frustum Culling enabled";

            Vector4f shadowLightPosition = new CelestialUniforms(sunPathRotation).getShadowLightPositionInWorldSpace();

            org.joml.Vector3f shadowLightVectorFromOrigin =
                    new org.joml.Vector3f(shadowLightPosition.x(), shadowLightPosition.y(), shadowLightPosition.z());

            shadowLightVectorFromOrigin.normalize();

            // Carry the previous frame's back/front plane classification forward so the new frustum can apply
            // hysteresis instead of flickering every time a plane's classification crosses zero.
            boolean[] previousBackPlaneClassification = null;
            if (holder.getFrustum() instanceof AdvancedShadowCullingFrustum previousFrustum) {
                previousBackPlaneClassification = previousFrustum.getBackPlaneClassification();
            }

            if (hasSafeZone) {
                BoxCuller distanceCuller = new BoxCuller(halfPlaneLength * renderMultiplier);
                return holder.setInfo(new SafeZoneCullingFrustum(CapturedRenderingState.INSTANCE.getGbufferModelView(),
                                CapturedRenderingState.INSTANCE.getGbufferProjection(), shadowLightVectorFromOrigin, boxCuller, distanceCuller,
                                previousBackPlaneClassification),
                        distanceInfo, cullingInfo);
            }

            return holder.setInfo(new AdvancedShadowCullingFrustum(CapturedRenderingState.INSTANCE.getGbufferModelView(),
                    CapturedRenderingState.INSTANCE.getGbufferProjection(), shadowLightVectorFromOrigin, boxCuller,
                    previousBackPlaneClassification), distanceInfo, cullingInfo);

        }

        return holder;
    }
}
