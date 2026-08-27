package re.lilith.aurum.mixin.vertices;

import net.minecraft.client.render.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * Ensures that the correct state for the extended vertex format is set up when needed.
 */
@Mixin(VertexFormat.class)
public class MixinVertexFormat {
    @Unique
    private static final int NO_OFFSET = -1;

    @Redirect(method = "addElement", at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V"))
    private void aurum$storeUvOffset(List<Integer> uvIndices, int index, Object offset) {
        while (uvIndices.size() < index) {
            uvIndices.add(NO_OFFSET);
        }

        if (index < uvIndices.size()) {
            uvIndices.set(index, (Integer) offset);
        } else {
            uvIndices.add((Integer) offset);
        }
    }
}
