package re.lilith.aurum.celeritas.mixin;

import dev.rdh.argentum.impl.render.terrain.PrimitiveRenderSectionManager;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.AsyncOcclusionMode;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import re.lilith.aurum.celeritas.terrain.AurumChunkVertexType;
import re.lilith.aurum.gbuffer.BlockRenderingSettings;

@Mixin(PrimitiveRenderSectionManager.class)
public class MixinPrimitiveRenderSectionManager {
    @ModifyArg(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lorg/embeddedt/embeddium/impl/render/chunk/RenderSectionManager;<init>(Lorg/embeddedt/embeddium/impl/render/chunk/RenderPassConfiguration;Ljava/util/function/Supplier;Ljava/util/function/BiFunction;ILorg/embeddedt/embeddium/impl/gl/device/CommandList;IIIZ)V"),
            index = 8)
    private static boolean aurum$enableShadowRenderList(boolean hasShadowPass) {
        return true;
    }


    @Inject(method = "getAsyncOcclusionMode", at = @At("HEAD"), cancellable = true)
    private void aurum$forceSynchronousOcclusion(CallbackInfoReturnable<AsyncOcclusionMode> cir) {
        if (Boolean.getBoolean("aurum.asyncOcclusion.off")) {
            cir.setReturnValue(AsyncOcclusionMode.NONE);
        }
    }

    @ModifyVariable(method = "create", at = @At("HEAD"), argsOnly = true)
    private static ChunkVertexType aurum$useExtendedVertexFormat(ChunkVertexType vertexType) {
        boolean extended = BlockRenderingSettings.INSTANCE.shouldUseExtendedVertexFormat()
                && !Boolean.getBoolean("aurum.terrainFormat.vanilla");

        return extended ? AurumChunkVertexType.INSTANCE : vertexType;
    }
}
