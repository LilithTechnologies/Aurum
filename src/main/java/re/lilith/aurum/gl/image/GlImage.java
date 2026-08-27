package re.lilith.aurum.gl.image;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.GlDebug;
import re.lilith.aurum.gl.GlResource;
import re.lilith.aurum.gl.texture.InternalTextureFormat;
import re.lilith.aurum.gl.texture.PixelFormat;
import re.lilith.aurum.gl.texture.PixelType;
import re.lilith.aurum.gl.texture.TextureType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GlImage extends GlResource {
    public static final Map<String, GlImage> BY_NAME = new ConcurrentHashMap<>();

    protected final String name;
    protected final String samplerName;
    protected final TextureType target;
    protected final PixelFormat format;
    protected final InternalTextureFormat internalTextureFormat;
    protected final PixelType pixelType;
    private final boolean clear;

    public GlImage(String name, String samplerName, TextureType target, PixelFormat format, InternalTextureFormat internalFormat, PixelType pixelType, boolean clear, int width, int height, int depth) {
        super(AurumRenderSystem.createTexture(target.getGlType()));

        this.name = name;
        this.samplerName = samplerName;
        this.target = target;
        this.format = format;
        this.internalTextureFormat = internalFormat;
        this.pixelType = pixelType;
        this.clear = clear;

        GlDebug.nameObject(GL11.GL_TEXTURE, getGlId(), name);

        AurumRenderSystem.bindTextureForTarget(target.getGlType(), getGlId());
        target.apply(getGlId(), width, height, depth, internalFormat.getGlFormat(), format.getGlFormat(), pixelType.getGlFormat(), null);

        setup(getGlId(), width, height, depth);

        AurumRenderSystem.bindTextureForTarget(target.getGlType(), 0);

        BY_NAME.put(name, this);
    }

    protected void setup(int texture, int width, int height, int depth) {
        final boolean isInteger = internalTextureFormat.getPixelFormat().isInteger();
        AurumRenderSystem.texParameteri(texture, target.getGlType(), GL11.GL_TEXTURE_MIN_FILTER, isInteger ? GL11.GL_NEAREST : GL11.GL_LINEAR);
        AurumRenderSystem.texParameteri(texture, target.getGlType(), GL11.GL_TEXTURE_MAG_FILTER, isInteger ? GL11.GL_NEAREST : GL11.GL_LINEAR);
        AurumRenderSystem.texParameteri(texture, target.getGlType(), GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);

        if (height > 0) {
            AurumRenderSystem.texParameteri(texture, target.getGlType(), GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        }

        if (depth > 0) {
            AurumRenderSystem.texParameteri(texture, target.getGlType(), GL12.GL_TEXTURE_WRAP_R, GL12.GL_CLAMP_TO_EDGE);
        }

        AurumRenderSystem.texParameteri(texture, target.getGlType(), GL12.GL_TEXTURE_MAX_LEVEL, 0);
        AurumRenderSystem.texParameteri(texture, target.getGlType(), GL12.GL_TEXTURE_MIN_LOD, 0);
        AurumRenderSystem.texParameteri(texture, target.getGlType(), GL12.GL_TEXTURE_MAX_LOD, 0);
        AurumRenderSystem.texParameterf(texture, target.getGlType(), GL14.GL_TEXTURE_LOD_BIAS, 0.0F);

        AurumRenderSystem.clearTexImage(texture, 0, format.getGlFormat(), pixelType.getGlFormat());
    }

    public String getName() {
        return name;
    }

    public String getSamplerName() {
        return samplerName;
    }

    public TextureType getTarget() {
        return target;
    }

    public boolean shouldClear() {
        return clear;
    }

    public int getId() {
        return getGlId();
    }

    public void clear() {
        AurumRenderSystem.clearTexImage(getGlId(), 0, format.getGlFormat(), pixelType.getGlFormat());
    }

    public void updateNewSize(int width, int height) {
    }

    @Override
    protected void destroyInternal() {
        GL11.glDeleteTextures(getGlId());
        AurumRenderSystem.forgetTextureTarget(getGlId());
        BY_NAME.remove(name, this);
    }

    public InternalTextureFormat getInternalFormat() {
        return internalTextureFormat;
    }

    @Override
    public String toString() {
        return "GlImage name " + name + " format " + format + "internalformat " + internalTextureFormat + " pixeltype " + pixelType;
    }

    public PixelFormat getFormat() {
        return format;
    }

    public PixelType getPixelType() {
        return pixelType;
    }

    public static class Relative extends GlImage {

        private final float relativeHeight;
        private final float relativeWidth;

        public Relative(String name, String samplerName, PixelFormat format, InternalTextureFormat internalFormat, PixelType pixelType, boolean clear, float relativeWidth, float relativeHeight, int currentWidth, int currentHeight) {
            super(name, samplerName, TextureType.TEXTURE_2D, format, internalFormat, pixelType, clear, (int) (currentWidth * relativeWidth), (int) (currentHeight * relativeHeight), 0);

            this.relativeWidth = relativeWidth;
            this.relativeHeight = relativeHeight;
        }

        @Override
        public void updateNewSize(int width, int height) {
            AurumRenderSystem.bindTextureForTarget(target.getGlType(), getGlId());
            target.apply(getGlId(), (int) (width * relativeWidth), (int) (height * relativeHeight), 0, internalTextureFormat.getGlFormat(), format.getGlFormat(), pixelType.getGlFormat(), null);

            setup(getGlId(), width, height, 0);

            AurumRenderSystem.bindTextureForTarget(target.getGlType(), 0);
        }
    }
}
