package re.lilith.aurum.expression;

import kroppeb.stareval.expression.Expression;
import kroppeb.stareval.function.*;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * #   sin(x)
 * #   cos(x)
 * #   asin(x)
 * #   acos(x)
 * #   tan(x)
 * #   atan(x)
 * #   atan2(y, x)
 * #   torad(deg)
 * #   todeg(rad)
 * #   min(x, y ,...)
 * #   max(x, y, ...)
 * #   clamp(x, min, max)                             Limits a value to be between min and max values
 * #   abs(x)
 * #   floor(x)
 * #   ceil(x)
 * #   exp(x)
 * #   frac(x)
 * #   log(x)
 * #   pow(x)
 * #   random()
 * #   round(x)
 * #   signum(x)
 * #   sqrt(x)
 * #   fmod(x, y)                                     Similar to Math.floorMod()
 * #   if(cond, val, [cond2, val2, ...], val_else)    Select a value based one or more conditions
 * #   smooth([id], val, [fadeInTime, [fadeOutTime]]) Smooths a variable with custom fade-in time.
 * #                                                  The "id" must be unique, if not specified it is generated automatically
 * #                                                  Default fade time is 1 sec.
 * <p>
 * # Boolean functions
 * #   between(x, min, max)                           Check if a value is between min and max values
 * #   equals(x, y, epsilon)                          Compare two float values with error margin
 * #   in(x, val1, val2, ...)                         Check if a value equals one of several values
 */
public class AurumFunctions {
    public static final FunctionResolver functions;
    static final FunctionResolver.Builder builder = new FunctionResolver.Builder();

    static {
        OperatorFunctions.registerOperators();
        TrigAndExponentialFunctions.register();
        CommonMathFunctions.register();
        ConditionalFunctions.register();
        OperatorFunctions.registerCastsAndComparisons();
        VectorFunctions.register();

        functions = builder.build();
    }

    static <T extends TypedFunction> void addVectorized(String name, T function) {
        if (function.getReturnType() instanceof Type.Primitive) {
            add(name, new VectorizedFunction(function, 2));
            add(name, new VectorizedFunction(function, 3));
            add(name, new VectorizedFunction(function, 4));
        } else {
            throw new IllegalArgumentException(name + " is not vectorizable");
        }
    }

    static <T extends TypedFunction> void addVectorizable(String name, T function) {
        add(name, function);
        addVectorized(name, function);
    }

    static <T extends TypedFunction> void addBooleanVectorizable(String name, T function) {
        assert function.getReturnType().equals(Type.Boolean);
        add(name, function);
        if (function.getReturnType() instanceof Type.Primitive) {
            add(name, new BooleanVectorizedFunction(function, 2));
            add(name, new BooleanVectorizedFunction(function, 3));
            add(name, new BooleanVectorizedFunction(function, 4));
        } else {
            throw new IllegalArgumentException(name + " is not vectorizable");
        }
    }

    static <T> void addUnaryOpJOML(String name, VectorType.JOMLVector<T> type, BiConsumer<T, T> function) {
        builder.add(name, new AbstractTypedFunction(
                type,
                new Type[]{type}
        ) {
            final private T vector = type.create();

            @SuppressWarnings("unchecked")
            @Override
            public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                params[0].evaluateTo(context, functionReturn);
                T a = (T) functionReturn.objectReturn;

                function.accept(a, this.vector);
                functionReturn.objectReturn = this.vector;
            }
        });
    }

    static <T> void addBinaryOpJOML(String name, VectorType.JOMLVector<T> type, TriConsumer<T, T, T> function) {
        builder.add(name, new AbstractTypedFunction(
                type,
                new Type[]{type, type}
        ) {
            final private T vector = type.create();

            @SuppressWarnings("unchecked")
            @Override
            public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                params[0].evaluateTo(context, functionReturn);
                T a = (T) functionReturn.objectReturn;

                params[1].evaluateTo(context, functionReturn);
                T b = (T) functionReturn.objectReturn;

                function.accept(a, b, this.vector);
                functionReturn.objectReturn = this.vector;
            }
        });
    }

    static <T> void addTernaryOpJOML(String name, VectorType.JOMLVector<T> type, QuadConsumer<T, T, T, T> function) {
        builder.add(name, new AbstractTypedFunction(
                type,
                new Type[]{type, type, type}
        ) {
            final private T vector = type.create();

            @SuppressWarnings("unchecked")
            @Override
            public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                params[0].evaluateTo(context, functionReturn);
                T a = (T) functionReturn.objectReturn;

                params[1].evaluateTo(context, functionReturn);
                T b = (T) functionReturn.objectReturn;

                params[2].evaluateTo(context, functionReturn);
                T c = (T) functionReturn.objectReturn;

                function.accept(a, b, c, this.vector);
                functionReturn.objectReturn = this.vector;
            }
        });
    }

    static <T> void addBinaryToBooleanOpJOML(
            String name,
            VectorType.JOMLVector<T> type,
            boolean inverted,
            ObjectObject2BooleanFunction<T, T> function) {
        builder.add(name, new AbstractTypedFunction(
                type,
                new Type[]{type, type}
        ) {
            @SuppressWarnings("unchecked")
            @Override
            public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                params[0].evaluateTo(context, functionReturn);
                T a = (T) functionReturn.objectReturn;

                params[1].evaluateTo(context, functionReturn);
                T b = (T) functionReturn.objectReturn;

                functionReturn.objectReturn = function.apply(a, b) != inverted;
            }
        });
    }

    static <T extends TypedFunction> void add(String name, T function) {
        builder.add(name, function);
    }

    static void addCast(final String name, final Type from, final Type to, final Consumer<FunctionReturn> function) {
        add(name, new TypedFunction() {
            @Override
            public Type getReturnType() {
                return to;
            }

            @Override
            public Parameter[] getParameters() {
                return new Parameter[]{new Parameter(from)};
            }

            @Override
            public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                params[0].evaluateTo(context, functionReturn);
                function.accept(functionReturn);
            }
        });
    }

    static void addImplicitCast(final Type from, final Type to, final Consumer<FunctionReturn> function) {
        addCast("<cast>", from, to, function);
        addExplicitCast(from, to, function);
    }

    static void addExplicitCast(final Type from, final Type to, final Consumer<FunctionReturn> function) {
        addCast("to" + to.getClass().getSimpleName(), from, to, function);
    }

    static void main() {
        functions.logAllFunctions();
    }

    interface ObjectObject2BooleanFunction<T, U> {
        boolean apply(T t, U u);
    }

    interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }

    interface QuadConsumer<T, U, V, W> {
        void accept(T t, U u, V v, W w);
    }
}

