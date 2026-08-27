package re.lilith.aurum.mixin.vertices.blocks;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import re.lilith.aurum.celeritas.terrain.BlockContextHolder;
import re.lilith.aurum.gbuffer.BlockRenderingSettings;
import re.lilith.aurum.vertices.BlockSensitiveBufferBuilder;
import re.lilith.aurum.vertices.ExtendedDataHelper;

@Mixin(FluidRenderer.class)
public class MixinFluidRenderer {
    @Unique
    private final ThreadLocal<BlockSensitiveBufferBuilder> lastBufferBuilder = new ThreadLocal<>();

    @Unique
    private short resolveBlockId(BlockState state) {
        Object2IntMap<BlockState> blockStateIds = BlockRenderingSettings.INSTANCE.getBlockStateIds();

        if (blockStateIds == null) {
            return -1;
        }

        return (short) blockStateIds.getOrDefault(state, -1);
    }

    @Inject(method = "render", at = @At(value = "HEAD"))
    private void aurum$onRenderLiquid(BlockView world, BlockState state, BlockPos pos, BufferBuilder buffer, CallbackInfoReturnable<Boolean> cir) {
        short blockId = resolveBlockId(state);
        byte blockEmission = (byte) state.getBlock().getLightLevel();

        BlockContextHolder.get().setBlock(blockId, ExtendedDataHelper.FLUID_RENDER_TYPE, blockEmission,
                pos.getX() & 0xF, pos.getY() & 0xF, pos.getZ() & 0xF);

        if (buffer instanceof BlockSensitiveBufferBuilder blockSensitive) {
            lastBufferBuilder.set(blockSensitive);
            blockSensitive.aurum$beginBlock(blockId, ExtendedDataHelper.FLUID_RENDER_TYPE, blockEmission,
                    pos.getX() & 0xF, pos.getY() & 0xF, pos.getZ() & 0xF);
        }
    }

    @Inject(method = "render", at = @At(value = "RETURN"))
    private void aurum$finishRenderingLiquid(BlockView world, BlockState state, BlockPos pos, BufferBuilder buffer, CallbackInfoReturnable<Boolean> cir) {
        BlockContextHolder.get().reset();

        BlockSensitiveBufferBuilder blockSensitive = lastBufferBuilder.get();

        if (blockSensitive != null) {
            blockSensitive.aurum$endBlock();
            lastBufferBuilder.remove();
        }
    }
}
