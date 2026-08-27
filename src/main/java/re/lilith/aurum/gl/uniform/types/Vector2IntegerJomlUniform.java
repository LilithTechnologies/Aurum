package re.lilith.aurum.gl.uniform.types;

import org.joml.Vector2i;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.state.ValueUpdateNotifier;
import re.lilith.aurum.gl.uniform.Uniform;

import java.util.function.Supplier;

public class Vector2IntegerJomlUniform extends Uniform {
    private Vector2i cachedValue;
    private final Supplier<Vector2i> value;

    public Vector2IntegerJomlUniform(int location, Supplier<Vector2i> value) {
        this(location, value, null);
    }

    public Vector2IntegerJomlUniform(int location, Supplier<Vector2i> value, ValueUpdateNotifier notifier) {
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
        Vector2i newValue = value.get();

        if (!newValue.equals(cachedValue)) {
            cachedValue = newValue;
            AurumRenderSystem.uniform2i(this.location, newValue.x, newValue.y);
        }
    }
}
