package re.lilith.aurum.mixin.vertices.blocks;

import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import re.lilith.aurum.celeritas.terrain.BlockContextHolder;

@Mixin(BlockModelRenderer.class)
public class MixinBlockModelRenderer_QuadFace {
    @Redirect(method = {"renderQuadsFlat", "renderQuadsSmooth", "renderQuads"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/model/BakedQuad;getVertexData()[I"))
    private int[] aurum$captureQuadFace(BakedQuad quad) {
        BlockContextHolder.get().setDoubleSidedQuad(quad.getFace() == null);
        return quad.getVertexData();
    }
}
