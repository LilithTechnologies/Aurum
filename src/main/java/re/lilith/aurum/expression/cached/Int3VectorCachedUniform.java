package re.lilith.aurum.expression.cached;

import org.joml.Vector3i;
import org.joml.Vector3ic;
import re.lilith.aurum.expression.VectorType;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.uniform.UniformUpdateFrequency;

import java.util.function.Supplier;

public class Int3VectorCachedUniform extends VectorCachedUniform<Vector3ic> {

    public Int3VectorCachedUniform(String name, UniformUpdateFrequency updateFrequency, Supplier<Vector3ic> supplier) {
        super(name, updateFrequency, new Vector3i(), supplier);
    }

    @Override
    protected void setFrom(Vector3ic other) {
        ((Vector3i) this.cached).set(other);
    }

    @Override
    public void push(int location) {
        AurumRenderSystem.uniform3i(location, this.cached.x(), this.cached.y(), this.cached.z());
    }

    @Override
    public VectorType getType() {
        return VectorType.I_VEC3;
    }
}
