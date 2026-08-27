package re.lilith.aurum.gl.uniform.types;

import org.lwjgl.util.vector.Vector3f;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.uniform.Uniform;

import java.util.function.Supplier;

public class VanillaVector3Uniform extends Uniform {
    private final Vector3f cachedValue;
    private final Supplier<Vector3f> value;

    public VanillaVector3Uniform(int location, Supplier<Vector3f> value) {
        super(location);

        this.cachedValue = new Vector3f();
        this.value = value;
    }

    @Override
    public void update() {
        Vector3f newValue = value.get();

        if (!newValue.equals(cachedValue)) {
            cachedValue.set(newValue.x, newValue.y, newValue.z);
            AurumRenderSystem.uniform3f(location, cachedValue.x, cachedValue.y, cachedValue.z);
        }
    }
}
