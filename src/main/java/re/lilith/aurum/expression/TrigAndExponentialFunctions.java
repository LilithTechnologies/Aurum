package re.lilith.aurum.expression;

import kroppeb.stareval.function.F2FFunction;
import kroppeb.stareval.function.FF2FFunction;


final class TrigAndExponentialFunctions {
    private TrigAndExponentialFunctions() {
    }

    static void register() {
        {
            // Angle & Trigonometry Functions

            // optifine
            AurumFunctions.<F2FFunction>add("torad", (a) -> (float) Math.toRadians(a));
            AurumFunctions.<F2FFunction>add("todeg", (a) -> (float) Math.toDegrees(a));

            AurumFunctions.<F2FFunction>add("radians", (a) -> (float) Math.toRadians(a));
            AurumFunctions.<F2FFunction>add("degrees", (a) -> (float) Math.toDegrees(a));


            AurumFunctions.<F2FFunction>add("sin", (a) -> (float) Math.sin(a));
            AurumFunctions.<F2FFunction>add("cos", (a) -> (float) Math.cos(a));
            AurumFunctions.<F2FFunction>add("tan", (a) -> (float) Math.tan(a));
            AurumFunctions.<F2FFunction>add("asin", (a) -> (float) Math.asin(a));
            AurumFunctions.<F2FFunction>add("acos", (a) -> (float) Math.acos(a));
            AurumFunctions.<F2FFunction>add("atan", (a) -> (float) Math.atan(a));
            AurumFunctions.<FF2FFunction>add("atan", (y, x) -> (float) Math.atan2(y, x));
            // optifine
            AurumFunctions.<FF2FFunction>add("atan2", (y, x) -> (float) Math.atan2(y, x));
        }
        {
            // Exponential Functions
            AurumFunctions.<FF2FFunction>add("pow", (a, b) -> (float) Math.pow(a, b));
            AurumFunctions.<F2FFunction>add("exp", (a) -> (float) Math.exp(a));
            AurumFunctions.<F2FFunction>add("log", (a) -> (float) Math.log(a));
            // java does not have built ins: https://bugs.java.com/bugdatabase/view_bug.do?bug_id=4851627
            AurumFunctions.<F2FFunction>add("exp2", (a) -> (float) Math.pow(2, a));
            AurumFunctions.<F2FFunction>add("log2", (a) -> (float) (Math.log(a) / Math.log(2)));

            AurumFunctions.<F2FFunction>add("sqrt", (a) -> (float) Math.sqrt(a));
            // AurumFunctions.<F2FFunction>addVectorizable("inversesqrt", (a) -> (float) Math.(a));

            // optifine
            AurumFunctions.<F2FFunction>add("log10", (a) -> (float) Math.log10(a));

            // TODO the base may be static so doing `log(2, x)` would be slower than `log(x)/log(2)`
            AurumFunctions.<FF2FFunction>add("log",
                    (base, value) -> (float) (Math.log(value) / Math.log(base)));

            // cause I want consistency
            AurumFunctions.<F2FFunction>add("exp10", (a) -> (float) Math.pow(10, a));

        }
    }

}
