package re.lilith.aurum.gl.program;

public record GlUniform1iCall(int location, int value) {
    public int getLocation() {
        return location;
    }

    public int getValue() {
        return value;
    }
}
