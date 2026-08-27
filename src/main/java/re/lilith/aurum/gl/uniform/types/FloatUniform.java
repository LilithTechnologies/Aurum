package re.lilith.aurum.gl.uniform.types;

import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.state.ValueUpdateNotifier;
import re.lilith.aurum.gl.uniform.FloatSupplier;
import re.lilith.aurum.gl.uniform.Uniform;

public class FloatUniform extends Uniform {
    private float cachedValue;
    private final FloatSupplier value;

    public FloatUniform(int location, FloatSupplier value) {
        this(location, value, null);
    }

    public FloatUniform(int location, FloatSupplier value, ValueUpdateNotifier notifier) {
        super(location, notifier);

        this.cachedValue = 0;
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
        float newValue = value.getAsFloat();

        if (cachedValue != newValue) {
            cachedValue = newValue;
            AurumRenderSystem.uniform1f(location, newValue);
        }
    }
}
