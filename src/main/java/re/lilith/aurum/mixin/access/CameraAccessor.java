package re.lilith.aurum.mixin.access;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.FloatBuffer;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Accessor("PROJECTION_MATRIX")
    static FloatBuffer getProjectionMatrix() {
        throw new AssertionError("mixin");
    }

    @Accessor("MODEL_MATRIX")
    static FloatBuffer getModelMatrix() {
        throw new AssertionError("mixin");
    }
}
