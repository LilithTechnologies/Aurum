package re.lilith.aurum.expression;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import kroppeb.stareval.function.Type;
import org.joml.*;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.expression.cached.*;
import re.lilith.aurum.gl.uniform.FloatSupplier;
import re.lilith.aurum.gl.uniform.UniformType;
import re.lilith.aurum.gl.uniform.UniformUpdateFrequency;
import re.lilith.aurum.gl.uniform.holder.UniformHolder;

import java.util.Collection;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class CustomUniformFixedInputUniformsHolder {
    final private ImmutableMap<String, CachedUniform> inputVariables;

    public CustomUniformFixedInputUniformsHolder(
            ImmutableMap<String, CachedUniform> inputVariables) {
        this.inputVariables = inputVariables;
    }

    public Type getType(String name) {
        CachedUniform uniform = this.inputVariables.get(name);
        if (uniform == null)
            return null;
        return uniform.getType();
    }

    public boolean containsKey(String name) {
        return this.inputVariables.containsKey(name);
    }

    public CachedUniform getUniform(String name) {
        return this.inputVariables.get(name);
    }

    public void updateAll() {
        for (CachedUniform value : this.inputVariables.values()) {
            value.update();
        }
    }

    public Collection<CachedUniform> getAll() {
        return this.inputVariables.values();
    }

    public static class Builder implements UniformHolder {
        final private Map<String, CachedUniform> inputVariables = new Object2ObjectOpenHashMap<>();

        private Builder put(String name, CachedUniform uniform) {
            if (inputVariables.containsKey(name)) {
                Aurum.LOGGER.warn("Duplicated fixed uniform supplied as inputs to the Custom uniform holder: " + name);
                return this;
            }
            inputVariables.put(name, uniform);
            return this;
        }

        @Override
        public Builder uniform1f(UniformUpdateFrequency updateFrequency, String name, FloatSupplier value) {
            return this.put(name, new FloatCachedUniform(name, updateFrequency, value));
        }

        @Override
        public Builder uniform1f(UniformUpdateFrequency updateFrequency, String name, IntSupplier value) {
            return this.put(name, new FloatCachedUniform(name, updateFrequency, value::getAsInt));
        }

        @Override
        public Builder uniform1f(UniformUpdateFrequency updateFrequency, String name, DoubleSupplier value) {
            return this.put(name, new FloatCachedUniform(name, updateFrequency, () -> (float) value.getAsDouble()));
        }

        @Override
        public Builder uniform1i(UniformUpdateFrequency updateFrequency, String name, IntSupplier value) {
            return this.put(name, new IntCachedUniform(name, updateFrequency, value));
        }

        @Override
        public Builder uniform1b(UniformUpdateFrequency updateFrequency, String name, BooleanSupplier value) {
            return this.put(name, new BooleanCachedUniform(name, updateFrequency, value));
        }

        @Override
        public Builder uniform2f(UniformUpdateFrequency updateFrequency, String name, Supplier<Vector2f> value) {
            return this.put(name, new Float2VectorCachedUniform(name, updateFrequency, value));
        }

        @Override
        public Builder uniform2i(UniformUpdateFrequency updateFrequency, String name, Supplier<org.joml.Vector2i> value) {
            return this.put(name, new Int2VectorCachedUniform(name, updateFrequency, value::get));
        }

        @Override
        public Builder uniform3f(UniformUpdateFrequency updateFrequency, String name, Supplier<Vector3f> value) {
            return this.put(name, new Float3VectorCachedUniform(name, updateFrequency, value::get));
        }

        @Override
        public Builder uniform3i(UniformUpdateFrequency updateFrequency, String name, Supplier<Vector3ic> value) {
            return this.put(name, new Int3VectorCachedUniform(name, updateFrequency, value));
        }

        @Override
        public Builder uniformVanilla3f(UniformUpdateFrequency updateFrequency, String name, Supplier<org.lwjgl.util.vector.Vector3f> value) {
            throw new RuntimeException("Exception from Custom Uniform UniformHolder implementation. This should never be reached, please send help");
        }

        @Override
        public Builder uniformTruncated3f(UniformUpdateFrequency updateFrequency, String name, Supplier<Vector4f> value) {
            return this.put(name, new Float3VectorCachedUniform(name, updateFrequency, () -> {
                Vector4f vec = value.get();
                return new Vector3f(vec.x, vec.y, vec.z);
            }));
        }

        @Override
        public Builder uniform3d(UniformUpdateFrequency updateFrequency, String name, Supplier<Vector3d> value) {
            return this.put(name, new Float3VectorCachedUniform(name, updateFrequency, () -> {
                Vector3d vec = value.get();
                return new Vector3f((float) vec.x, (float) vec.y, (float) vec.z);
            }));
        }

        @Override
        public Builder uniform4f(UniformUpdateFrequency updateFrequency, String name, Supplier<Vector4f> value) {
            return this.put(name, new Float4VectorCachedUniform(name, updateFrequency, () -> value.get()));
        }

        @Override
        public Builder uniformMatrix(UniformUpdateFrequency updateFrequency, String name, Supplier<org.lwjgl.util.vector.Matrix4f> value) {
            Aurum.LOGGER.warn("Vanilla matrix fixed uniforms are not supported by custom uniforms, ignoring: " + name);
            return this;
        }

        @Override
        public Builder uniformJomlMatrix(UniformUpdateFrequency updateFrequency, String name, Supplier<Matrix4f> value) {
            return this.put(name, new Float4MatrixCachedUniform(name, updateFrequency, () -> (Matrix4fc) value.get()));
        }

        @Override
        public Builder uniformMatrixFromArray(UniformUpdateFrequency updateFrequency, String name, Supplier<float[]> value) {
            Matrix4f held = new Matrix4f();

            return this.put(name, new Float4MatrixCachedUniform(name, updateFrequency, () -> {
                held.set(value.get());
                return held;
            }));
        }

        @Override
        public Builder externallyManagedUniform(String name, UniformType type) {
            return this;
        }

        public CustomUniformFixedInputUniformsHolder build() {
            return new CustomUniformFixedInputUniformsHolder(ImmutableMap.copyOf(this.inputVariables));
        }
    }
}
