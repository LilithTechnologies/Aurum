package re.lilith.aurum.gl.uniform.types;

import java.util.function.BooleanSupplier;

public class BooleanUniform extends IntUniform {
    public BooleanUniform(int location, BooleanSupplier value) {
        super(location, () -> value.getAsBoolean() ? 1 : 0);
    }
}
