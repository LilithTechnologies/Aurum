package re.lilith.aurum.pipeline;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.gl.Framebuffer;
import org.jetbrains.annotations.Nullable;
import re.lilith.aurum.gbuffer.BlockMaterialMapping;
import re.lilith.aurum.gbuffer.BlockRenderingSettings;
import re.lilith.aurum.gbuffer.matching.RenderCondition;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.buffer.ShaderStorageBufferHolder;
import re.lilith.aurum.gl.buffer.ShaderStorageInfo;
import re.lilith.aurum.gl.image.GlImage;
import re.lilith.aurum.gl.image.ImageInformation;
import re.lilith.aurum.gl.texture.DepthBufferFormat;
import re.lilith.aurum.shaderpack.PackDirectives;
import re.lilith.aurum.shaderpack.PackShadowDirectives;
import re.lilith.aurum.shaderpack.program.ProgramId;
import re.lilith.aurum.shaderpack.program.ProgramSet;
import re.lilith.aurum.targets.depth.DepthAttachedFramebuffer;
import re.lilith.aurum.targets.render.RenderTargets;
import re.lilith.aurum.texture.TextureInfoCache;

import java.util.ArrayList;
import java.util.List;

public final class PipelineConstruction {
    private PipelineConstruction() {
    }

    public static ProgramId[] createProgramIdTable() {
        ProgramId[] ids = new ProgramId[]{
                ProgramId.Basic, ProgramId.Textured, ProgramId.TexturedLit,
                ProgramId.SkyBasic, ProgramId.SkyTextured, ProgramId.SkyTextured,
                null, null, ProgramId.Terrain,
                null, null, ProgramId.Water,
                null, ProgramId.Clouds, ProgramId.Clouds,
                null, ProgramId.DamagedBlock, ProgramId.DamagedBlock,
                ProgramId.Block, ProgramId.Block, ProgramId.Block,
                ProgramId.BlockTrans, ProgramId.BlockTrans, ProgramId.BlockTrans,
                ProgramId.BeaconBeam, ProgramId.BeaconBeam, ProgramId.BeaconBeam,
                ProgramId.Entities, ProgramId.Entities, ProgramId.Entities,
                ProgramId.EntitiesTrans, ProgramId.EntitiesTrans, ProgramId.EntitiesTrans,
                null, ProgramId.ArmorGlint, ProgramId.ArmorGlint,
                null, ProgramId.SpiderEyes, ProgramId.SpiderEyes,
                ProgramId.Hand, ProgramId.Hand, ProgramId.Hand,
                ProgramId.HandWater, ProgramId.HandWater, ProgramId.HandWater,
                null, null, ProgramId.Weather,
                // world border uses textured_lit even though it has no lightmap :/
                null, ProgramId.TexturedLit, ProgramId.TexturedLit,
                ProgramId.Shadow, ProgramId.Shadow, ProgramId.Shadow
        };

        if (ids.length != RenderCondition.values().length * 3) {
            throw new IllegalStateException("Program ID table length mismatch");
        }
        return ids;
    }

    public static RenderTargets createRenderTargets(Framebuffer mainTarget, PackDirectives directives) {
        int depthTextureId = ((DepthAttachedFramebuffer) mainTarget).getAurum$depthTextureId();
        int internalFormat = TextureInfoCache.INSTANCE.getInfo(depthTextureId).getInternalFormat();
        DepthBufferFormat depthBufferFormat = DepthBufferFormat.fromGlEnumOrDefault(internalFormat);

        return new RenderTargets(mainTarget.viewportWidth, mainTarget.viewportHeight, depthTextureId,
                ((DepthAttachedFramebuffer) mainTarget).aurum$getDepthBufferVersion(),
                depthBufferFormat, directives.getRenderTargetDirectives().getRenderTargetSettings(), directives);
    }

    @Nullable
    public static ShaderStorageBufferHolder createSsboHolder(Int2ObjectMap<ShaderStorageInfo> bufferObjectInfos, Framebuffer mainTarget) {
        if (!bufferObjectInfos.isEmpty() && AurumRenderSystem.supportsSSBO()) {
            return new ShaderStorageBufferHolder(bufferObjectInfos, mainTarget.viewportWidth, mainTarget.viewportHeight);
        }

        return null;
    }

    public record ImageLists(GlImage[] all, GlImage[] toClear) {
    }

    public static ImageLists buildCustomImages(ProgramSet programs, int viewportWidth, int viewportHeight) {
        List<GlImage> customImageList = new ArrayList<>();
        List<GlImage> clearList = new ArrayList<>();

        for (ImageInformation info : programs.getPack().getCustomImages().values()) {
            GlImage image = createCustomImage(viewportWidth, viewportHeight, info);
            customImageList.add(image);
            if (image.shouldClear()) {
                clearList.add(image);
            }
        }

        return new ImageLists(customImageList.toArray(new GlImage[0]), clearList.toArray(new GlImage[0]));
    }

    private static GlImage createCustomImage(int viewportWidth, int viewportHeight, ImageInformation info) {
        if (info.isRelative()) {
            return new GlImage.Relative(info.name(), info.samplerName(), info.format(), info.internalTextureFormat(),
                    info.type(), info.clear(), info.relativeWidth(), info.relativeHeight(), viewportWidth, viewportHeight);
        }

        return new GlImage(info.name(), info.samplerName(), info.target(), info.format(), info.internalTextureFormat(),
                info.type(), info.clear(), info.width(), info.height(), info.depth());
    }

    public static @Nullable Integer computeForcedShadowRenderDistanceChunks(PackShadowDirectives shadowDirectives) {
        if (shadowDirectives.isDistanceRenderImplicit()) {
            return null;
        }

        if (shadowDirectives.getDistanceRenderMul() >= 0.0) {
            // add 15 and then divide by 16 to ensure we're rounding up
            return ((int) (shadowDirectives.getDistance() * shadowDirectives.getDistanceRenderMul()) + 15) / 16;
        }

        return -1;
    }

    public static void applyBlockRenderingSettings(ProgramSet programs, boolean disableDirectionalShading) {
        BlockRenderingSettings.INSTANCE.setBlockStateIds(
                BlockMaterialMapping.createBlockStateIdMap(programs.getPack().getIdMap().getBlockProperties()));
        BlockRenderingSettings.INSTANCE.setBlockTypeIds(BlockMaterialMapping.createBlockTypeMap(programs.getPack().getIdMap().getBlockRenderTypeMap()));

        BlockRenderingSettings.INSTANCE.setEntityIds(programs.getPack().getIdMap().getEntityIdMap());
        BlockRenderingSettings.INSTANCE.setAmbientOcclusionLevel(programs.getPackDirectives().getAmbientOcclusionLevel());
        BlockRenderingSettings.INSTANCE.setDisableDirectionalShading(disableDirectionalShading);
        BlockRenderingSettings.INSTANCE.setUseSeparateAo(programs.getPackDirectives().shouldUseSeparateAo());
        BlockRenderingSettings.INSTANCE.setUseExtendedVertexFormat(true);
    }
}
