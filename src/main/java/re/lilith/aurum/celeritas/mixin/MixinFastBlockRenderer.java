package re.lilith.aurum.celeritas.mixin;

import dev.rdh.argentum.impl.render.terrain.compile.PrimitiveBuiltRenderSectionData;
import dev.rdh.argentum.impl.render.terrain.compile.pipeline.FastBlockRenderer;
import dev.rdh.argentum.impl.world.cloned.ChunkRenderContext;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.BlockPos;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.celeritas.terrain.BlockContextHolder;
import re.lilith.aurum.gbuffer.BlockRenderingSettings;
import re.lilith.aurum.vertices.ExtendedDataHelper;

@Mixin(FastBlockRenderer.class)
public class MixinFastBlockRenderer {
    @Inject(method = "render", at = @At("HEAD"))
    private void aurum$captureBlockContext(BlockState state, BlockPos pos, ChunkRenderContext world, RenderLayer layer,
                                           ChunkBuildBuffers buffers, PrimitiveBuiltRenderSectionData renderData,
                                           CallbackInfo ci) {
        Object2IntMap<BlockState> blockStateIds = BlockRenderingSettings.INSTANCE.getBlockStateIds();
        short blockId = blockStateIds == null ? -1 : (short) blockStateIds.getOrDefault(state, -1);

        BlockContextHolder.get().setBlock(
                blockId,
                ExtendedDataHelper.BLOCK_RENDER_TYPE,
                (byte) state.getBlock().getLightLevel(),
                pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
    }
}
