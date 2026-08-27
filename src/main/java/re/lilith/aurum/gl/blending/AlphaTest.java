package re.lilith.aurum.gl.blending;

public record AlphaTest(AlphaTestFunction function, float reference) {
    public AlphaTestFunction getFunction() {
        return function;
    }

    public float getReference() {
        return reference;
    }
}
