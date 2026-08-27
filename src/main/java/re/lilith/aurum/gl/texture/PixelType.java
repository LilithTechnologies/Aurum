package re.lilith.aurum.gl.texture;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL30C;

import java.util.Optional;

public enum PixelType {
    BYTE(GL11C.GL_BYTE),
    SHORT(GL11C.GL_SHORT),
    INT(GL11C.GL_INT),
    HALF_FLOAT(GL30C.GL_HALF_FLOAT),
    FLOAT(GL11C.GL_FLOAT),
    UNSIGNED_BYTE(GL11C.GL_UNSIGNED_BYTE),
    UNSIGNED_BYTE_3_3_2(GL12C.GL_UNSIGNED_BYTE_3_3_2),
    UNSIGNED_BYTE_2_3_3_REV(GL12C.GL_UNSIGNED_BYTE_2_3_3_REV),
    UNSIGNED_SHORT(GL11C.GL_UNSIGNED_SHORT),
    UNSIGNED_SHORT_5_6_5(GL12C.GL_UNSIGNED_SHORT_5_6_5),
    UNSIGNED_SHORT_5_6_5_REV(GL12C.GL_UNSIGNED_SHORT_5_6_5_REV),
    UNSIGNED_SHORT_4_4_4_4(GL12C.GL_UNSIGNED_SHORT_4_4_4_4),
    UNSIGNED_SHORT_4_4_4_4_REV(GL12C.GL_UNSIGNED_SHORT_4_4_4_4_REV),
    UNSIGNED_SHORT_5_5_5_1(GL12C.GL_UNSIGNED_SHORT_5_5_5_1),
    UNSIGNED_SHORT_1_5_5_5_REV(GL12C.GL_UNSIGNED_SHORT_1_5_5_5_REV),
    UNSIGNED_INT(GL11C.GL_UNSIGNED_INT),
    UNSIGNED_INT_8_8_8_8(GL12C.GL_UNSIGNED_INT_8_8_8_8),
    UNSIGNED_INT_8_8_8_8_REV(GL12C.GL_UNSIGNED_INT_8_8_8_8_REV),
    UNSIGNED_INT_10_10_10_2(GL12C.GL_UNSIGNED_INT_10_10_10_2),
    UNSIGNED_INT_2_10_10_10_REV(GL12C.GL_UNSIGNED_INT_2_10_10_10_REV);

    private final int glFormat;

    PixelType(int glFormat) {
        this.glFormat = glFormat;
    }

    public static Optional<PixelType> fromString(String name) {
        try {
            return Optional.of(PixelType.valueOf(name));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public int getGlFormat() {
        return glFormat;
    }
}
