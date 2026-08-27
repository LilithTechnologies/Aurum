package re.lilith.aurum.expression.cached;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import re.lilith.aurum.expression.VectorType;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.uniform.UniformUpdateFrequency;

import java.util.function.Supplier;

public class Float3VectorCachedUniform extends VectorCachedUniform<Vector3fc> {

    public Float3VectorCachedUniform(String name, UniformUpdateFrequency updateFrequency, Supplier<Vector3fc> supplier) {
        super(name, updateFrequency, new Vector3f(), supplier);
    }

    @Override
    protected void setFrom(Vector3fc other) {
        ((Vector3f) this.cached).set(other);
    }

    @Override
    public void push(int location) {
        AurumRenderSystem.uniform3f(location, this.cached.x(), this.cached.y(), this.cached.z());
    }

    @Override
    public VectorType getType() {
        return VectorType.VEC3;
    }
}
