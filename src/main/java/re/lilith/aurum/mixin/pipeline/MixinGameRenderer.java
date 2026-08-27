package re.lilith.aurum.mixin.pipeline;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.resource.ResourceManager;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.celeritas.ArgentumFastPaths;
import re.lilith.aurum.gl.program.Program;
import re.lilith.aurum.mixin.access.CameraAccessor;
import re.lilith.aurum.mixin.access.WorldRendererAccessor;
import re.lilith.aurum.pipeline.WorldRenderingPhase;
import re.lilith.aurum.pipeline.WorldRenderingPipeline;
import re.lilith.aurum.pipeline.pathways.HandRenderer;
import re.lilith.aurum.pipeline.state.CapturedRenderingState;
import re.lilith.aurum.uniforms.SystemTimeUniforms;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    @Shadow
    protected abstract void renderDebugCrosshair(float tickDelta);

    @Shadow
    private boolean renderHand;

    @Shadow
    protected abstract void renderHand(float tickDelta, int anaglyphOffset);

    @Shadow
    private float fogRed;

    @Shadow
    private float fogGreen;

    @Shadow
    private float fogBlue;

    @Inject(method = "updateFog(F)V", at = @At("TAIL"))
    private void aurum$captureFogColor(float tickDelta, CallbackInfo ci) {
        CapturedRenderingState.INSTANCE.setFogColor(this.fogRed, this.fogGreen, this.fogBlue);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void aurum$logSystem(MinecraftClient minecraftClient, ResourceManager resourceManager, CallbackInfo ci) {
        Aurum.LOGGER.info("Hardware information:");
        Aurum.LOGGER.info("CPU: {}", GLX.getProcessor());
        Aurum.LOGGER.info("GPU: {} (Supports OpenGL {})", GL11.glGetString(GL11.GL_RENDERER), GL11.glGetString(GL11.GL_VERSION));
        Aurum.LOGGER.info("OS: {} ({})", System.getProperty("os.name"), System.getProperty("os.version"));
    }

    @Redirect(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderArmHoldingItem(F)V"))
    private void disableVanillaHandRendering(HeldItemRenderer instance, float tickDelta) {
        if (Aurum.getCurrentPack().isPresent()) {
            return;
        }

        instance.renderArmHoldingItem(tickDelta);
    }

    @Unique
    private WorldRenderingPipeline pipeline;


    // Begin shader rendering after buffers have been cleared.
    // At this point we've ensured that Minecraft's main framebuffer is cleared.
    // This is important or else very odd issues will happen with shaders that have a final pass that doesn't write to
    // all pixels.
    @Inject(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;clear(I)V", shift = At.Shift.AFTER))
    private void aurum$beginLevelRender(int anaglyphFilter, float tickDelta, long limitTime, CallbackInfo ci) {
        CapturedRenderingState.INSTANCE.setGbufferModelView(new Matrix4f(CameraAccessor.getModelMatrix()));
        CapturedRenderingState.INSTANCE.setGbufferProjection(new Matrix4f(CameraAccessor.getProjectionMatrix()));
        CapturedRenderingState.INSTANCE.setTickDelta(tickDelta);
        SystemTimeUniforms.COUNTER.beginFrame();
        SystemTimeUniforms.TIMER.beginFrame(limitTime);

        Program.unbind();

        pipeline = Aurum.getPipelineManager().preparePipeline(Aurum.getCurrentDimension());

        ArgentumFastPaths.update();

        pipeline.beginLevelRendering();
    }

    // Inject a bit early so that we can end our rendering before mods like VoxelMap (which inject at RETURN)
    // render their waypoint beams.
    @Inject(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", ordinal = 18), cancellable = true)
    private void aurum$endLevelRender(int anaglyphFilter, float tickDelta, long limitTime, CallbackInfo ci) {
        if (this.renderHand) {
            if (Aurum.getCurrentPack().isEmpty()) {
                GlStateManager.clear(256);
            }

            this.renderHand(tickDelta, anaglyphFilter);
            this.renderDebugCrosshair(tickDelta);
        }


        HandRenderer.INSTANCE.renderTranslucent(tickDelta, (GameRenderer) (Object) this, pipeline);
        MinecraftClient.getInstance().profiler.swap("aurum_final");
        pipeline.finalizeLevelRendering();
        pipeline = null;
        Program.unbind();
        ci.cancel();
    }

    // Setup shadow terrain & render shadows before the main terrain setup. We need to do things in this order to
    // avoid breaking other mods such as Light Overlay: https://github.com/IrisShaders/Iris/issues/1356
    //
    // Camera.update copies the world modelview and projection out of GL, so this is the first point in the
    // frame where the gbuffer matrices are the current ones. Capturing them earlier leaves them a frame
    // stale, which offsets every shadow lookup, since packs rebuild world position with the inverse.
    @Inject(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;update(Lnet/minecraft/entity/player/PlayerEntity;Z)V", shift = At.Shift.AFTER))
    private void aurum$renderTerrainShadows(int anaglyphFilter, float tickDelta, long limitTime, CallbackInfo ci) {
        CapturedRenderingState.INSTANCE.setGbufferModelView(new Matrix4f(CameraAccessor.getModelMatrix()));
        CapturedRenderingState.INSTANCE.setGbufferProjection(new Matrix4f(CameraAccessor.getProjectionMatrix()));

        pipeline.renderShadows((WorldRendererAccessor) MinecraftClient.getInstance().worldRenderer);
    }

    @Inject(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;renderSky(FI)V"))
    private void aurum$beginSky(int anaglyphFilter, float tickDelta, long limitTime, CallbackInfo ci) {
        // The horizon box has to be drawn here rather than in beginLevelRendering. That runs right after
        // the buffer clear, before setupCamera and Camera.update, so the modelview and projection there
        // are still the previous frame's and the prism lands in the wrong place.

        pipeline.renderHorizonBox();

        // Use CUSTOM_SKY until levelFogColor is called as a heuristic to catch FabricSkyboxes.
        pipeline.setPhase(WorldRenderingPhase.CUSTOM_SKY);
    }

    @Redirect(method = "renderWorld(IFJ)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;viewDistance:I", opcode = Opcodes.GETFIELD))
    private int aurum$alwaysRenderSky(GameOptions instance) {
        return Math.max(instance.viewDistance, 4);
    }


    @Inject(method = "renderWeather(F)V", at = @At("HEAD"), cancellable = true)
    private void aurum$disableWeather(float tickDelta, CallbackInfo ci) {
        WorldRenderingPipeline pipeline = Aurum.getPipelineManager().getPipelineNullable();

        if (pipeline != null && !pipeline.shouldRenderWeather()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;renderWeather(F)V"))
    private void aurum$beginWeather(int anaglyphFilter, float tickDelta, long limitTime, CallbackInfo ci) {
        Aurum.getPipelineManager().getPipelineNullable().setPhase(WorldRenderingPhase.RAIN_SNOW);
    }

    @ModifyArg(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;depthMask(Z)V", ordinal = 0))
    private boolean aurum$writeRainAndSnowToDepthBuffer(boolean depthMaskEnabled) {
        if (Aurum.getPipelineManager().getPipelineNullable().shouldWriteRainAndSnowToDepthBuffer()) {
            return true;
        }

        return depthMaskEnabled;
    }

    @Inject(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;renderWeather(F)V", shift = At.Shift.AFTER))
    private void aurum$endWeather(int anaglyphFilter, float tickDelta, long limitTime, CallbackInfo ci) {
        Aurum.getPipelineManager().getPipelineNullable().setPhase(WorldRenderingPhase.NONE);
    }

    @Inject(method = "renderWorld(IFJ)V", at = @At(value = "CONSTANT", args = "stringValue=translucent"))
    private void aurum$beginTranslucents(int anaglyphFilter, float tickDelta, long limitTime, CallbackInfo ci) {
        Aurum.getPipelineManager().getPipelineNullable().beginHand();
        HandRenderer.INSTANCE.renderSolid(tickDelta, (GameRenderer) (Object) this, pipeline);
        MinecraftClient.getInstance().profiler.swap("aurum_pre_translucent");
        Aurum.getPipelineManager().getPipelineNullable().beginTranslucents();
    }
}
