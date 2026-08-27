package re.lilith.aurum.mixin.vertices;

import net.minecraft.client.render.VertexBuffer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.gbuffer.BlockRenderingSettings;
import re.lilith.aurum.vertices.AurumVertexFormats;

@Mixin(VertexBuffer.class)
public class MixinVertexBuffer {
    @Shadow
    @Final
    @Mutable
    private VertexFormat format;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void aurum$onInit(VertexFormat format, CallbackInfo ci) {
        if (BlockRenderingSettings.INSTANCE.shouldUseExtendedVertexFormat()) {
            // We have to fix the vertex format here, or else the vertex count will be calculated wrongly and too many
            // vertices will be drawn.
            //
            // Needless to say, that is not good if you don't like access violation crashes!

            if (format == VertexFormats.BLOCK) {
                this.format = AurumVertexFormats.TERRAIN;
            } else if (format == VertexFormats.ENTITY) {
                this.format = AurumVertexFormats.ENTITY;
            }
        }
    }
}
