package re.lilith.aurum.gbuffer;

import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import org.jetbrains.annotations.Nullable;
import re.lilith.aurum.shaderpack.materialmap.NamespacedId;

import java.util.Map;

public class BlockRenderingSettings {
    public static final BlockRenderingSettings INSTANCE = new BlockRenderingSettings();

    private Object2IntMap<BlockState> blockStateIds;
    private Map<Block, RenderLayer> blockTypeIds;
    private Object2IntFunction<NamespacedId> entityIds;
    private float ambientOcclusionLevel;
    private boolean disableDirectionalShading;
    private boolean useSeparateAo;
    private boolean useExtendedVertexFormat;

    public BlockRenderingSettings() {
        blockStateIds = null;
        blockTypeIds = null;
        ambientOcclusionLevel = 1.0F;
        disableDirectionalShading = false;
        useSeparateAo = false;
        useExtendedVertexFormat = false;
    }

    @Nullable
    public Object2IntMap<BlockState> getBlockStateIds() {
        return blockStateIds;
    }

    @Nullable
    public Map<Block, RenderLayer> getBlockTypeIds() {
        return blockTypeIds;
    }

    // NB: entity ID lookup lives here alongside block state/type IDs for lack of a better shared home
    @Nullable
    public Object2IntFunction<NamespacedId> getEntityIds() {
        return entityIds;
    }

    public void setBlockStateIds(Object2IntMap<BlockState> blockStateIds) {
        if (this.blockStateIds != null && this.blockStateIds.equals(blockStateIds)) {
            return;
        }

        this.blockStateIds = blockStateIds;
    }

    public void setBlockTypeIds(Map<Block, RenderLayer> blockTypeIds) {
        if (this.blockTypeIds != null && this.blockTypeIds.equals(blockTypeIds)) {
            return;
        }

        this.blockTypeIds = blockTypeIds;
    }

    public void setEntityIds(Object2IntFunction<NamespacedId> entityIds) {
        // note: no reload needed, entities are rebuilt every frame.
        this.entityIds = entityIds;
    }

    public void setAmbientOcclusionLevel(float ambientOcclusionLevel) {
        if (ambientOcclusionLevel == this.ambientOcclusionLevel) {
            return;
        }

        this.ambientOcclusionLevel = ambientOcclusionLevel;
    }

    public void setDisableDirectionalShading(boolean disableDirectionalShading) {
        if (disableDirectionalShading == this.disableDirectionalShading) {
            return;
        }

        this.disableDirectionalShading = disableDirectionalShading;
    }

    public void setUseSeparateAo(boolean useSeparateAo) {
        if (useSeparateAo == this.useSeparateAo) {
            return;
        }

        this.useSeparateAo = useSeparateAo;
    }

    public boolean shouldUseExtendedVertexFormat() {
        return useExtendedVertexFormat;
    }

    public void setUseExtendedVertexFormat(boolean useExtendedVertexFormat) {
        if (useExtendedVertexFormat == this.useExtendedVertexFormat) {
            return;
        }

        this.useExtendedVertexFormat = useExtendedVertexFormat;
    }
}
