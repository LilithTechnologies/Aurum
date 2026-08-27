package re.lilith.aurum.expression.cached;

import kroppeb.stareval.expression.Expression;
import kroppeb.stareval.expression.VariableExpression;
import kroppeb.stareval.function.FunctionContext;
import kroppeb.stareval.function.FunctionReturn;
import kroppeb.stareval.function.Type;
import org.joml.*;
import re.lilith.aurum.expression.MatrixType;
import re.lilith.aurum.expression.VectorType;
import re.lilith.aurum.gl.uniform.UniformUpdateFrequency;

public abstract class CachedUniform implements VariableExpression {
    private final String name;
    private final UniformUpdateFrequency updateFrequency;
    private int valueGen;

    public CachedUniform(String name, UniformUpdateFrequency updateFrequency) {
        this.name = name;
        this.updateFrequency = updateFrequency;
    }

    static public CachedUniform forExpression(String name, Type type, Expression expression, FunctionContext context) {
        final FunctionReturn held = new FunctionReturn();
        final UniformUpdateFrequency frequency = UniformUpdateFrequency.CUSTOM;
        if (type.equals(Type.Boolean)) {
            return new BooleanCachedUniform(name, frequency, () -> {
                expression.evaluateTo(context, held);
                return held.booleanReturn;
            });
        } else if (type.equals(Type.Int)) {
            return new IntCachedUniform(name, frequency, () -> {
                expression.evaluateTo(context, held);
                return held.intReturn;
            });
        } else if (type.equals(Type.Float)) {
            return new FloatCachedUniform(name, frequency, () -> {
                expression.evaluateTo(context, held);
                return held.floatReturn;
            });
        } else if (type.equals(VectorType.VEC2)) {
            return new Float2VectorCachedUniform(name, frequency, () -> {
                expression.evaluateTo(context, held);
                return (Vector2f) held.objectReturn;
            });
        } else if (type.equals(VectorType.VEC3)) {
            return new Float3VectorCachedUniform(name, frequency, () -> {
                expression.evaluateTo(context, held);
                return (Vector3f) held.objectReturn;
            });
        } else if (type.equals(VectorType.VEC4)) {
            return new Float4VectorCachedUniform(name, frequency, () -> {
                expression.evaluateTo(context, held);
                return (Vector4f) held.objectReturn;
            });
        } else if (type.equals(MatrixType.MAT3)) {
            return new Float3MatrixCachedUniform(name, frequency, () -> {
                expression.evaluateTo(context, held);
                return (Matrix3f) held.objectReturn;
            });
        } else if (type.equals(MatrixType.MAT4)) {
            return new Float4MatrixCachedUniform(name, frequency, () -> {
                expression.evaluateTo(context, held);
                return (Matrix4f) held.objectReturn;
            });
        } else {
            throw new IllegalArgumentException("Custom uniforms of type: " + type + " are currently not supported");
        }
    }

    public void update() {
        if (doUpdate()) valueGen++;
    }

    public int valueGen() {
        return valueGen;
    }

    protected abstract boolean doUpdate();

    public abstract void push(int location);

    @Override
    public void evaluateTo(FunctionContext context, FunctionReturn functionReturn) {
        this.writeTo(functionReturn);
    }

    public abstract void writeTo(FunctionReturn functionReturn);

    public abstract Type getType();

    public String getName() {
        return name;
    }

    public UniformUpdateFrequency getUpdateFrequency() {
        return updateFrequency;
    }
}
