package re.lilith.aurum.expression;

import kroppeb.stareval.expression.Expression;
import kroppeb.stareval.function.*;
import org.joml.*;


final class VectorFunctions {
    private VectorFunctions() {
    }

    static void register() {
        {
            for (Type.Primitive type : new Type.Primitive[]{Type.Boolean, Type.Int}) {
                for (int size = 2; size <= 4; size++) {
                    TypedFunction function = new VectorConstructor(type, size);
                    // TODO make it possible to do `vec3(vec2(0),0)`
                    AurumFunctions.add(
                            Character.toLowerCase(
                                    type.getClass().getSimpleName().charAt(0)
                            ) + "vec" + size, function);
                }
            }

            AurumFunctions.add("vec2", new AbstractTypedFunction(
                    VectorType.VEC2,
                    new Type[]{Type.Float, Type.Float}
            ) {
                @Override
                public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                    params[0].evaluateTo(context, functionReturn);
                    float x = functionReturn.floatReturn;

                    params[1].evaluateTo(context, functionReturn);
                    float y = functionReturn.floatReturn;

                    // TODO: this can't be cached atm. If we swap this to a function provider we could
                    functionReturn.objectReturn = new Vector2f(x, y);
                }
            });

            AurumFunctions.add("vec3", new AbstractTypedFunction(
                    VectorType.VEC3,
                    new Type[]{Type.Float, Type.Float, Type.Float}
            ) {
                @Override
                public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                    params[0].evaluateTo(context, functionReturn);
                    float x = functionReturn.floatReturn;

                    params[1].evaluateTo(context, functionReturn);
                    float y = functionReturn.floatReturn;

                    params[2].evaluateTo(context, functionReturn);
                    float z = functionReturn.floatReturn;

                    // TODO: this can't be cached atm. If we swap this to a function provider we could
                    functionReturn.objectReturn = new Vector3f(x, y, z);
                }
            });

            AurumFunctions.add("vec4", new AbstractTypedFunction(
                    VectorType.VEC4,
                    new Type[]{Type.Float, Type.Float, Type.Float, Type.Float}
            ) {
                @Override
                public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                    params[0].evaluateTo(context, functionReturn);
                    float x = functionReturn.floatReturn;

                    params[1].evaluateTo(context, functionReturn);
                    float y = functionReturn.floatReturn;

                    params[2].evaluateTo(context, functionReturn);
                    float z = functionReturn.floatReturn;

                    params[3].evaluateTo(context, functionReturn);
                    float w = functionReturn.floatReturn;

                    // TODO: this can't be cached atm. If we swap this to a function provider we could
                    functionReturn.objectReturn = new Vector4f(x, y, z, w);
                }
            });
        }

        // accessors
        {
            // is this the best way to do these?
            String[][] accessNames = new String[][]{
                    new String[]{"0", "r", "x", "s"},
                    new String[]{"1", "g", "y", "t"},
                    new String[]{"2", "b", "z", "p"},
                    new String[]{"3", "a", "w", "q"}
            };

            {
                // access$0
                for (String access : accessNames[0]) {
                    AurumFunctions.add("<access$" + access + ">", new AbstractTypedFunction(
                            Type.Float,
                            new Type[]{VectorType.VEC2}
                    ) {
                        @Override
                        public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                            params[0].evaluateTo(context, functionReturn);
                            functionReturn.floatReturn = ((Vector2f) functionReturn.objectReturn).x;
                        }
                    });

                    AurumFunctions.add("<access$" + access + ">", new AbstractTypedFunction(
                            Type.Int,
                            new Type[]{VectorType.I_VEC2}
                    ) {
                        @Override
                        public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                            params[0].evaluateTo(context, functionReturn);
                            functionReturn.intReturn = ((Vector2i) functionReturn.objectReturn).x;
                        }
                    });

                    AurumFunctions.add("<access$" + access + ">", new AbstractTypedFunction(
                            Type.Float,
                            new Type[]{VectorType.VEC3}
                    ) {
                        @Override
                        public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                            params[0].evaluateTo(context, functionReturn);
                            functionReturn.floatReturn = ((Vector3f) functionReturn.objectReturn).x;
                        }
                    });

                    AurumFunctions.add("<access$" + access + ">", new AbstractTypedFunction(
                            Type.Int,
                            new Type[]{VectorType.I_VEC3}
                    ) {
                        @Override
                        public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                            params[0].evaluateTo(context, functionReturn);
                            functionReturn.intReturn = ((Vector3i) functionReturn.objectReturn).x;
                        }
                    });

                    AurumFunctions.add("<access$" + access + ">", new AbstractTypedFunction(
                            Type.Float,
                            new Type[]{VectorType.VEC4}
                    ) {
                        @Override
                        public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                            params[0].evaluateTo(context, functionReturn);
                            functionReturn.floatReturn = ((Vector4f) functionReturn.objectReturn).x;
                        }
                    });

                    AurumFunctions.add("<access$" + access + ">", new AbstractTypedFunction(
                            Type.Int,
                            new Type[]{VectorType.I_VEC4}
                    ) {
                        @Override
                        public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                            params[0].evaluateTo(context, functionReturn);
                            functionReturn.intReturn = ((Vector4i) functionReturn.objectReturn).x;
                        }
                    });
                }
            }

            {
                // access$1
                for (String access : accessNames[1]) {
                    AurumFunctions.add("<access$" + access + ">", new AbstractTypedFunction(
                            Type.Float,
                            new Type[]{VectorType.VEC2}
                    ) {
                        @Override
                        public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                            params[0].evaluateTo(context, functionReturn);
                            functionReturn.floatReturn = ((Vector2f) functionReturn.objectReturn).y;
                        }
                    });

                    AurumFunctions.add("<access$" + access + ">", new AbstractTypedFunction(
                            Type.Int,
                            new Type[]{VectorType.I_VEC2}
                    ) {
                        @Override
                        public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                            params[0].evaluateTo(context, functionReturn);
                            functionReturn.intReturn = ((Vector2i) functionReturn.objectReturn).y;
                        }
                    });

                    AurumFunctions.add("<access$" + access + ">", new AbstractTypedFunction(
                            Type.Float,
                            new Type[]{VectorType.VEC3}
                    ) {
                        @Override
                        public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                            params[0].evaluateTo(context, functionReturn);
                            functionReturn.floatReturn = ((Vector3f) functionReturn.objectReturn).y;
                        }
                    });

                    AurumFunctions.add("<access$" + access + ">", new AbstractTypedFunction(
                            Type.Int,
                            new Type[]{VectorType.I_VEC3}
                    ) {
                        @Override
                        public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                            params[0].evaluateTo(context, functionReturn);
                            functionReturn.intReturn = ((Vector3i) functionReturn.objectReturn).y;
                        }
                    });

                    AurumFunctions.add("<access$" + access + ">", new AbstractTypedFunction(
                            Type.Float,
                            new Type[]{VectorType.VEC4}
                    ) {
                        @Override
                        public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                            params[0].evaluateTo(context, functionReturn);
                            functionReturn.floatReturn = ((Vector4f) functionReturn.objectReturn).y;
                        }
                    });

                    AurumFunctions.add("<access$" + access + ">", new AbstractTypedFunction(
                            Type.Int,
                            new Type[]{VectorType.I_VEC4}
                    ) {
                        @Override
                        public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                            params[0].evaluateTo(context, functionReturn);
                            functionReturn.intReturn = ((Vector4i) functionReturn.objectReturn).y;
                        }
                    });
                }
            }

            {
                // access$2
                for (String access : accessNames[2]) {
                    AurumFunctions.add("<access$" + access + ">", new AbstractTypedFunction(
                            Type.Float,
                            new Type[]{VectorType.VEC3}
                    ) {
                        @Override
                        public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                            params[0].evaluateTo(context, functionReturn);
                            functionReturn.floatReturn = ((Vector3f) functionReturn.objectReturn).z;
                        }
                    });

                    AurumFunctions.add("<access$" + access + ">", new AbstractTypedFunction(
                            Type.Int,
                            new Type[]{VectorType.I_VEC3}
                    ) {
                        @Override
                        public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                            params[0].evaluateTo(context, functionReturn);
                            functionReturn.intReturn = ((Vector3i) functionReturn.objectReturn).z;
                        }
                    });

                    AurumFunctions.add("<access$" + access + ">", new AbstractTypedFunction(
                            Type.Float,
                            new Type[]{VectorType.VEC4}
                    ) {
                        @Override
                        public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                            params[0].evaluateTo(context, functionReturn);
                            functionReturn.floatReturn = ((Vector4f) functionReturn.objectReturn).z;
                        }
                    });

                    AurumFunctions.add("<access$" + access + ">", new AbstractTypedFunction(
                            Type.Int,
                            new Type[]{VectorType.I_VEC4}
                    ) {
                        @Override
                        public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                            params[0].evaluateTo(context, functionReturn);
                            functionReturn.intReturn = ((Vector4i) functionReturn.objectReturn).z;
                        }
                    });
                }
            }

            {
                // access$3
                for (String access : accessNames[3]) {
                    AurumFunctions.add("<access$" + access + ">", new AbstractTypedFunction(
                            Type.Float,
                            new Type[]{VectorType.VEC4}
                    ) {
                        @Override
                        public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                            params[0].evaluateTo(context, functionReturn);
                            functionReturn.floatReturn = ((Vector4f) functionReturn.objectReturn).w;
                        }
                    });

                    AurumFunctions.add("<access$" + access + ">", new AbstractTypedFunction(
                            Type.Int,
                            new Type[]{VectorType.I_VEC4}
                    ) {
                        @Override
                        public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                            params[0].evaluateTo(context, functionReturn);
                            functionReturn.intReturn = ((Vector4i) functionReturn.objectReturn).w;
                        }
                    });
                }
            }

            {
                // matrix access
                for (int i = 0; i < 4; i++) {
                    for (String access : accessNames[i]) {
                        int finalI = i;
                        AurumFunctions.add("<access$" + access + ">", new AbstractTypedFunction(
                                VectorType.VEC4,
                                new Type[]{MatrixType.MAT4}
                        ) {
                            @Override
                            public void evaluateTo(Expression[] params, FunctionContext context, FunctionReturn functionReturn) {
                                params[0].evaluateTo(context, functionReturn);
                                functionReturn.objectReturn = ((Matrix4f) functionReturn.objectReturn).getColumn(finalI, new Vector4f());
                            }
                        });
                    }
                }
            }
        }
    }

}
