package re.lilith.aurum.gl.uniform.types;

import org.joml.Vector2f;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.uniform.Uniform;

import java.util.function.Supplier;

public class Vector2Uniform extends Uniform {
    private Vector2f cachedValue;
    private final Supplier<Vector2f> value;

    public Vector2Uniform(int location, Supplier<Vector2f> value) {
        super(location);

        this.cachedValue = null;
        this.value = value;

    }

    @Override
    public void update() {
        Vector2f newValue = value.get();

        if (!newValue.equals(cachedValue)) {
            cachedValue = newValue;
            AurumRenderSystem.uniform2f(this.location, newValue.x, newValue.y);
        }
    }
}
