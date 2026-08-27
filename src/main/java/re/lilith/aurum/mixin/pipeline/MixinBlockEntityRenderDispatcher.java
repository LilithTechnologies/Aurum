package re.lilith.aurum.mixin.pipeline;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.pipeline.WorldRenderingPhase;
import re.lilith.aurum.pipeline.WorldRenderingPipeline;

@Mixin(BlockEntityRenderDispatcher.class)
public class MixinBlockEntityRenderDispatcher {
    @Inject(method = "renderEntity(Lnet/minecraft/block/entity/BlockEntity;FI)V", at = @At("HEAD"))
    private void aurum$beginBlockEntity(BlockEntity blockEntity, float tickDelta, int destroyProgress, CallbackInfo ci) {
        aurum$setPhase(WorldRenderingPhase.BLOCK_ENTITIES);
    }

    @Inject(method = "renderEntity(Lnet/minecraft/block/entity/BlockEntity;FI)V", at = @At("RETURN"))
    private void aurum$endBlockEntity(BlockEntity blockEntity, float tickDelta, int destroyProgress, CallbackInfo ci) {
        aurum$setPhase(WorldRenderingPhase.NONE);
    }

    @Unique
    private static void aurum$setPhase(WorldRenderingPhase phase) {
        WorldRenderingPipeline pipeline = Aurum.getPipelineManager().getPipelineNullable();

        if (pipeline != null) {
            pipeline.setPhase(phase);
        }
    }
}
