package re.lilith.aurum.pipeline.pathways.clear;

import org.joml.Vector4f;

public record ClearPassInformation(Vector4f color, int width, int height) {
    public Vector4f getColor() {
        return color;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
