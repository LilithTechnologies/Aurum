package re.lilith.aurum.texture.util;

import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class NativeImage implements Closeable {
    private final Format format;
    private final int width;
    private final int height;
    private long pixels;

    public NativeImage(int width, int height, boolean clear) {
        this(Format.RGBA, width, height, clear);
    }

    public NativeImage(Format format, int width, int height, boolean clear) {
        this.format = format;
        this.width = width;
        this.height = height;
        long size = (long) width * height * format.components();
        this.pixels = clear ? MemoryUtil.nmemCalloc(1, size) : MemoryUtil.nmemAlloc(size);
    }

    @Override
    public String toString() {
        return "NativeImage[" + format + " " + width + "x" + height + "@" + pixels + "]";
    }

    public static NativeImage read(InputStream inputStream) throws IOException {
        try (InputStream in = inputStream) {
            return fromBufferedImage(readBufferedImage(in));
        }
    }

    public static NativeImage read(ByteBuffer byteBuffer) throws IOException {
        ByteBuffer duplicate = byteBuffer.duplicate();
        byte[] bytes = new byte[duplicate.remaining()];
        duplicate.get(bytes);
        return fromBufferedImage(readBufferedImage(new ByteArrayInputStream(bytes)));
    }

    private static BufferedImage readBufferedImage(InputStream inputStream) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(inputStream);
        if (bufferedImage == null) {
            throw new IOException("Could not load image: unrecognized or corrupt format");
        }
        return bufferedImage;
    }

    private static NativeImage fromBufferedImage(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        NativeImage nativeImage = new NativeImage(Format.RGBA, width, height, false);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                nativeImage.setPixelRGBA(x, y, fromArgb(bufferedImage.getRGB(x, y)));
            }
        }

        return nativeImage;
    }

    private static void setClamp(boolean clamp) {
        if (clamp) {
            GL11.glTexParameteri(3553, 10242, 10496);
            GL11.glTexParameteri(3553, 10243, 10496);
        } else {
            GL11.glTexParameteri(3553, 10242, 10497);
            GL11.glTexParameteri(3553, 10243, 10497);
        }
    }

    private static void setFilter(boolean blur, boolean mipmap) {
        if (blur) {
            GL11.glTexParameteri(3553, 10241, mipmap ? 9987 : 9729);
            GL11.glTexParameteri(3553, 10240, 9729);
        } else {
            GL11.glTexParameteri(3553, 10241, mipmap ? 9986 : 9728);
            GL11.glTexParameteri(3553, 10240, 9728);
        }
    }

    private void checkAllocated() {
        if (this.pixels == 0L) {
            throw new IllegalStateException("Image is not allocated.");
        }
    }

    @Override
    public void close() {
        if (this.pixels != 0L) {
            MemoryUtil.nmemFree(this.pixels);
        }
        this.pixels = 0L;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public Format format() {
        return this.format;
    }

    private long getOffset(int x, int y) {
        if (x > this.width || y > this.height) {
            throw new IllegalArgumentException(String.format("(%s, %s) outside of image bounds (%s, %s)", x, y, this.width, this.height));
        }
        this.checkAllocated();
        return (x + y * (long) this.width) * 4;
    }

    public int getPixelRGBA(int x, int y) {
        if (this.format != Format.RGBA) {
            throw new IllegalArgumentException(String.format("getPixelRGBA only works on RGBA images; have %s", this.format));
        }
        long offset = getOffset(x, y);
        return MemoryUtil.memGetInt(this.pixels + offset);
    }

    public void setPixelRGBA(int x, int y, int color) {
        if (this.format != Format.RGBA) {
            throw new IllegalArgumentException(String.format("setPixelRGBA only works on RGBA images; have %s", this.format));
        }
        long offset = getOffset(x, y);
        MemoryUtil.memPutInt(this.pixels + offset, color);
    }

    public int[] makePixelArray() {
        this.checkAllocated();
        int[] pixelArray = new int[this.getWidth() * this.getHeight()];
        for (int y = 0; y < this.getHeight(); ++y) {
            for (int x = 0; x < this.getWidth(); ++x) {
                pixelArray[x + y * this.getWidth()] = toArgb(this.getPixelRGBA(x, y));
            }
        }
        return pixelArray;
    }

    public void upload(int level, int x, int y, boolean close) {
        this.upload(level, x, y, 0, 0, this.width, this.height, false, close);
    }

    public void upload(int level, int x, int y, int unpackSkipPixels, int unpackSkipRows, int width, int height, boolean mipmap, boolean close) {
        this.upload(level, x, y, unpackSkipPixels, unpackSkipRows, width, height, false, false, mipmap, close);
    }

    public void upload(int level, int x, int y, int unpackSkipPixels, int unpackSkipRows, int width, int height, boolean blur, boolean clamp, boolean mipmap, boolean close) {
        this.checkAllocated();
        setFilter(blur, mipmap);
        setClamp(clamp);
        if (width == this.getWidth()) {
            GL11.glPixelStorei(3314, 0);
        } else {
            GL11.glPixelStorei(3314, this.getWidth());
        }
        GL11.glPixelStorei(3316, unpackSkipPixels);
        GL11.glPixelStorei(3315, unpackSkipRows);
        this.format.setUnpackPixelStoreState();
        GL11.glTexSubImage2D(3553, level, x, y, width, height, this.format.glFormat(), 5121, this.pixels);
        if (close) {
            this.close();
        }
    }

    public void downloadTexture(int level, boolean fillOpaqueAlpha) {
        this.checkAllocated();
        this.format.setPackPixelStoreState();
        GL11.glGetTexImage(3553, level, this.format.glFormat(), 5121, this.pixels);
        if (fillOpaqueAlpha && this.format.hasAlpha()) {
            for (int y = 0; y < this.getHeight(); ++y) {
                for (int x = 0; x < this.getWidth(); ++x) {
                    this.setPixelRGBA(x, y, this.getPixelRGBA(x, y) | 255 << this.format.alphaOffset());
                }
            }
        }
    }

    public void writeToFile(Path path) throws IOException {
        this.checkAllocated();
        BufferedImage bufferedImage = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                bufferedImage.setRGB(x, y, toArgb(this.getPixelRGBA(x, y)));
            }
        }
        try (OutputStream out = Files.newOutputStream(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            if (!ImageIO.write(bufferedImage, "png", out)) {
                throw new IOException("No PNG writer available to write \"" + path.toAbsolutePath() + "\"");
            }
        }
    }

    public static int getA(int rgba) {
        return rgba >> 24 & 0xFF;
    }

    public static int getR(int rgba) {
        return rgba & 0xFF;
    }

    public static int getG(int rgba) {
        return rgba >> 8 & 0xFF;
    }

    public static int getB(int rgba) {
        return rgba >> 16 & 0xFF;
    }

    public static int combine(int a, int b, int g, int r) {
        return (a & 0xFF) << 24 | (b & 0xFF) << 16 | (g & 0xFF) << 8 | (r & 0xFF);
    }

    private static int toArgb(int rgba) {
        return getA(rgba) << 24 | getR(rgba) << 16 | getG(rgba) << 8 | getB(rgba);
    }

    private static int fromArgb(int argb) {
        int a = argb >>> 24;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return combine(a, b, g, r);
    }

    public enum Format {
        RGBA(4, 6408, true, 24);

        private final int components;
        private final int glFormat;
        private final boolean hasAlpha;
        private final int alphaOffset;

        Format(int components, int glFormat, boolean hasAlpha, int alphaOffset) {
            this.components = components;
            this.glFormat = glFormat;
            this.hasAlpha = hasAlpha;
            this.alphaOffset = alphaOffset;
        }

        public int components() {
            return this.components;
        }

        public void setPackPixelStoreState() {
            GL11.glPixelStorei(3333, this.components());
        }

        public void setUnpackPixelStoreState() {
            GL11.glPixelStorei(3317, this.components());
        }

        public int glFormat() {
            return this.glFormat;
        }

        public boolean hasAlpha() {
            return this.hasAlpha;
        }

        public int alphaOffset() {
            return this.alphaOffset;
        }
    }
}
