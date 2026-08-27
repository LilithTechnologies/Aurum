package re.lilith.aurum.expression.cached;

import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import re.lilith.aurum.expression.MatrixType;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.uniform.UniformUpdateFrequency;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.function.Supplier;

public class Float3MatrixCachedUniform extends VectorCachedUniform<Matrix3fc> {
    final private FloatBuffer buffer = ByteBuffer.allocateDirect(9 << 2).order(ByteOrder.nativeOrder()).asFloatBuffer();

    public Float3MatrixCachedUniform(String name, UniformUpdateFrequency updateFrequency, Supplier<Matrix3fc> supplier) {
        super(name, updateFrequency, new Matrix3f(), supplier);
    }

    @Override
    protected void setFrom(Matrix3fc other) {
        ((Matrix3f) this.cached).set(other);
    }

    @Override
    public void push(int location) {
        // `gets` the values from the matrix and put's them into a buffer
        this.cached.get(buffer);
        AurumRenderSystem.uniformMatrix3fv(location, false, buffer);
    }

    @Override
    public MatrixType<Matrix3f> getType() {
        return MatrixType.MAT3;
    }
}
