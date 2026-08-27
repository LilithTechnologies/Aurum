package re.lilith.aurum.gl.texture;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL30C;

import java.util.Optional;

public enum PixelFormat {
    RED(GL11C.GL_RED, false),
    RG(GL30C.GL_RG, false),
    RGB(GL11C.GL_RGB, false),
    BGR(GL12C.GL_BGR, false),
    RGBA(GL11C.GL_RGBA, false),
    BGRA(GL12C.GL_BGRA, false),
    RED_INTEGER(GL30C.GL_RED_INTEGER, true),
    RG_INTEGER(GL30C.GL_RG_INTEGER, true),
    RGB_INTEGER(GL30C.GL_RGB_INTEGER, true),
    BGR_INTEGER(GL30C.GL_BGR_INTEGER, true),
    RGBA_INTEGER(GL30C.GL_RGBA_INTEGER, true),
    BGRA_INTEGER(GL30C.GL_BGRA_INTEGER, true);

    private final int glFormat;
    private final boolean isInteger;

    PixelFormat(int glFormat, boolean isInteger) {
        this.glFormat = glFormat;
        this.isInteger = isInteger;
    }

    public static Optional<PixelFormat> fromString(String name) {
        try {
            return Optional.of(PixelFormat.valueOf(name));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public int getGlFormat() {
        return glFormat;
    }

    public boolean isInteger() {
        return isInteger;
    }
}
