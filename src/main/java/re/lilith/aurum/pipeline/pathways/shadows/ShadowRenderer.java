package re.lilith.aurum.pipeline.pathways.shadows;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.mixin.access.EntityRenderDispatcherAccessor;
import re.lilith.aurum.mixin.access.WorldRendererAccessor;
import re.lilith.aurum.pipeline.pathways.shadows.frustum.BoxCuller;
import re.lilith.aurum.pipeline.pathways.shadows.frustum.CullingDataCache;
import re.lilith.aurum.pipeline.pathways.shadows.frustum.FrustumHolder;
import re.lilith.aurum.pipeline.state.CapturedRenderingState;
import re.lilith.aurum.shaderpack.PackDirectives;
import re.lilith.aurum.shaderpack.PackShadowDirectives;
import re.lilith.aurum.shaderpack.ShadowCullState;
import re.lilith.aurum.shaderpack.program.ProgramSource;
import re.lilith.aurum.uniforms.CameraUniforms;
import re.lilith.aurum.uniforms.CelestialUniforms;
import re.lilith.aurum.vertices.PoseStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class ShadowRenderer {
    public static Matrix4f MODELVIEW;
    public static Matrix4f PROJECTION;
    public static List<BlockEntity> visibleBlockEntities;
    public static boolean ACTIVE = false;
    private final float halfPlaneLength;
    private final float renderDistanceMultiplier;
    private final float entityShadowDistanceMultiplier;
    private final float intervalSize;
    private final Float fov;
    private final ShadowRenderTargets targets;
    @Nullable
    private final ShadowCompositeRenderer compositeRenderer;
    private final ShadowFrustumFactory frustumFactory;
    private final boolean shouldRenderTerrain;
    private final boolean shouldRenderTranslucent;
    private final boolean shouldRenderEntities;
    private final boolean shouldRenderPlayer;
    private final boolean shouldRenderBlockEntities;
    private final float sunPathRotation;
    private final ShadowSamplingConfigurer samplingConfigurer;
    private final String debugStringOverall;
    private FrustumHolder terrainFrustumHolder;
    private FrustumHolder entityFrustumHolder;
    private String debugStringTerrain = "(unavailable)";
    private int renderedShadowEntities = 0;
    private int renderedShadowBlockEntities = 0;
    private Profiler profiler;

    public ShadowRenderer(ProgramSource shadow, PackDirectives directives,
                          ShadowRenderTargets shadowRenderTargets) {
        this(shadow, directives, shadowRenderTargets, null);
    }

    public ShadowRenderer(ProgramSource shadow, PackDirectives directives,
                          ShadowRenderTargets shadowRenderTargets, @Nullable ShadowCompositeRenderer compositeRenderer) {

        this.profiler = MinecraftClient.getInstance().profiler;
        this.compositeRenderer = compositeRenderer;

        final PackShadowDirectives shadowDirectives = directives.getShadowDirectives();

        this.halfPlaneLength = shadowDirectives.getDistance();
        this.renderDistanceMultiplier = shadowDirectives.getDistanceRenderMul();
        this.entityShadowDistanceMultiplier = shadowDirectives.getEntityShadowDistanceMul();
        int resolution = shadowDirectives.getResolution();
        this.intervalSize = shadowDirectives.getIntervalSize();
        float voxelDistance = shadowDirectives.getVoxelDistance();
        this.shouldRenderTerrain = shadowDirectives.shouldRenderTerrain();
        this.shouldRenderTranslucent = shadowDirectives.shouldRenderTranslucent();
        this.shouldRenderEntities = shadowDirectives.shouldRenderEntities();
        this.shouldRenderPlayer = shadowDirectives.shouldRenderPlayer();
        this.shouldRenderBlockEntities = shadowDirectives.shouldRenderBlockEntities();

        debugStringOverall = "half plane = " + halfPlaneLength + " meters @ " + resolution + "x" + resolution;

        this.terrainFrustumHolder = new FrustumHolder();
        this.entityFrustumHolder = new FrustumHolder();

        this.fov = shadowDirectives.getFov();
        this.targets = shadowRenderTargets;

        // Assume that the shader pack is doing voxelization if a geometry shader is detected.
        // Also assume voxelization if image load / store is detected.
        boolean packHasVoxelization = shadow != null && shadow.getGeometrySource().isPresent();
        ShadowCullState packCullingState = shadow != null ? shadowDirectives.getCullingState() : ShadowCullState.DEFAULT;

        this.sunPathRotation = directives.getSunPathRotation();

        this.frustumFactory = new ShadowFrustumFactory(packCullingState, packHasVoxelization, halfPlaneLength,
                voxelDistance, sunPathRotation);

        this.samplingConfigurer = new ShadowSamplingConfigurer(shadowRenderTargets, resolution, halfPlaneLength);
        this.samplingConfigurer.configure(shadowDirectives);
    }

    public void setUsesImages(boolean usesImages) {
        this.frustumFactory.setUsesImages(usesImages);
    }

    public static PoseStack createShadowModelView(float sunPathRotation, float intervalSize) {
        // Determine the camera position
        Vector3d cameraPos = CameraUniforms.getUnshiftedCameraPosition();

        double cameraX = cameraPos.x;
        double cameraY = cameraPos.y;
        double cameraZ = cameraPos.z;

        // Set up our modelview matrix stack
        PoseStack modelView = new PoseStack();
        ShadowMatrices.createModelViewMatrix(modelView, getShadowAngle(), intervalSize, sunPathRotation, cameraX, cameraY, cameraZ);

        return modelView;
    }

    private static ClientWorld getLevel() {
        return Objects.requireNonNull(MinecraftClient.getInstance().world);
    }

    private static float getSkyAngle() {
        return getLevel().getSkyAngle(CapturedRenderingState.INSTANCE.getTickDelta());
    }

    private static float getSunAngle() {
        float skyAngle = getSkyAngle();

        if (skyAngle < 0.75F) {
            return skyAngle + 0.25F;
        } else {
            return skyAngle - 0.75F;
        }
    }

    private static float getShadowAngle() {
        float shadowAngle = getSunAngle();

        if (!CelestialUniforms.isDay()) {
            shadowAngle -= 0.5F;
        }

        return shadowAngle;
    }


    private void setupGlState(PoseStack modelView, Matrix4f projMatrix) {
        // Set up our projection matrix and load it into the legacy matrix stack
        AurumRenderSystem.setupProjectionMatrix(projMatrix.get(new float[16]));

        // Terrain, entities, and block entities are all drawn with the fixed-function matrix stack on this version.
        // The shadow modelview must therefore replace the camera modelview for the whole shadow pass.
        AurumRenderSystem.setupModelViewMatrix(modelView.last().pose().get(new float[16]));

        // Disable backface culling
        // This partially works around an issue where if the front face of a mountain isn't visible, it casts no
        // shadow.
        //
        // However, it only partially resolves issues of light leaking into caves.
        //
        // TODO: Better way of preventing light from leaking into places where it shouldn't
        GlStateManager.disableCull();
    }

    private static void aurum$resetPackVertexAttributes() {
        for (int location = 11; location <= 14; location++) {
            org.lwjgl.opengl.GL20.glDisableVertexAttribArray(location);
            org.lwjgl.opengl.GL20.glVertexAttrib4f(location, 0.0F, 0.0F, 0.0F, 1.0F);
        }
    }

    private void restoreGlState() {
        // Restore backface culling
        GlStateManager.enableCull();

        // Make sure to unload the shadow matrices
        AurumRenderSystem.restoreModelViewMatrix();
        AurumRenderSystem.restoreProjectionMatrix();
    }

    private void copyPreTranslucentDepth() {
        profiler.swap("translucent depth copy");

        targets.copyPreTranslucentDepth();
    }

    private void renderEntities(WorldRendererAccessor levelRenderer, CameraView frustum, double cameraX, double cameraY, double cameraZ, float tickDelta) {
        EntityRenderDispatcher dispatcher = levelRenderer.getEntityRenderDispatcher();

        AtomicInteger shadowEntities = new AtomicInteger();

        profiler.push("cull");

        var client = MinecraftClient.getInstance();

        dispatcher.updateCamera(getLevel(), client.textRenderer, client.getCameraEntity(), client.targetedEntity, client.options, tickDelta);
        dispatcher.updateCamera(cameraX, cameraY, cameraZ);

        List<Entity> renderedEntities = new ArrayList<>(32);

        // TODO: I'm sure that this can be improved / optimized.
        for (Entity entity : getLevel().loadedEntities) {
            if (!dispatcher.shouldRender(entity, frustum, cameraX, cameraY, cameraZ)) {
                continue;
            }

            renderedEntities.add(entity);
        }

        profiler.swap("build geometry");

        for (Entity entity : renderedEntities) {
            dispatcher.renderEntity(entity, tickDelta);
            shadowEntities.getAndIncrement();
        }

        renderedShadowEntities = shadowEntities.get();

        profiler.pop();
    }

    private void renderPlayerEntity(WorldRendererAccessor levelRenderer, CameraView frustum, double cameraX, double cameraY, double cameraZ, float tickDelta) {
        EntityRenderDispatcher dispatcher = levelRenderer.getEntityRenderDispatcher();

        profiler.push("cull");

        Entity player = MinecraftClient.getInstance().player;

        if (!dispatcher.shouldRender(player, frustum, cameraX, cameraY, cameraZ)) {
            return;
        }

        profiler.swap("build geometry");

        AtomicInteger shadowEntities = new AtomicInteger();

        Entity previousCameraEntity = ((EntityRenderDispatcherAccessor) dispatcher).getCameraEntity();
        ((EntityRenderDispatcherAccessor) dispatcher).setCameraEntity(MinecraftClient.getInstance().getCameraEntity());

        if (player.rider != null) {
            dispatcher.renderEntity(player.rider, tickDelta);
            shadowEntities.getAndIncrement();
        }

        if (player.vehicle != null) {
            dispatcher.renderEntity(player.vehicle, tickDelta);
            shadowEntities.getAndIncrement();
        }

        dispatcher.renderEntity(player, tickDelta);

        ((EntityRenderDispatcherAccessor) dispatcher).setCameraEntity(previousCameraEntity);

        shadowEntities.getAndIncrement();

        renderedShadowEntities = shadowEntities.get();

        profiler.pop();
    }

    private void renderBlockEntities(double cameraX, double cameraY, double cameraZ, float tickDelta, boolean hasEntityFrustum) {
        profiler.push("build blockentities");

        int shadowBlockEntities = 0;
        BoxCuller culler = null;
        if (hasEntityFrustum) {
            culler = new BoxCuller(halfPlaneLength * (renderDistanceMultiplier * entityShadowDistanceMultiplier));
            culler.setPosition(cameraX, cameraY, cameraZ);
        }

        var client = MinecraftClient.getInstance();

        BlockEntityRenderDispatcher.INSTANCE.updateCamera(getLevel(), client.getTextureManager(), client.textRenderer, client.getCameraEntity(), tickDelta);

        BlockEntityRenderDispatcher.CAMERA_X = cameraX;
        BlockEntityRenderDispatcher.CAMERA_Y = cameraY;
        BlockEntityRenderDispatcher.CAMERA_Z = cameraZ;

        for (BlockEntity entity : visibleBlockEntities) {
            BlockPos pos = entity.getPos();
            if (hasEntityFrustum) {
                if (culler.isCulled(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1)) {
                    continue;
                }
            }

            BlockEntityRenderDispatcher.INSTANCE.renderEntity(entity, tickDelta, -1);

            shadowBlockEntities++;
        }

        renderedShadowBlockEntities = shadowBlockEntities;

        profiler.pop();
    }

    public void renderShadows(WorldRendererAccessor levelRenderer) {
        ACTIVE = true;

        try {
            renderShadowsUnguarded(levelRenderer);
        } finally {
            ACTIVE = false;
        }
    }

    private void renderShadowsUnguarded(WorldRendererAccessor levelRenderer) {
        // We have to re-query this each frame since this changes based on whether the profiler is active
        // If the profiler is inactive, it will return InactiveProfiler.INSTANCE
        this.profiler = MinecraftClient.getInstance().profiler;

        // Get the current tick delta. Normally this is the same as client.getTickDelta(), but when the game is paused,
        // it is set to a fixed value.
        final float tickDelta = CapturedRenderingState.INSTANCE.getTickDelta();

        MinecraftClient client = MinecraftClient.getInstance();

        profiler.swap("shadows");

        visibleBlockEntities = new ArrayList<>();

        // Create our camera
        PoseStack modelView = createShadowModelView(this.sunPathRotation, this.intervalSize);
        MODELVIEW = new Matrix4f(modelView.last().pose());
        if (this.fov != null) {
            // If FOV is not null, the pack wants a perspective based projection matrix. (This is to support legacy packs)
            PROJECTION = ShadowMatrices.createPerspectiveMatrix(this.fov);
        } else {
            PROJECTION = ShadowMatrices.createOrthoMatrix(halfPlaneLength);
        }

        profiler.push("terrain_setup");

        if (levelRenderer instanceof CullingDataCache) {
            ((CullingDataCache) levelRenderer).aurum$saveState();
        }

        profiler.push("initialize frustum");

        terrainFrustumHolder = frustumFactory.create(renderDistanceMultiplier, terrainFrustumHolder);

        // Determine the player camera position
        Vector3d cameraPos = CameraUniforms.getUnshiftedCameraPosition();

        double cameraX = cameraPos.x();
        double cameraY = cameraPos.y();
        double cameraZ = cameraPos.z();

        Frustum terrainFrustum = terrainFrustumHolder.getFrustum();
        terrainFrustum.start();

        PROJECTION.get(terrainFrustum.projectionMatrix);
        MODELVIEW.get(terrainFrustum.modelMatrix);

        for (float[] plane : terrainFrustum.homogeneousCoordinates) {
            plane[0] = 0.0F;
            plane[1] = 0.0F;
            plane[2] = 0.0F;
            plane[3] = 1.0F;
        }

        profiler.pop();

        // Disable chunk occlusion culling - it's a bit complex to get this properly working with shadow rendering
        // as-is, however in the future it will be good to work on restoring it for a nice performance boost.
        //
        // TODO: Get chunk occlusion working with shadows
        boolean wasChunkCullingEnabled = client.chunkCullingEnabled;
        client.chunkCullingEnabled = false;

        // Always schedule a terrain update
        // TODO: Only schedule a terrain update if the sun / moon is moving, or the shadow map camera moved.
        // We have to ensure that we don't regenerate clouds every frame, since that's what needsUpdate ends up doing.
        // This took up to 10% of the frame time before we applied this fix! That's really bad!
        // Execute the vanilla terrain setup / culling routines using our shadow frustum.
        var cameraView = new CullingCameraView(terrainFrustumHolder.getFrustum());

        Vector3f shadowEyeOffset = new Matrix4f(MODELVIEW).invert().transformPosition(new org.joml.Vector3f());

        cameraView.setPos(cameraX - shadowEyeOffset.x, cameraY - shadowEyeOffset.y, cameraZ - shadowEyeOffset.z);


        levelRenderer.invokeSetupRender(client.player, tickDelta, cameraView, levelRenderer.getFrameId(), false);

        // Don't forget to increment the frame counter! This variable is arbitrary and only used in terrain setup,
        // and if it's not incremented, the vanilla culling code will get confused and think that it's already seen
        // chunks during traversal, and break rendering in concerning ways.
        levelRenderer.setFrameId(levelRenderer.getFrameId() + 1);

        client.chunkCullingEnabled = wasChunkCullingEnabled;

        profiler.swap("terrain");

        setupGlState(modelView, PROJECTION);

        // Render all opaque terrain unless pack requests not to
        if (shouldRenderTerrain) {
            levelRenderer.invokeRenderLayer(RenderLayer.SOLID, tickDelta, 2, client.player);
            levelRenderer.invokeRenderLayer(RenderLayer.CUTOUT, tickDelta, 2, client.player);
            levelRenderer.invokeRenderLayer(RenderLayer.CUTOUT_MIPPED, tickDelta, 2, client.player);
        }


        profiler.swap("entities");

        // Terrain leaves the shader pack's generic attribute arrays bound to its own vertex buffer. Entities are
        // drawn with the fixed-function arrays and never touch locations 11-14, so without this the shadow program
        // reads mc_Entity straight out of the last terrain buffer. That feeds a garbage material id to DoWave,
        // which displaces part of the entity in the shadow map and leaves it self-shadowing in animated stripes.
        // Only the shadow program reads mc_Entity, which is why the entity gbuffer looks correct.
        aurum$resetPackVertexAttributes();

        // Create a constrained shadow frustum for entities to avoid rendering faraway entities in the shadow pass,
        // if the shader pack has requested it. Otherwise, use the same frustum as for terrain.
        boolean hasEntityFrustum = false;

        if (entityShadowDistanceMultiplier == 1.0F || entityShadowDistanceMultiplier < 0.0F) {
            entityFrustumHolder.setInfo(terrainFrustumHolder.getFrustum(), terrainFrustumHolder.getDistanceInfo(), terrainFrustumHolder.getCullingInfo());
        } else {
            hasEntityFrustum = true;
            entityFrustumHolder = frustumFactory.create(renderDistanceMultiplier * entityShadowDistanceMultiplier, entityFrustumHolder);
        }

        Frustum entityShadowFrustum = entityFrustumHolder.getFrustum();
        entityShadowFrustum.start();

        cameraView = new CullingCameraView(entityFrustumHolder.getFrustum());

        GlStateManager.polygonOffset(1.0F, 1.0F);
        GlStateManager.enablePolyOffset();

        try {
            if (shouldRenderEntities) {
                renderEntities(levelRenderer, cameraView, cameraX, cameraY, cameraZ, tickDelta);
            } else if (shouldRenderPlayer) {
                renderPlayerEntity(levelRenderer, cameraView, cameraX, cameraY, cameraZ, tickDelta);
            }

            if (shouldRenderBlockEntities) {
                renderBlockEntities(cameraX, cameraY, cameraZ, tickDelta, hasEntityFrustum);
            }
        } finally {
            GlStateManager.disablePolyOffset();
            GlStateManager.polygonOffset(0.0F, 0.0F);
        }


        profiler.swap("draw entities");

        copyPreTranslucentDepth();

        profiler.swap("translucent terrain");

        // TODO: Prevent these calls from scheduling translucent sorting...
        // It doesn't matter a ton, since this just means that they won't be sorted in the normal rendering pass.
        // Just something to watch out for, however...
        if (shouldRenderTranslucent) {
            levelRenderer.invokeRenderLayer(RenderLayer.TRANSLUCENT, tickDelta, 2, client.player);
        }

        debugStringTerrain = ((WorldRenderer) levelRenderer).getChunksDebugString();

        profiler.swap("generate mipmaps");

        samplingConfigurer.generateMipmaps();

        profiler.swap("restore gl state");

        restoreGlState();

        if (levelRenderer instanceof CullingDataCache) {
            ((CullingDataCache) levelRenderer).aurum$restoreState();
        }

        profiler.swap("shadowcomp");

        if (compositeRenderer != null) {
            compositeRenderer.renderAll();
        }

        profiler.pop();
        profiler.swap("updatechunks");
    }


    public void addDebugText(List<String> messages) {
        messages.add("[" + Aurum.MODNAME + "] Shadow Maps: " + debugStringOverall);
        messages.add("[" + Aurum.MODNAME + "] Shadow Distance Terrain: " + terrainFrustumHolder.getDistanceInfo() + " Entity: " + entityFrustumHolder.getDistanceInfo());
        messages.add("[" + Aurum.MODNAME + "] Shadow Culling Terrain: " + terrainFrustumHolder.getCullingInfo() + " Entity: " + entityFrustumHolder.getCullingInfo());
        messages.add("[" + Aurum.MODNAME + "] Shadow Terrain: " + debugStringTerrain
                + (shouldRenderTerrain ? "" : " (no terrain) ") + (shouldRenderTranslucent ? "" : "(no translucent)"));
        messages.add("[" + Aurum.MODNAME + "] Shadow Entities: " + getEntitiesDebugString());
        messages.add("[" + Aurum.MODNAME + "] Shadow Block Entities: " + getBlockEntitiesDebugString());
    }

    private String getEntitiesDebugString() {
        return (shouldRenderEntities || shouldRenderPlayer) ? (renderedShadowEntities + "/" + MinecraftClient.getInstance().world.loadedEntities.size()) : "disabled by pack";
    }

    private String getBlockEntitiesDebugString() {
        return shouldRenderBlockEntities ? (renderedShadowBlockEntities + "/" + MinecraftClient.getInstance().world.blockEntities.size()) : "disabled by pack";
    }

}
