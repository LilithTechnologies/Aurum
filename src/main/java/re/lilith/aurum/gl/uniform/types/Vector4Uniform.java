package re.lilith.aurum.gl.uniform.types;

import org.joml.Vector4f;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.state.ValueUpdateNotifier;
import re.lilith.aurum.gl.uniform.Uniform;

import java.util.function.Supplier;

public class Vector4Uniform extends Uniform {
    private final Vector4f cachedValue;
    private final Supplier<Vector4f> value;

    public Vector4Uniform(int location, Supplier<Vector4f> value) {
        this(location, value, null);
    }

    public Vector4Uniform(int location, Supplier<Vector4f> value, ValueUpdateNotifier notifier) {
        super(location, notifier);

        this.cachedValue = new Vector4f();
        this.value = value;
    }

    @Override
    public void update() {
        updateValue();

        if (notifier != null) {
            notifier.setListener(this::updateValue);
        }
    }

    private void updateValue() {
        Vector4f newValue = value.get();

        if (!newValue.equals(cachedValue)) {
            cachedValue.set(newValue.x(), newValue.y(), newValue.z(), newValue.w());
            AurumRenderSystem.uniform4f(location, cachedValue.x(), cachedValue.y(), cachedValue.z(), cachedValue.w());
        }
    }
}
