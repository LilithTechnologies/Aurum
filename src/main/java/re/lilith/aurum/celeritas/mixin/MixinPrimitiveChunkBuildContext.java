package re.lilith.aurum.celeritas.mixin;

import dev.rdh.argentum.impl.render.terrain.compile.PrimitiveChunkBuildContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.util.QuadUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.celeritas.terrain.BlockContextHolder;
import re.lilith.aurum.vertices.ExtendingBufferBuilder;

@Mixin(PrimitiveChunkBuildContext.class)
public class MixinPrimitiveChunkBuildContext {
    @Inject(method = "beginSection", at = @At("HEAD"))
    private void aurum$resetRecordedQuads(CallbackInfo ci) {
        BlockContextHolder.get().clearRecordedQuads();
    }

    @Redirect(method = "outputQuads",
            at = @At(value = "INVOKE",
                    target = "Lorg/embeddedt/embeddium/impl/util/QuadUtil;calculateNormal([Lorg/embeddedt/embeddium/impl/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;)I"))
    private int aurum$replayBlockForQuad(ChunkVertexEncoder.Vertex[] quad) {
        BlockContextHolder.get().replayNextQuad();

        return QuadUtil.calculateNormal(quad);
    }

    @Redirect(method = "getBuffer",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/BufferBuilder;begin(ILnet/minecraft/client/render/VertexFormat;)V"))
    private void aurum$beginWithoutExtending(BufferBuilder buffer, int drawMode, VertexFormat format) {
        ((ExtendingBufferBuilder) buffer).aurum$beginWithoutExtending(drawMode, format);
    }
}
