package re.lilith.aurum.uniforms;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.block.material.Material;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import org.joml.*;
import org.joml.Math;
import re.lilith.aurum.gbuffer.GbufferPrograms;
import re.lilith.aurum.gl.state.StateUpdateNotifiers;
import re.lilith.aurum.gl.uniform.holder.DynamicUniformHolder;
import re.lilith.aurum.gl.uniform.holder.UniformHolder;
import re.lilith.aurum.mixin.access.GlStateManagerAccessor;
import re.lilith.aurum.mixin.state.BooleanStateAccessor;
import re.lilith.aurum.pipeline.state.CapturedRenderingState;
import re.lilith.aurum.shaderpack.IdMap;
import re.lilith.aurum.shaderpack.PackDirectives;
import re.lilith.aurum.texture.TextureInfoCache;
import re.lilith.aurum.texture.TextureInfoCache.TextureInfo;
import re.lilith.aurum.texture.TextureTracker;
import re.lilith.aurum.uniforms.transforms.SmoothedFloat;
import re.lilith.aurum.uniforms.transforms.SmoothedVec2f;
import re.lilith.aurum.uniforms.utility.FrameUpdateNotifier;

import java.util.Objects;
import java.util.function.Predicate;

import static re.lilith.aurum.gl.uniform.UniformUpdateFrequency.*;

public final class CommonUniforms {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final Vector2i ZERO_VECTOR_2i = new Vector2i();
    private static final Vector4i ZERO_VECTOR_4i = new Vector4i(0, 0, 0, 0);
    private static final Vector3d ZERO_VECTOR_3d = new Vector3d();

    private CommonUniforms() {
        // no construction allowed
    }

    // Needs to use a LocationalUniformHolder as we need it for the common uniforms
    public static void addCommonUniforms(DynamicUniformHolder uniforms, IdMap idMap, PackDirectives directives, FrameUpdateNotifier updateNotifier) {
        CameraUniforms.addCameraUniforms(uniforms, updateNotifier);
        ViewportUniforms.addViewportUniforms(uniforms);
        WorldTimeUniforms.addWorldTimeUniforms(uniforms);
        SystemTimeUniforms.addSystemTimeUniforms(uniforms);
        new CelestialUniforms(directives.getSunPathRotation()).addCelestialUniforms(uniforms);
        IdMapUniforms.addIdMapUniforms(updateNotifier, uniforms, idMap);
        AurumExclusiveUniforms.addAurumExclusiveUniforms(uniforms);
        MatrixUniforms.addMatrixUniforms(uniforms, directives);
        HardcodedCustomUniforms.addHardcodedCustomUniforms(uniforms, updateNotifier);
        FogUniforms.addFogUniforms(uniforms);
        AurumInternalUniforms.addFogUniforms(uniforms);

        uniforms.uniform2i("atlasSize", () -> {
            int glId = GlStateManagerAccessor.getTEXTURES()[0].boundTexture;

            AbstractTexture texture = TextureTracker.INSTANCE.getTexture(glId);
            if (texture instanceof SpriteAtlasTexture) {
                TextureInfo info = TextureInfoCache.INSTANCE.getInfo(glId);
                return new Vector2i(info.getWidth(), info.getHeight());
            }

            return ZERO_VECTOR_2i;
        }, StateUpdateNotifiers.bindTextureNotifier);

        uniforms.uniform2i("gtextureSize", () -> {
            int glId = GlStateManagerAccessor.getTEXTURES()[0].boundTexture;

            TextureInfo info = TextureInfoCache.INSTANCE.getInfo(glId);
            return new Vector2i(info.getWidth(), info.getHeight());

        }, StateUpdateNotifiers.bindTextureNotifier);

        uniforms.uniform4i("blendFunc", () -> {
            GlStateManager.BlendFuncState blend = GlStateManagerAccessor.getBLEND();

            if (((BooleanStateAccessor) blend.capState).isEnabled()) {
                return new Vector4i(blend.srcFactorRGB, blend.dstFactorRGB, blend.srcFactorAlpha, blend.dstFactorAlpha);
            } else {
                return ZERO_VECTOR_4i;
            }
        }, StateUpdateNotifiers.blendFuncNotifier);

        uniforms.uniform1i("renderStage", () -> GbufferPrograms.getCurrentPhase().ordinal(), StateUpdateNotifiers.phaseChangeNotifier);

        CommonUniforms.generalCommonUniforms(uniforms, updateNotifier, directives);
    }

    public static void addNonDynamicUniforms(UniformHolder uniforms, PackDirectives directives, FrameUpdateNotifier updateNotifier,
                                             Predicate<String> declaredByPack) {
        CameraUniforms.addCameraUniforms(uniforms, updateNotifier);
        ViewportUniforms.addViewportUniforms(uniforms);
        WorldTimeUniforms.addWorldTimeUniforms(uniforms);
        SystemTimeUniforms.addSystemTimeUniforms(uniforms);
        BiomeUniforms.addBiomeUniforms(uniforms);
        new CelestialUniforms(directives.getSunPathRotation()).addCelestialUniforms(uniforms);
        AurumExclusiveUniforms.addAurumExclusiveUniforms(uniforms);
        MatrixUniforms.addMatrixUniforms(uniforms, directives);
        HardcodedCustomUniforms.addHardcodedCustomUniforms(uniforms, updateNotifier, declaredByPack);

        CommonUniforms.generalCommonUniforms(uniforms, updateNotifier, directives);
    }

    public static void generalCommonUniforms(UniformHolder uniforms, FrameUpdateNotifier updateNotifier, PackDirectives directives) {
        ExternallyManagedUniforms.addExternallyManagedUniforms116(uniforms);

        SmoothedVec2f eyeBrightnessSmooth = new SmoothedVec2f(directives.getEyeBrightnessHalfLife(), directives.getEyeBrightnessHalfLife(), CommonUniforms::getEyeBrightness, updateNotifier);

        uniforms
                .uniform1b(PER_FRAME, "hideGUI", () -> client.options.hudHidden)
                .uniform1f(PER_FRAME, "eyeAltitude", () -> {
                    Entity camera = Objects.requireNonNull(client.getCameraEntity());
                    return (float) camera.getPos().y + camera.getEyeHeight();
                })
                .uniform1i(PER_FRAME, "isEyeInWater", CommonUniforms::isEyeInWater)
                .uniform1f(ONCE, "darknessFactor", () -> 0.0F)
                .uniform1f(ONCE, "darknessLightFactor", () -> 0.0F)
                .uniform1f(PER_FRAME, "blindness", CommonUniforms::getBlindness)
                .uniform1f(PER_FRAME, "nightVision", CommonUniforms::getNightVision)
                .uniform1f(PER_FRAME, "screenBrightness", () -> client.options.gamma)
                .uniform4f(ONCE, "entityColor", Vector4f::new)
                .uniform1f(PER_TICK, "playerMood", CommonUniforms::getPlayerMood)
                .uniform2i(PER_FRAME, "eyeBrightness", CommonUniforms::getEyeBrightness)
                .uniform2i(PER_FRAME, "eyeBrightnessSmooth", () -> {
                    Vector2f smoothed = eyeBrightnessSmooth.get();
                    return new Vector2i((int) smoothed.x(), (int) smoothed.y());
                })
                .uniform1f(PER_TICK, "rainStrength", CommonUniforms::getRainStrength)
                .uniform1f(PER_TICK, "wetness", new SmoothedFloat(directives.getWetnessHalfLife(), directives.getDrynessHalfLife(), CommonUniforms::getRainStrength, updateNotifier))
                .uniform3d(PER_FRAME, "skyColor", CommonUniforms::getSkyColor)
                .uniform3d(PER_FRAME, "fogColor", CapturedRenderingState.INSTANCE::getFogColor);
    }

    private static Vector3d getSkyColor() {
        if (client.world == null || client.getCameraEntity() == null) {
            return ZERO_VECTOR_3d;
        }

        var vec = client.world.getCloudColor(CapturedRenderingState.INSTANCE.getTickDelta());
        return new Vector3d(vec.x, vec.y, vec.z);
    }

    static float getBlindness() {
        Entity cameraEntity = client.getCameraEntity();

        if (cameraEntity instanceof LivingEntity) {
            StatusEffectInstance blindness = ((LivingEntity) cameraEntity).getEffectInstance(StatusEffect.BLINDNESS);

            if (blindness != null) {
                return Math.clamp(0.0F, 1.0F, blindness.getDuration() / 20.0F);
            }
        }

        return 0.0F;
    }

    private static float getPlayerMood() {
        // TODO: Didn't even know players could have moods
        return 0.0F;
    }

    static float getRainStrength() {
        if (client.world == null) {
            return 0f;
        }

        // Note: Ensure this is in the range of 0 to 1 - some custom servers send out of range values.
        return Math.clamp(0.0F, 1.0F,
                client.world.getRainGradient(CapturedRenderingState.INSTANCE.getTickDelta()));
    }

    private static Vector2i getEyeBrightness() {
        if (client.getCameraEntity() == null || client.world == null) {
            return ZERO_VECTOR_2i;
        }

        Vec3d feet = client.getCameraEntity().getPos();
        Vec3d eyes = new Vec3d(feet.x, feet.y + client.getCameraEntity().getEyeHeight(), feet.z);
        BlockPos eyeBlockPos = new BlockPos(eyes);

        int blockLight = client.world.getLightAtPos(LightType.BLOCK, eyeBlockPos);
        int skyLight = client.world.getLightAtPos(LightType.SKY, eyeBlockPos);

        return new Vector2i(blockLight * 16, skyLight * 16);
    }

    private static float getNightVision() {
        Entity cameraEntity = client.getCameraEntity();

        if (cameraEntity instanceof LivingEntity livingEntity) {
            try {
                // See MixinGameRenderer#aurum$safecheckNightvisionStrength.
                //
                // We modify the behavior of getNightVisionScale so that it's safe for us to call it even on entities
                // that don't have the effect, allowing us to pick up modified night vision strength values from mods
                // like Origins.
                //
                // See: https://github.com/apace100/apoli/blob/320b0ef547fbbf703de7154f60909d30366f6500/src/main/java/io/github/apace100/apoli/mixin/GameRendererMixin.java#L153
                float nightVisionStrength = getNightVisionStrength(livingEntity, CapturedRenderingState.INSTANCE.getTickDelta());

                if (nightVisionStrength > 0) {
                    // Just protecting against potential weird mod behavior
                    return Math.clamp(0.0F, 1.0F, nightVisionStrength);
                }
            } catch (NullPointerException e) {
                // If our injection didn't get applied, a NullPointerException will occur from calling that method if
                // the entity doesn't currently have night vision. This isn't pretty but it's functional.
                return 0.0F;
            }
        }

        return 0.0F;
    }

    private static float getNightVisionStrength(LivingEntity entity, float tickDelta) {
        int i = entity.getEffectInstance(StatusEffect.NIGHTVISION).getDuration();
        return i > 200 ? 1.0F : 0.7F + MathHelper.sin(((float) i - tickDelta) * (float) java.lang.Math.PI * 0.2F) * 0.3F;
    }

    static int isEyeInWater() {
        // Note: With certain utility / cheat mods, this method will return air even when the player is submerged when
        // the "No Overlay" feature is enabled.
        //
        // I'm not sure what the best way to deal with this is, but the current approach seems to be an acceptable one -
        // after all, disabling the overlay results in the intended effect of it not really looking like you're
        // underwater on most shaderpacks. For now, I will leave this as-is, but it is something to keep in mind.
        Entity cameraEntity = client.getCameraEntity();

        if (cameraEntity.isSubmergedIn(Material.WATER)) {
            return 1;
        } else if (cameraEntity.isSubmergedIn(Material.LAVA)) {
            return 2;
        } else {
            return 0;
        }
    }

    static {
        GbufferPrograms.init();
    }
}
