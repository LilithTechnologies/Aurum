package re.lilith.aurum.expression;

import kroppeb.stareval.expression.Expression;
import kroppeb.stareval.function.*;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Arrays;
import java.util.Random;

final class CommonMathFunctions {
    private CommonMathFunctions() {
    }

    static void register() {
        {
            // Common Functions
            AurumFunctions.<I2IFunction>addVectorizable("abs", Math::abs);
            AurumFunctions.<F2FFunction>add("abs", Math::abs);

            AurumFunctions.addUnaryOpJOML("abs", VectorType.VEC2, Vector2f::absolute);
            AurumFunctions.addUnaryOpJOML("abs", VectorType.VEC3, Vector3f::absolute);
            AurumFunctions.addUnaryOpJOML("abs", VectorType.VEC4, Vector4f::absolute);


            AurumFunctions.<F2FFunction>add("sign", Math::signum);
            // AurumFunctions.addUnaryOpJOML("abs", VectorType.VEC2, Vector2f::??);
            // AurumFunctions.addUnaryOpJOML("abs", VectorType.VEC3, Vector3f::??);
            // AurumFunctions.addUnaryOpJOML("abs", VectorType.VEC4, Vector4f::??);

            // optifine
            AurumFunctions.<F2FFunction>add("signum", Math::signum);
            // AurumFunctions.addUnaryOpJOML("abs", VectorType.VEC2, Vector2f::??);
            // AurumFunctions.addUnaryOpJOML("abs", VectorType.VEC3, Vector3f::??);
            // AurumFunctions.addUnaryOpJOML("abs", VectorType.VEC4, Vector4f::??);

            // because my type checker can handle (float) -> float and (float) -> int,
            // floor doesn't require a cast to int, but does not cause issues if the float is too big and
            // casting to float and back would change the result

            AurumFunctions.<F2FFunction>add("floor", (a) -> (float) Math.floor(a));
            AurumFunctions.<F2IFunction>add("floor", (a) -> (int) Math.floor(a));

            AurumFunctions.addUnaryOpJOML("floor", VectorType.VEC2, Vector2f::floor);
            AurumFunctions.addUnaryOpJOML("floor", VectorType.VEC3, Vector3f::floor);
            AurumFunctions.addUnaryOpJOML("floor", VectorType.VEC4, Vector4f::floor);

            AurumFunctions.<F2FFunction>add("ceil", (a) -> (float) Math.ceil(a));
            AurumFunctions.<F2IFunction>add("ceil", (a) -> (int) Math.ceil(a));

            AurumFunctions.addUnaryOpJOML("ceil", VectorType.VEC2, Vector2f::ceil);
            AurumFunctions.addUnaryOpJOML("ceil", VectorType.VEC3, Vector3f::ceil);
            AurumFunctions.addUnaryOpJOML("ceil", VectorType.VEC4, Vector4f::ceil);

            AurumFunctions.<F2FFunction>add("frac", (a) -> (float) (a - Math.floor(a)));

            // AurumFunctions.addUnaryOpJOML("frac", VectorType.VEC2, Vector2f::??);
            // AurumFunctions.addUnaryOpJOML("frac", VectorType.VEC3, Vector3f::??);
            // AurumFunctions.addUnaryOpJOML("frac", VectorType.VEC4, Vector4f::??);

            // optifine
            // TODO: why does Math.round give an int?
            // AurumFunctions.<F2FFunction>addVectorizable("round", (a) -> (float) Math.round(a));
            // TODO maybe add round with a specifyable precission?
            // AurumFunctions.<F2IFunction>addVectorizable("round", (a) -> (int) Math.round(a));


            // mod is also already an operator

            // TODO: min and max require vararg for optifine compat
            // TODO: glsl has vecn min(vecn a, float b)
            AurumFunctions.<II2IFunction>addVectorizable("min", Math::min);
            AurumFunctions.<FF2FFunction>add("min", Math::min);

            AurumFunctions.addBinaryOpJOML("min", VectorType.VEC2, Vector2f::min);
            AurumFunctions.addBinaryOpJOML("min", VectorType.VEC3, Vector3f::min);
            AurumFunctions.addBinaryOpJOML("min", VectorType.VEC4, Vector4f::min);

            AurumFunctions.<II2IFunction>addVectorizable("max", Math::max);
            AurumFunctions.<FF2FFunction>add("max", Math::max);

            AurumFunctions.addBinaryOpJOML("max", VectorType.VEC2, Vector2f::max);
            AurumFunctions.addBinaryOpJOML("max", VectorType.VEC3, Vector3f::max);
            AurumFunctions.addBinaryOpJOML("max", VectorType.VEC4, Vector4f::max);

            {
                // Fake vararg
                for (int length = 3; length <= 16; length++) {
                    {
                        // min float
                        Type[] inputs = new Type[length];
                        Arrays.fill(inputs, Type.Float);
                        AurumFunctions.add("min", new AbstractTypedFunction(Type.Float, inputs) {
                            @Override
                            public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                                params[0].evaluateTo(context, functionReturn);
                                float min = functionReturn.floatReturn;
                                for (int i = 1; i < params.length; i++) {
                                    params[1].evaluateTo(context, functionReturn);
                                    min = Math.min(min, functionReturn.floatReturn);
                                }
                                functionReturn.floatReturn = min;
                            }
                        });
                    }
                    {
                        // max float
                        Type[] inputs = new Type[length];
                        Arrays.fill(inputs, Type.Float);
                        AurumFunctions.add("max", new AbstractTypedFunction(Type.Float, inputs) {
                            @Override
                            public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                                params[0].evaluateTo(context, functionReturn);
                                float max = functionReturn.floatReturn;
                                for (int i = 1; i < params.length; i++) {
                                    params[1].evaluateTo(context, functionReturn);
                                    max = Math.max(max, functionReturn.floatReturn);
                                }
                                functionReturn.floatReturn = max;
                            }
                        });
                    }
                    {
                        // min int
                        Type[] inputs = new Type[length];
                        Arrays.fill(inputs, Type.Int);
                        AurumFunctions.addVectorizable("min", new AbstractTypedFunction(Type.Int, inputs) {
                            @Override
                            public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                                params[0].evaluateTo(context, functionReturn);
                                int min = functionReturn.intReturn;
                                for (int i = 1; i < params.length; i++) {
                                    params[1].evaluateTo(context, functionReturn);
                                    min = Math.min(min, functionReturn.intReturn);
                                }
                                functionReturn.intReturn = min;
                            }
                        });
                    }
                    {
                        // max int
                        Type[] inputs = new Type[length];
                        Arrays.fill(inputs, Type.Int);
                        AurumFunctions.addVectorizable("max", new AbstractTypedFunction(Type.Int, inputs) {
                            @Override
                            public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                                params[0].evaluateTo(context, functionReturn);
                                int max = functionReturn.intReturn;
                                for (int i = 1; i < params.length; i++) {
                                    params[1].evaluateTo(context, functionReturn);
                                    max = Math.max(max, functionReturn.intReturn);
                                }
                                functionReturn.intReturn = max;
                            }
                        });
                    }
                }
            }

            // if max < min => undefined behaviour
            // TODO: glsl has vecn mix(vecn x, float min, float max)
            AurumFunctions.<III2IFunction>addVectorizable("clamp",
                    (val, min, max) -> Math.max(min, Math.min(max, val)));
            AurumFunctions.<FFF2FFunction>add("clamp",
                    (val, min, max) -> Math.max(min, Math.min(max, val)));
            AurumFunctions.addTernaryOpJOML("clamp", VectorType.VEC2, (val, min, max, dest) -> {
                val.min(max, dest);
                dest.max(min);
            });
            AurumFunctions.addTernaryOpJOML("clamp", VectorType.VEC3, (val, min, max, dest) -> {
                val.min(max, dest);
                dest.max(min);
            });
            AurumFunctions.addTernaryOpJOML("clamp", VectorType.VEC4, (val, min, max, dest) -> {
                val.min(max, dest);
                dest.max(min);
            });

            // TODO: glsl has vecn mix(vecn x, vecn y, float a)
            AurumFunctions.<FFF2FFunction>add("mix", (x, y, a) -> x + (y - x) * a);
            // TODO flaot vector lerp

            // TODO: glsl has vecn step(float edge, vecn x)
            AurumFunctions.<II2IFunction>addVectorizable("edge", (edge, x) -> (x < edge) ? 0 : 1);
            AurumFunctions.<FF2FFunction>add("edge", (edge, x) -> (x < edge) ? 0 : 1);
            // TODO float vector step
            // TODO: smooth step
        }
        {
            // Geometric Functions
            // TODO: Geometric Functions
        }
        {
            // Matrix Functions
            // TODO: Add matrices
        }
        {
            // Vector Relational Functions
            // TODO: These might clash with the operators
            // Although we can have multiple return values so
        }
        {
            // fmod
            AurumFunctions.<II2IFunction>addVectorizable("fmod", Math::floorMod);
            AurumFunctions.<FF2FFunction>add("fmod", (a, b) -> (a % b + b) % b);
        }
        {
            Random random = new Random();
            // randomInt(), randomInt(int bound), randomInt(int inclusiveMin, int exclusiveMax)
            AurumFunctions.<V2IFunction>addVectorizable("randomInt", random::nextInt);
            AurumFunctions.<I2IFunction>addVectorizable("randomInt", random::nextInt);
            AurumFunctions.<II2IFunction>addVectorizable("randomInt", (a, b) -> random.nextInt(b - a) + a);

            // random, random(float min, float max)
            AurumFunctions.<V2FFunction>add("random", random::nextFloat);
            AurumFunctions.<FF2FFunction>add("random", (min, max) ->
                    min + random.nextFloat() * (max - min));
        }
    }

}
