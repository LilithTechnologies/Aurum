package re.lilith.aurum.pipeline.impl;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL20;
import re.lilith.aurum.gbuffer.BlockRenderingSettings;
import re.lilith.aurum.gbuffer.matching.InputAvailability;
import re.lilith.aurum.gbuffer.matching.SpecialCondition;
import re.lilith.aurum.gbuffer.state.RenderTargetStateListener;
import re.lilith.aurum.mixin.access.WorldRendererAccessor;
import re.lilith.aurum.pipeline.WorldRenderingPhase;
import re.lilith.aurum.pipeline.WorldRenderingPipeline;
import re.lilith.aurum.pipeline.impl.celeritas.CeleritasTerrainPipeline;
import re.lilith.aurum.shaderpack.CloudSetting;
import re.lilith.aurum.uniforms.utility.FrameUpdateNotifier;

import java.util.List;
import java.util.OptionalInt;

public class FixedFunctionWorldRenderingPipeline implements WorldRenderingPipeline {
    public FixedFunctionWorldRenderingPipeline() {
        BlockRenderingSettings.INSTANCE.setDisableDirectionalShading(shouldDisableDirectionalShading());
        BlockRenderingSettings.INSTANCE.setUseSeparateAo(false);
        BlockRenderingSettings.INSTANCE.setAmbientOcclusionLevel(1.0f);
        BlockRenderingSettings.INSTANCE.setUseExtendedVertexFormat(false);
        BlockRenderingSettings.INSTANCE.setBlockTypeIds(null);
    }

    @Override
    public void beginLevelRendering() {
        // Use the default Minecraft framebuffer and ensure that no programs are in use
        MinecraftClient.getInstance().getFramebuffer().bind(true);
        GL20.glUseProgram(0);
    }

    @Override
    public void renderShadows(WorldRendererAccessor levelRenderer) {
        // stub: nothing to do here
    }

    @Override
    public void addDebugText(List<String> messages) {
        // stub: nothing to do here
    }

    @Override
    public OptionalInt getForcedShadowRenderDistanceChunksForDisplay() {
        return OptionalInt.empty();
    }

    @Override
    public WorldRenderingPhase getPhase() {
        return WorldRenderingPhase.NONE;
    }

    @Override
    public void beginCeleritasTerrainRendering() {

    }

    @Override
    public void endCeleritasTerrainRendering() {

    }

    @Override
    public void setOverridePhase(WorldRenderingPhase phase) {

    }

    @Override
    public void setPhase(WorldRenderingPhase phase) {

    }

    @Override
    public void setInputs(InputAvailability availability) {

    }

    @Override
    public void setSpecialCondition(SpecialCondition special) {

    }

    @Override
    public void syncProgram() {

    }

    @Override
    public RenderTargetStateListener getRenderTargetStateListener() {
        return RenderTargetStateListener.NOP;
    }

    @Override
    public int getCurrentNormalTexture() {
        return 0;
    }

    @Override
    public int getCurrentSpecularTexture() {
        return 0;
    }

    @Override
    public void onBindTexture(int id) {

    }

    @Override
    public void beginHand() {
        // stub: nothing to do here
    }

    @Override
    public void beginTranslucents() {
        // stub: nothing to do here
    }

    @Override
    public void finalizeLevelRendering() {
        // stub: nothing to do here
    }

    @Override
    public void destroy() {
        // stub: nothing to do here
    }

    @Override
    public CeleritasTerrainPipeline getCeleritasTerrainPipeline() {
        // no shaders to override
        return null;
    }

    @Override
    public FrameUpdateNotifier getFrameUpdateNotifier() {
        // return a dummy notifier
        return new FrameUpdateNotifier();
    }

    @Override
    public boolean shouldDisableVanillaEntityShadows() {
        return false;
    }

    @Override
    public boolean shouldDisableDirectionalShading() {
        return false;
    }

    @Override
    public CloudSetting getCloudSetting() {
        return CloudSetting.DEFAULT;
    }

    @Override
    public boolean shouldRenderUnderwaterOverlay() {
        return true;
    }

    @Override
    public boolean shouldRenderVignette() {
        return true;
    }

    @Override
    public boolean shouldRenderSun() {
        return true;
    }

    @Override
    public boolean shouldRenderMoon() {
        return true;
    }

    @Override
    public boolean shouldRenderWeather() {
        return true;
    }

    @Override
    public boolean shouldRenderWeatherParticles() {
        return true;
    }

    @Override
    public boolean shouldWriteRainAndSnowToDepthBuffer() {
        return false;
    }

    @Override
    public boolean shouldRenderParticlesBeforeDeferred() {
        return false;
    }

    @Override
    public boolean allowConcurrentCompute() {
        return false;
    }

    @Override
    public float getSunPathRotation() {
        // No sun tilt
        return 0;
    }
}
