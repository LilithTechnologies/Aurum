package re.lilith.aurum.gl.texture;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL31C;

import java.util.Optional;

public enum InternalTextureFormat {
    // Default
    // NB: strictly speaking this isn't in the OptiFine format spec, but it's the GL default so we expose it anyway
    RGBA(GL11C.GL_RGBA, PixelFormat.RGBA),
    // 8-bit normalized
    R8(GL30C.GL_R8, PixelFormat.RED),
    RG8(GL30C.GL_RG8, PixelFormat.RG),
    RGB8(GL11C.GL_RGB8, PixelFormat.RGB),
    RGBA8(GL11C.GL_RGBA8, PixelFormat.RGBA),
    // 8-bit signed normalized
    R8_SNORM(GL31C.GL_R8_SNORM, PixelFormat.RED),
    RG8_SNORM(GL31C.GL_RG8_SNORM, PixelFormat.RG),
    RGB8_SNORM(GL31C.GL_RGB8_SNORM, PixelFormat.RGB),
    RGBA8_SNORM(GL31C.GL_RGBA8_SNORM, PixelFormat.RGBA),
    // 16-bit normalized
    R16(GL30C.GL_R16, PixelFormat.RED),
    RG16(GL30C.GL_RG16, PixelFormat.RG),
    RGB16(GL11C.GL_RGB16, PixelFormat.RGB),
    RGBA16(GL11C.GL_RGBA16, PixelFormat.RGBA),
    // 16-bit signed normalized
    R16_SNORM(GL31C.GL_R16_SNORM, PixelFormat.RED),
    RG16_SNORM(GL31C.GL_RG16_SNORM, PixelFormat.RG),
    RGB16_SNORM(GL31C.GL_RGB16_SNORM, PixelFormat.RGB),
    RGBA16_SNORM(GL31C.GL_RGBA16_SNORM, PixelFormat.RGBA),
    // 16-bit float
    R16F(GL30C.GL_R16F, PixelFormat.RED),
    RG16F(GL30C.GL_RG16F, PixelFormat.RG),
    RGB16F(GL30C.GL_RGB16F, PixelFormat.RGB),
    RGBA16F(GL30C.GL_RGBA16F, PixelFormat.RGBA),
    // 32-bit float
    R32F(GL30C.GL_R32F, PixelFormat.RED),
    RG32F(GL30C.GL_RG32F, PixelFormat.RG),
    RGB32F(GL30C.GL_RGB32F, PixelFormat.RGB),
    RGBA32F(GL30C.GL_RGBA32F, PixelFormat.RGBA),
    // 8-bit integer
    R8I(GL30C.GL_R8I, PixelFormat.RED_INTEGER),
    RG8I(GL30C.GL_RG8I, PixelFormat.RG_INTEGER),
    RGB8I(GL30C.GL_RGB8I, PixelFormat.RGB_INTEGER),
    RGBA8I(GL30C.GL_RGBA8I, PixelFormat.RGBA_INTEGER),
    // 8-bit unsigned integer
    R8UI(GL30C.GL_R8UI, PixelFormat.RED_INTEGER),
    RG8UI(GL30C.GL_RG8UI, PixelFormat.RG_INTEGER),
    RGB8UI(GL30C.GL_RGB8UI, PixelFormat.RGB_INTEGER),
    RGBA8UI(GL30C.GL_RGBA8UI, PixelFormat.RGBA_INTEGER),
    // 16-bit integer
    R16I(GL30C.GL_R16I, PixelFormat.RED_INTEGER),
    RG16I(GL30C.GL_RG16I, PixelFormat.RG_INTEGER),
    RGB16I(GL30C.GL_RGB16I, PixelFormat.RGB_INTEGER),
    RGBA16I(GL30C.GL_RGBA16I, PixelFormat.RGBA_INTEGER),
    // 16-bit unsigned integer
    R16UI(GL30C.GL_R16UI, PixelFormat.RED_INTEGER),
    RG16UI(GL30C.GL_RG16UI, PixelFormat.RG_INTEGER),
    RGB16UI(GL30C.GL_RGB16UI, PixelFormat.RGB_INTEGER),
    RGBA16UI(GL30C.GL_RGBA16UI, PixelFormat.RGBA_INTEGER),
    // 32-bit integer
    R32I(GL30C.GL_R32I, PixelFormat.RED_INTEGER),
    RG32I(GL30C.GL_RG32I, PixelFormat.RG_INTEGER),
    RGB32I(GL30C.GL_RGB32I, PixelFormat.RGB_INTEGER),
    RGBA32I(GL30C.GL_RGBA32I, PixelFormat.RGBA_INTEGER),
    // 32-bit unsigned integer
    R32UI(GL30C.GL_R32UI, PixelFormat.RED_INTEGER),
    RG32UI(GL30C.GL_RG32UI, PixelFormat.RG_INTEGER),
    RGB32UI(GL30C.GL_RGB32UI, PixelFormat.RGB_INTEGER),
    RGBA32UI(GL30C.GL_RGBA32UI, PixelFormat.RGBA_INTEGER),
    // Mixed
    R3_G3_B2(GL11C.GL_R3_G3_B2, PixelFormat.RGB),
    RGB5_A1(GL11C.GL_RGB5_A1, PixelFormat.RGBA),
    RGB10_A2(GL11C.GL_RGB10_A2, PixelFormat.RGBA),
    R11F_G11F_B10F(GL30C.GL_R11F_G11F_B10F, PixelFormat.RGB),
    RGB9_E5(GL30C.GL_RGB9_E5, PixelFormat.RGB);

    private final int glFormat;
    private final PixelFormat expectedPixelFormat;

    InternalTextureFormat(int glFormat, PixelFormat expectedPixelFormat) {
        this.glFormat = glFormat;
        this.expectedPixelFormat = expectedPixelFormat;
    }

    public static Optional<InternalTextureFormat> fromString(String name) {
        try {
            return Optional.of(InternalTextureFormat.valueOf(name));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public int getGlFormat() {
        return glFormat;
    }

    public PixelFormat getPixelFormat() {
        return expectedPixelFormat;
    }
}
