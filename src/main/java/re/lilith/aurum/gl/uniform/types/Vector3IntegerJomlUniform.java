package re.lilith.aurum.gl.uniform.types;

import org.joml.Vector3i;
import org.joml.Vector3ic;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.state.ValueUpdateNotifier;
import re.lilith.aurum.gl.uniform.Uniform;

import java.util.function.Supplier;

public class Vector3IntegerJomlUniform extends Uniform {
    private Vector3i cachedValue;
    private final Supplier<Vector3ic> value;

    public Vector3IntegerJomlUniform(int location, Supplier<Vector3ic> value) {
        this(location, value, null);
    }

    public Vector3IntegerJomlUniform(int location, Supplier<Vector3ic> value, ValueUpdateNotifier notifier) {
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
        Vector3ic newValue = value.get();

        if (!newValue.equals(cachedValue)) {
            cachedValue = new Vector3i(newValue);
            AurumRenderSystem.uniform3i(this.location, newValue.x(), newValue.y(), newValue.z());
        }
    }
}
