package re.lilith.aurum.gl.uniform.types;

import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.state.ValueUpdateNotifier;
import re.lilith.aurum.gl.uniform.Uniform;

import java.util.function.IntSupplier;

public class IntUniform extends Uniform {
    private int cachedValue;
    private final IntSupplier value;

    public IntUniform(int location, IntSupplier value) {
        this(location, value, null);
    }

    public IntUniform(int location, IntSupplier value, ValueUpdateNotifier notifier) {
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
        int newValue = value.getAsInt();

        if (cachedValue != newValue) {
            cachedValue = newValue;
            AurumRenderSystem.uniform1i(location, newValue);
        }
    }
}
