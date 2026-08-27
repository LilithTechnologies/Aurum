package re.lilith.aurum.pipeline;

import re.lilith.aurum.gbuffer.matching.InputAvailability;
import re.lilith.aurum.gbuffer.matching.SpecialCondition;
import re.lilith.aurum.gbuffer.state.RenderTargetStateListener;
import re.lilith.aurum.mixin.access.WorldRendererAccessor;
import re.lilith.aurum.pipeline.impl.celeritas.CeleritasTerrainPipeline;
import re.lilith.aurum.shaderpack.CloudSetting;
import re.lilith.aurum.uniforms.utility.FrameUpdateNotifier;

import java.util.List;
import java.util.OptionalInt;

public interface WorldRenderingPipeline {
    void beginLevelRendering();

    default void renderHorizonBox() {
    }

    void renderShadows(WorldRendererAccessor levelRenderer);

    void addDebugText(List<String> messages);

    OptionalInt getForcedShadowRenderDistanceChunksForDisplay();

    WorldRenderingPhase getPhase();

    void beginCeleritasTerrainRendering();

    void endCeleritasTerrainRendering();

    void setOverridePhase(WorldRenderingPhase phase);

    void setPhase(WorldRenderingPhase phase);

    void setInputs(InputAvailability availability);

    void setSpecialCondition(SpecialCondition special);

    void syncProgram();

    RenderTargetStateListener getRenderTargetStateListener();

    int getCurrentNormalTexture();

    int getCurrentSpecularTexture();

    void onBindTexture(int id);

    void beginHand();

    void beginTranslucents();

    void finalizeLevelRendering();

    void destroy();

    CeleritasTerrainPipeline getCeleritasTerrainPipeline();

    FrameUpdateNotifier getFrameUpdateNotifier();

    boolean shouldDisableVanillaEntityShadows();

    boolean shouldDisableDirectionalShading();

    CloudSetting getCloudSetting();

    boolean shouldRenderUnderwaterOverlay();

    boolean shouldRenderVignette();

    boolean shouldRenderSun();

    boolean shouldRenderMoon();

    boolean shouldRenderWeather();

    boolean shouldRenderWeatherParticles();

    boolean shouldWriteRainAndSnowToDepthBuffer();

    boolean shouldRenderParticlesBeforeDeferred();

    boolean allowConcurrentCompute();

    float getSunPathRotation();
}
