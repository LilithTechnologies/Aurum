package re.lilith.aurum.gl.uniform.types;

import org.joml.Vector4i;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.state.ValueUpdateNotifier;
import re.lilith.aurum.gl.uniform.Uniform;

import java.util.function.Supplier;

public class Vector4IntegerJomlUniform extends Uniform {
    private Vector4i cachedValue;
    private final Supplier<Vector4i> value;

    public Vector4IntegerJomlUniform(int location, Supplier<Vector4i> value, ValueUpdateNotifier notifier) {
        super(location, notifier);

        this.cachedValue = null;
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
        Vector4i newValue = value.get();

        if (!newValue.equals(cachedValue)) {
            cachedValue = newValue;
            AurumRenderSystem.uniform4i(this.location, newValue.x, newValue.y, newValue.z, newValue.w);
        }
    }
}
