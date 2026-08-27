package re.lilith.aurum.uniforms;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningBoltEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Math;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;
import re.lilith.aurum.gbuffer.BlockRenderingSettings;
import re.lilith.aurum.gl.uniform.UniformUpdateFrequency;
import re.lilith.aurum.gl.uniform.holder.UniformHolder;
import re.lilith.aurum.pipeline.state.CapturedRenderingState;
import re.lilith.aurum.uniforms.utility.EntityIdHelper;

import java.util.Objects;

public class AurumExclusiveUniforms {
    private static Vector3d forwardVector(float yawDeg, float pitchDeg) {
        double yr = -yawDeg * 0.017453292 - Math.PI;
        double pr = -pitchDeg * 0.017453292;
        double cy = Math.cos(yr);
        double sy = Math.sin(yr);
        double cp = -Math.cos(pr);
        double sp = Math.sin(pr);
        return new Vector3d(sy * cp, sp, cy * cp);
    }

    public static void addAurumExclusiveUniforms(UniformHolder uniforms) {
        WorldInfoUniforms.addWorldInfoUniforms(uniforms);

        uniforms.uniform1b(UniformUpdateFrequency.PER_TICK, "isRightHanded", () -> true);
        uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "endFlashIntensity", () -> 0.0f);
        uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "previousEndFlashIntensity", () -> 0.0f);
        uniforms.uniform1b(UniformUpdateFrequency.PER_TICK, "feetInWater", AurumExclusiveUniforms::isFeetInWater);
        uniforms.uniform1b(UniformUpdateFrequency.PER_TICK, "isRiding", AurumExclusiveUniforms::isRiding);
        uniforms.uniform1b(UniformUpdateFrequency.PER_TICK, "vehicleInWater", AurumExclusiveUniforms::isVehicleInWater);
        uniforms.uniform1i(UniformUpdateFrequency.PER_TICK, "vehicleId", AurumExclusiveUniforms::getVehicleId);
        uniforms.uniform3d(UniformUpdateFrequency.PER_FRAME, "vehicleLookVector", AurumExclusiveUniforms::getVehicleLookVector);
        uniforms.uniform3d(UniformUpdateFrequency.PER_FRAME, "relativeVehiclePosition", AurumExclusiveUniforms::getRelativeVehiclePosition);
        uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "currentPlayerArmor", AurumExclusiveUniforms::getCurrentArmor);
        uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "maxPlayerArmor", () -> 20);
        uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "seaLevel", () -> 63);
        uniforms.uniform3d(UniformUpdateFrequency.PER_FRAME, "playerLookVector", AurumExclusiveUniforms::getPlayerLookVector);
        uniforms.uniform3d(UniformUpdateFrequency.PER_FRAME, "playerBodyVector", AurumExclusiveUniforms::getPlayerBodyVector);
        uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "currentSelectedBlockId", AurumExclusiveUniforms::getCurrentSelectedBlockId);
        uniforms.uniform3f(UniformUpdateFrequency.PER_FRAME, "currentSelectedBlockPos", AurumExclusiveUniforms::getCurrentSelectedBlockPos);

        // All Aurum-exclusive uniforms (uniforms which do not exist in either OptiFine or ShadersMod) should be registered here.
        uniforms.uniform1f(UniformUpdateFrequency.PER_FRAME, "thunderStrength", AurumExclusiveUniforms::getThunderStrength);
        uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "currentPlayerHealth", AurumExclusiveUniforms::getCurrentHealth);
        uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "maxPlayerHealth", AurumExclusiveUniforms::getMaxHealth);
        uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "currentPlayerHunger", AurumExclusiveUniforms::getCurrentHunger);
        uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "maxPlayerHunger", () -> 20);
        uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "currentPlayerAir", AurumExclusiveUniforms::getCurrentAir);
        uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "maxPlayerAir", AurumExclusiveUniforms::getMaxAir);
        uniforms.uniform1b(UniformUpdateFrequency.PER_FRAME, "firstPersonCamera", AurumExclusiveUniforms::isFirstPersonCamera);
        uniforms.uniform1b(UniformUpdateFrequency.PER_TICK, "isSpectator", AurumExclusiveUniforms::isSpectator);
        uniforms.uniform3d(UniformUpdateFrequency.PER_FRAME, "eyePosition", AurumExclusiveUniforms::getEyePosition);
        uniforms.uniform3d(UniformUpdateFrequency.PER_FRAME, "relativeEyePosition",
                () -> CameraUniforms.getUnshiftedCameraPosition().sub(getEyePosition()));
        Vector4f zero = new Vector4f(0, 0, 0, 0);
        uniforms.uniform4f(UniformUpdateFrequency.PER_TICK, "lightningBoltPosition", () -> {
            if (MinecraftClient.getInstance().world != null) {
                return MinecraftClient.getInstance().world.getLoadedEntities().stream().filter(bolt -> bolt instanceof LightningBoltEntity).findAny().map(bolt -> {
                    Vector3d unshiftedCameraPosition = CameraUniforms.getUnshiftedCameraPosition();
                    Vec3d vec3 = bolt.getPos();
                    return new Vector4f((float) (vec3.x - unshiftedCameraPosition.x), (float) (vec3.y - unshiftedCameraPosition.y), (float) (vec3.z - unshiftedCameraPosition.z), 1);
                }).orElse(zero);
            } else {
                return zero;
            }
        });
    }

    private static boolean isFeetInWater() {
        PlayerEntity player = MinecraftClient.getInstance().player;
        return player != null && player.isTouchingWater();
    }

    private static boolean isRiding() {
        PlayerEntity player = MinecraftClient.getInstance().player;
        return player != null && player.vehicle != null;
    }

    private static boolean isVehicleInWater() {
        PlayerEntity player = MinecraftClient.getInstance().player;
        return player != null && player.vehicle != null && player.vehicle.isTouchingWater();
    }

    private static int getVehicleId() {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || player.vehicle == null) {
            return 0;
        }
        return Math.max(EntityIdHelper.getEntityId(player.vehicle), 0);
    }

    private static Vector3d getVehicleLookVector() {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || player.vehicle == null) {
            return new Vector3d();
        }
        Entity vehicle = player.vehicle;
        return forwardVector(vehicle.yaw, vehicle.pitch);
    }

    private static Vector3d getRelativeVehiclePosition() {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || player.vehicle == null) {
            return new Vector3d();
        }
        Entity vehicle = player.vehicle;
        float delta = CapturedRenderingState.INSTANCE.getTickDelta();
        double vx = vehicle.prevX + (vehicle.x - vehicle.prevX) * delta;
        double vy = vehicle.prevY + (vehicle.y - vehicle.prevY) * delta;
        double vz = vehicle.prevZ + (vehicle.z - vehicle.prevZ) * delta;
        Vector3d cam = CameraUniforms.getUnshiftedCameraPosition();
        return new Vector3d(vx - cam.x, vy - cam.y, vz - cam.z);
    }

    private static float getCurrentArmor() {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return -1;
        }
        return player.getArmorProtectionValue() / 20.0f;
    }

    private static Vector3d getPlayerLookVector() {
        LivingEntity entity = (LivingEntity) MinecraftClient.getInstance().getCameraEntity();
        if (entity == null) {
            return new Vector3d();
        }
        float delta = CapturedRenderingState.INSTANCE.getTickDelta();
        float yaw = entity.prevYaw + (entity.yaw - entity.prevYaw) * delta;
        float pitch = entity.prevPitch + (entity.pitch - entity.prevPitch) * delta;
        return forwardVector(yaw, pitch);
    }

    private static Vector3d getPlayerBodyVector() {
        LivingEntity entity = (LivingEntity) MinecraftClient.getInstance().getCameraEntity();
        if (entity == null) {
            return new Vector3d();
        }
        return forwardVector(entity.bodyYaw, 0.0f);
    }

    private static int getCurrentSelectedBlockId() {
        MinecraftClient client = MinecraftClient.getInstance();
        BlockHitResult hit = client.result;

        if (client.world == null || hit == null || hit.type != BlockHitResult.Type.BLOCK) {
            return 0;
        }

        Object2IntMap<BlockState> blockStateIds = BlockRenderingSettings.INSTANCE.getBlockStateIds();

        if (blockStateIds == null) {
            return 0;
        }

        BlockPos pos = hit.getBlockPos();
        BlockState state = client.world.getBlockState(pos);

        return Math.max(blockStateIds.getOrDefault(state, 0), 0);
    }

    private static Vector3f getCurrentSelectedBlockPos() {
        MinecraftClient client = MinecraftClient.getInstance();
        BlockHitResult hit = client.result;

        if (client.world == null || hit == null || hit.type != BlockHitResult.Type.BLOCK) {
            return new Vector3f(-256.0f, -256.0f, -256.0f);
        }

        BlockPos pos = hit.getBlockPos();
        Vector3d cam = CameraUniforms.getUnshiftedCameraPosition();

        return new Vector3f(
                (float) ((pos.getX() + 0.5) - cam.x),
                (float) ((pos.getY() + 0.5) - cam.y),
                (float) ((pos.getZ() + 0.5) - cam.z));
    }

    private static float getThunderStrength() {
        // Note: Ensure this is in the range of 0 to 1 - some custom servers send out of range values.
        return Math.clamp(0.0F, 1.0F,
                MinecraftClient.getInstance().world.getThunderGradient(CapturedRenderingState.INSTANCE.getTickDelta()));
    }

    private static float getCurrentHealth() {
        if (MinecraftClient.getInstance().player == null) {
            return -1;
        }

        return MinecraftClient.getInstance().player.getHealth() / MinecraftClient.getInstance().player.getMaxHealth();
    }

    private static float getCurrentHunger() {
        if (MinecraftClient.getInstance().player == null) {
            return -1;
        }

        return MinecraftClient.getInstance().player.getAbsorption() / 20f;
    }

    private static float getCurrentAir() {
        if (MinecraftClient.getInstance().player == null) {
            return -1;
        }

        return (float) MinecraftClient.getInstance().player.getAir();
    }

    private static float getMaxAir() {
        if (MinecraftClient.getInstance().player == null) {
            return -1;
        }

        return (float) MinecraftClient.getInstance().player.getAir();
    }

    private static float getMaxHealth() {
        if (MinecraftClient.getInstance().player == null) {
            return -1;
        }

        return MinecraftClient.getInstance().player.getMaxHealth();
    }

    private static boolean isFirstPersonCamera() {
        // If camera type is not explicitly third-person, assume it's first-person.
        return switch (MinecraftClient.getInstance().options.perspective) {
            case 2, 3 -> false;
            default -> true;
        };
    }

    private static boolean isSpectator() {
        return MinecraftClient.getInstance().player.isSpectator();
    }

    private static Vector3d getEyePosition() {
        Entity camera = Objects.requireNonNull(MinecraftClient.getInstance().getCameraEntity());
        Vector3d position = CameraUniforms.getUnshiftedCameraPosition();

        return new Vector3d(position.x, position.y + camera.getEyeHeight(), position.z);
    }

    public static class WorldInfoUniforms {
        public static void addWorldInfoUniforms(UniformHolder uniforms) {
            ClientWorld level = MinecraftClient.getInstance().world;
            uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "bedrockLevel", () -> 0);
            uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "heightLimit", () -> {
                if (level != null) {
                    return level.getMaxBuildHeight();
                } else {
                    return 256;
                }
            });
            uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "logicalHeightLimit", () -> {
                if (level != null) {
                    return level.getMaxBuildHeight();
                } else {
                    return 256;
                }
            });
            uniforms.uniform1f(UniformUpdateFrequency.PER_FRAME, "cloudHeight", () -> 128.0f);
            uniforms.uniform1b(UniformUpdateFrequency.PER_FRAME, "hasCeiling", () -> {
                if (level != null) {
                    return level.dimension.hasNoSkylight();
                } else {
                    return false;
                }
            });
            uniforms.uniform1b(UniformUpdateFrequency.PER_FRAME, "hasSkylight", () -> {
                if (level != null) {
                    return !level.dimension.hasNoSkylight();
                } else {
                    return true;
                }
            });
            uniforms.uniform1f(UniformUpdateFrequency.PER_FRAME, "ambientLight", () -> {
                if (level != null) {
                    return level.dimension.getLightLevelToBrightness()[0];
                } else {
                    return 0f;
                }
            });

        }
    }
}
