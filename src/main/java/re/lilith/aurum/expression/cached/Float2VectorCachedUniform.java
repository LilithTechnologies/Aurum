package re.lilith.aurum.expression.cached;

import org.joml.Vector2f;
import re.lilith.aurum.expression.VectorType;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.uniform.UniformUpdateFrequency;

import java.util.function.Supplier;

public class Float2VectorCachedUniform extends VectorCachedUniform<Vector2f> {

    public Float2VectorCachedUniform(String name, UniformUpdateFrequency updateFrequency, Supplier<Vector2f> supplier) {
        super(name, updateFrequency, new Vector2f(), supplier);
    }

    @Override
    protected void setFrom(Vector2f other) {
        this.cached.set(other);
    }

    @Override
    public void push(int location) {
        AurumRenderSystem.uniform2f(location, this.cached.x, this.cached.y);
    }

    @Override
    public VectorType getType() {
        return VectorType.VEC2;
    }
}
