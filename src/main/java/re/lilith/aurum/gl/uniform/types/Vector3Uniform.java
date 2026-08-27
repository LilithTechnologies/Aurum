package re.lilith.aurum.gl.uniform.types;

import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.uniform.Uniform;

import java.util.function.Supplier;

public class Vector3Uniform extends Uniform {
    private final Vector3f cachedValue;
    private final Supplier<Vector3f> value;

    public Vector3Uniform(int location, Supplier<Vector3f> value) {
        super(location);

        this.cachedValue = new Vector3f();
        this.value = value;
    }

    public static Vector3Uniform converted(int location, Supplier<Vector3d> value) {
        Vector3f held = new Vector3f();

        return new Vector3Uniform(location, () -> {
            Vector3d updated = value.get();

            held.set((float) updated.x, (float) updated.y, (float) updated.z);

            return held;
        });
    }

    public static Vector3Uniform truncated(int location, Supplier<Vector4f> value) {
        Vector3f held = new Vector3f();

        return new Vector3Uniform(location, () -> {
            Vector4f updated = value.get();

            held.set(updated.x(), updated.y(), updated.z());

            return held;
        });
    }

    @Override
    public void update() {
        Vector3f newValue = value.get();

        if (!newValue.equals(cachedValue)) {
            cachedValue.set(newValue.x(), newValue.y(), newValue.z());
            AurumRenderSystem.uniform3f(location, cachedValue.x(), cachedValue.y(), cachedValue.z());
        }
    }
}
