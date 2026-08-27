package re.lilith.aurum.gl.blending;

public record BufferBlendInformation(int index, BlendMode blendMode) {
    public BlendMode getBlendMode() {
        return blendMode;
    }

    public int getIndex() {
        return index;
    }
}
