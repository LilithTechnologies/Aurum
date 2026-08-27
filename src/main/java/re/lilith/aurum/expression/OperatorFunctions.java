package re.lilith.aurum.expression;

import kroppeb.stareval.expression.Expression;
import kroppeb.stareval.function.*;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Arrays;

final class OperatorFunctions {
    private OperatorFunctions() {
    }

    static void registerOperators() {
        {
            // Unary ops
            {
                // negate
                AurumFunctions.<I2IFunction>addVectorizable("negate", (a) -> -a);
                AurumFunctions.<F2FFunction>add("negate", (a) -> -a);

                AurumFunctions.addUnaryOpJOML("negate", VectorType.VEC2, Vector2f::negate);
                AurumFunctions.addUnaryOpJOML("negate", VectorType.VEC3, Vector3f::negate);
                AurumFunctions.addUnaryOpJOML("negate", VectorType.VEC4, Vector4f::negate);
            }
        }
        {
            // binary ops
            {
                // add
                AurumFunctions.<II2IFunction>addVectorizable("add", Integer::sum);
                AurumFunctions.<FF2FFunction>add("add", Float::sum);

                AurumFunctions.addBinaryOpJOML("add", VectorType.VEC2, Vector2f::add);
                AurumFunctions.addBinaryOpJOML("add", VectorType.VEC3, Vector3f::add);
                AurumFunctions.addBinaryOpJOML("add", VectorType.VEC4, Vector4f::add);
            }

            {
                // subtract
                AurumFunctions.<II2IFunction>addVectorizable("subtract", (a, b) -> a - b);
                AurumFunctions.<FF2FFunction>add("subtract", (a, b) -> a - b);

                AurumFunctions.addBinaryOpJOML("subtract", VectorType.VEC2, Vector2f::sub);
                AurumFunctions.addBinaryOpJOML("subtract", VectorType.VEC3, Vector3f::sub);
                AurumFunctions.addBinaryOpJOML("subtract", VectorType.VEC4, Vector4f::sub);
            }

            {
                // multiply
                AurumFunctions.<II2IFunction>addVectorizable("multiply", (a, b) -> a * b);
                AurumFunctions.<FF2FFunction>add("multiply", (a, b) -> a * b);

                AurumFunctions.addBinaryOpJOML("multiply", VectorType.VEC2, Vector2f::mul);
                AurumFunctions.addBinaryOpJOML("multiply", VectorType.VEC3, Vector3f::mul);
                AurumFunctions.addBinaryOpJOML("multiply", VectorType.VEC4, Vector4f::mul);
            }

            {
                // divide
                AurumFunctions.<FF2FFunction>add("divide", (a, b) -> a / b);

                AurumFunctions.addBinaryOpJOML("divide", VectorType.VEC2, Vector2f::div);
                AurumFunctions.addBinaryOpJOML("divide", VectorType.VEC3, Vector3f::div);
                AurumFunctions.addBinaryOpJOML("divide", VectorType.VEC4, Vector4f::div);
            }

            {
                // remainder
                AurumFunctions.<II2IFunction>addVectorizable("remainder", (a, b) -> a % b);
                AurumFunctions.<FF2FFunction>add("remainder", (a, b) -> a % b);

                // AurumFunctions.addBinaryOpJOML("multiply", VectorType.VEC2, Vector2f::??);
                // AurumFunctions.addBinaryOpJOML("multiply", VectorType.VEC3, Vector3f::??);
                // AurumFunctions.addBinaryOpJOML("multiply", VectorType.VEC4, Vector4f::??);
            }
        }
        {
            AurumFunctions.<II2BFunction>addBooleanVectorizable("equals", (a, b) -> a == b);
            AurumFunctions.<FF2BFunction>add("equals", (a, b) -> a == b);

            AurumFunctions.addBinaryToBooleanOpJOML("equal", VectorType.VEC2, false, Vector2f::equals);
            AurumFunctions.addBinaryToBooleanOpJOML("equal", VectorType.VEC3, false, Vector3f::equals);
            AurumFunctions.addBinaryToBooleanOpJOML("equal", VectorType.VEC4, false, Vector4f::equals);

            AurumFunctions.<II2BFunction>addBooleanVectorizable("notEquals", (a, b) -> a != b);
            AurumFunctions.<FF2BFunction>add("notEquals", (a, b) -> a != b);

            AurumFunctions.addBinaryToBooleanOpJOML("equal", VectorType.VEC2, true, Vector2f::equals);
            AurumFunctions.addBinaryToBooleanOpJOML("equal", VectorType.VEC3, true, Vector3f::equals);
            AurumFunctions.addBinaryToBooleanOpJOML("equal", VectorType.VEC4, true, Vector4f::equals);

            AurumFunctions.<II2BFunction>add("lessThanOrEquals", (a, b) -> a <= b);
            AurumFunctions.<FF2BFunction>add("lessThanOrEquals", (a, b) -> a <= b);

            AurumFunctions.<II2BFunction>add("moreThanOrEquals", (a, b) -> a >= b);
            AurumFunctions.<FF2BFunction>add("moreThanOrEquals", (a, b) -> a >= b);

            AurumFunctions.<II2BFunction>add("lessThan", (a, b) -> a < b);
            AurumFunctions.<FF2BFunction>add("lessThan", (a, b) -> a < b);

            AurumFunctions.<II2BFunction>add("moreThan", (a, b) -> a > b);
            AurumFunctions.<FF2BFunction>add("moreThan", (a, b) -> a > b);
        }
        {

            AurumFunctions.<BB2BFunction>addVectorizable("equals", (a, b) -> a == b);
            AurumFunctions.<BB2BFunction>addVectorizable("notEquals", (a, b) -> a != b);
            AurumFunctions.<BB2BFunction>addVectorizable("and", (a, b) -> a && b);
            AurumFunctions.<BB2BFunction>addVectorizable("or", (a, b) -> a || b);
            AurumFunctions.<B2BFunction>addVectorizable("not", (a) -> !a);
        }
    }

    static void registerCastsAndComparisons() {
        {
            AurumFunctions.addImplicitCast(Type.Int, Type.Float, r -> r.floatReturn = r.intReturn);
            // this is actually done by round i think
            AurumFunctions.addExplicitCast(Type.Float, Type.Int, r -> r.intReturn = (int) r.floatReturn);
        }

        // boolean functions
        {
            AurumFunctions.<III2BFunction>add("between", (a, min, max) -> a >= min && a <= max);
            AurumFunctions.<FFF2BFunction>add("between", (a, min, max) -> a >= min && a <= max);

            AurumFunctions.<FFF2BFunction>add("equals", (a, b, epsilon) -> Math.abs(a - b) <= epsilon);

            // TODO: varargs
            // TODO also for other types
            {
                // FAKE vararg
                for (int length = 2; length <= 32; length++) {
                    Type[] params = new Type[length];
                    Arrays.fill(params, Type.Float);
                    int finalLength = length;
                    AurumFunctions.add("in", new AbstractTypedFunction(
                            Type.Boolean,
                            params
                    ) {
                        @Override
                        public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                            params[0].evaluateTo(context, functionReturn);
                            float value = functionReturn.floatReturn;
                            for (int i = 1; i < finalLength; i++) {
                                params[i].evaluateTo(context, functionReturn);
                                if (functionReturn.floatReturn == value) {
                                    functionReturn.booleanReturn = true;
                                    return;
                                }
                            }
                            functionReturn.booleanReturn = false;
                        }
                    });
                }
            }
        }
    }

}
