package re.lilith.aurum.expression;

import kroppeb.stareval.expression.Expression;
import kroppeb.stareval.function.AbstractTypedFunction;
import kroppeb.stareval.function.FunctionContext;
import kroppeb.stareval.function.FunctionReturn;
import kroppeb.stareval.function.Type;
import kroppeb.stareval.function.TypedFunction.Parameter;


final class ConditionalFunctions {
    private ConditionalFunctions() {
    }

    static void register() {
        {
            // IF
            // if(boolean, primitive, primitive) -> primitive
            // if(boolean, xvec, xvec) -> xvec
            // TODO: REDO: if(bvec, xvec, xvec) -> xvec
            // TODO: optifine requires vararg
            for (Type.Primitive type : Type.AllPrimitives) {
                AurumFunctions.add("if", new AbstractTypedFunction(type, new Type[]{Type.Boolean, type, type}) {
                    @Override
                    public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                        params[0].evaluateTo(context, functionReturn);

                        params[
                                functionReturn.booleanReturn ? 1 : 2
                                ].evaluateTo(context, functionReturn);

                    }
                });
            }

            for (Type type : VectorType.AllVectorTypes) {
                AurumFunctions.add("if", new AbstractTypedFunction(type, new Type[]{Type.Boolean, type, type}) {
                    @Override
                    public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                        params[0].evaluateTo(context, functionReturn);

                        params[
                                functionReturn.booleanReturn ? 1 : 2
                                ].evaluateTo(context, functionReturn);

                    }
                });
            }

            {
                // FAKE vararg
                for (int length = 2; length <= 16; length++) {
                    for (Type.Primitive type : Type.AllPrimitives) {
                        Type[] params = new Type[length * 2 + 1];
                        for (int i = 0; i < length * 2; i += 2) {
                            params[i] = Type.Boolean;
                            params[i + 1] = type;
                        }
                        params[length * 2] = type;
                        int finalLength = length * 2;
                        AurumFunctions.add("if", new AbstractTypedFunction(type, params) {
                            @Override
                            public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                                for (int i = 0; i < finalLength; i += 2) {
                                    params[i].evaluateTo(context, functionReturn);
                                    if (functionReturn.booleanReturn) {
                                        params[i + 1].evaluateTo(context, functionReturn);
                                        return;
                                    }
                                    params[finalLength].evaluateTo(context, functionReturn);
                                }
                            }
                        });
                    }
                }
            }
        }
        {
            // smooth

            // smooth(target)
            AurumFunctions.builder.addDynamicFunction("smooth", Type.Float, () ->
                    new AbstractTypedFunction(
                            Type.Float,
                            new Parameter[]{
                                    new Parameter(Type.Float, false), // target
                            },
                            0,
                            false
                    ) {
                        private final SmoothFloat smoothFloat = new SmoothFloat();

                        @Override
                        public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                            params[0].evaluateTo(context, functionReturn);
                            float target = functionReturn.floatReturn;
                            functionReturn.floatReturn = smoothFloat.updateAndGet(
                                    target,
                                    1,
                                    1
                            );
                        }
                    });

            // smooth(id, target)
            AurumFunctions.builder.addDynamicFunction("smooth", Type.Float, () ->
                    new AbstractTypedFunction(
                            Type.Float,
                            new Parameter[]{
                                    new Parameter(Type.Float, true), // id for backward compat with optifine
                                    new Parameter(Type.Float, false), // target
                            },
                            1,
                            false
                    ) {
                        private final SmoothFloat smoothFloat = new SmoothFloat();

                        @Override
                        public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                            params[1].evaluateTo(context, functionReturn);
                            float target = functionReturn.floatReturn;
                            functionReturn.floatReturn = smoothFloat.updateAndGet(
                                    target,
                                    1,
                                    1
                            );
                        }
                    });

            // smooth(target, fadeTime)
            AurumFunctions.builder.addDynamicFunction("smooth", Type.Float, () ->
                    new AbstractTypedFunction(
                            Type.Float,
                            new Parameter[]{
                                    new Parameter(Type.Float, false), // target
                                    new Parameter(Type.Float, false), // fadeTime
                            },
                            0,
                            false
                    ) {
                        private final SmoothFloat smoothFloat = new SmoothFloat();

                        @Override
                        public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                            params[0].evaluateTo(context, functionReturn);
                            float target = functionReturn.floatReturn;

                            params[1].evaluateTo(context, functionReturn);
                            float fadeTime = functionReturn.floatReturn;

                            functionReturn.floatReturn = smoothFloat.updateAndGet(
                                    target,
                                    fadeTime,
                                    fadeTime
                            );
                        }
                    });

            // smooth(id, target, fadeTime)
            AurumFunctions.builder.addDynamicFunction("smooth", Type.Float, () ->
                    new AbstractTypedFunction(
                            Type.Float,
                            new Parameter[]{
                                    new Parameter(Type.Float, true), // id for backward compat with optifine
                                    new Parameter(Type.Float, false), // target
                                    new Parameter(Type.Float, false), // fadeTime
                            },
                            1,
                            false
                    ) {
                        private final SmoothFloat smoothFloat = new SmoothFloat();

                        @Override
                        public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                            params[1].evaluateTo(context, functionReturn);
                            float target = functionReturn.floatReturn;

                            params[2].evaluateTo(context, functionReturn);
                            float fadeTime = functionReturn.floatReturn;

                            functionReturn.floatReturn = smoothFloat.updateAndGet(
                                    target,
                                    fadeTime,
                                    fadeTime
                            );
                        }
                    });

            // smooth(target, fadeUpTime, fadeDownTime)
            AurumFunctions.builder.addDynamicFunction("smooth", Type.Float, () ->
                    new AbstractTypedFunction(
                            Type.Float,
                            new Parameter[]{
                                    new Parameter(Type.Float, false), // target
                                    new Parameter(Type.Float, false), // fadeUpTime
                                    new Parameter(Type.Float, false), // fadeDownTime
                            },
                            0,
                            false
                    ) {
                        private final SmoothFloat smoothFloat = new SmoothFloat();

                        @Override
                        public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                            params[0].evaluateTo(context, functionReturn);
                            float target = functionReturn.floatReturn;

                            params[1].evaluateTo(context, functionReturn);
                            float fadeUpTime = functionReturn.floatReturn;
                            params[2].evaluateTo(context, functionReturn);
                            float fadeDownTime = functionReturn.floatReturn;

                            functionReturn.floatReturn = smoothFloat.updateAndGet(
                                    target,
                                    fadeUpTime,
                                    fadeDownTime
                            );
                        }
                    });

            // smooth(id, target, fadeUpTime, fadeDownTime)
            AurumFunctions.builder.addDynamicFunction("smooth", Type.Float, () ->
                    new AbstractTypedFunction(
                            Type.Float,
                            new Parameter[]{
                                    new Parameter(Type.Float, true), // id for backward compat with optifine
                                    new Parameter(Type.Float, false), // target
                                    new Parameter(Type.Float, false), // fadeUpTime
                                    new Parameter(Type.Float, false), // fadeDownTime
                            },
                            1,
                            false
                    ) {
                        private final SmoothFloat smoothFloat = new SmoothFloat();

                        @Override
                        public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                            params[1].evaluateTo(context, functionReturn);
                            float target = functionReturn.floatReturn;

                            params[2].evaluateTo(context, functionReturn);
                            float fadeUpTime = functionReturn.floatReturn;
                            params[3].evaluateTo(context, functionReturn);
                            float fadeDownTime = functionReturn.floatReturn;

                            functionReturn.floatReturn = smoothFloat.updateAndGet(
                                    target,
                                    fadeUpTime,
                                    fadeDownTime
                            );
                        }
                    });
        }
    }

}
