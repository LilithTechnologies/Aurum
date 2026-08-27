package re.lilith.aurum.expression.cached;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import re.lilith.aurum.expression.MatrixType;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.uniform.UniformUpdateFrequency;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.function.Supplier;

public class Float4MatrixCachedUniform extends VectorCachedUniform<Matrix4fc> {
    final private FloatBuffer buffer = ByteBuffer.allocateDirect(16 << 2).order(ByteOrder.nativeOrder()).asFloatBuffer();

    public Float4MatrixCachedUniform(String name, UniformUpdateFrequency updateFrequency, Supplier<Matrix4fc> supplier) {
        super(name, updateFrequency, new Matrix4f(), supplier);
    }

    @Override
    protected void setFrom(Matrix4fc other) {
        ((Matrix4f) this.cached).set(other);
    }

    @Override
    public void push(int location) {
        // `gets` the values from the matrix and put's them into a buffer
        this.cached.get(buffer);
        AurumRenderSystem.uniformMatrix4fv(location, false, buffer);
    }

    @Override
    public MatrixType<Matrix4f> getType() {
        return MatrixType.MAT4;
    }
}
