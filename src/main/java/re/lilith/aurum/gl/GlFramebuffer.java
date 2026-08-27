package re.lilith.aurum.gl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;
import re.lilith.aurum.gl.texture.DepthBufferFormat;
import re.lilith.aurum.texture.TextureInfoCache;

import java.nio.IntBuffer;

public class GlFramebuffer extends GlResource {
    private final int maxDrawBuffers;
    private final int maxColorAttachments;
    private boolean hasDepthAttachment;

    public GlFramebuffer() {
        super(AurumRenderSystem.createFramebuffer());

        this.maxDrawBuffers = GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS);
        this.maxColorAttachments = GL11.glGetInteger(GL30.GL_MAX_COLOR_ATTACHMENTS);
        this.hasDepthAttachment = false;
    }

    public void addDepthAttachment(int texture) {
        final int internalFormat = TextureInfoCache.INSTANCE.getInfo(texture).getInternalFormat();
        final DepthBufferFormat depthBufferFormat = DepthBufferFormat.fromGlEnumOrDefault(internalFormat);

        final int fb = getGlId();

        if (depthBufferFormat.isCombinedStencil()) {
            AurumRenderSystem.framebufferTexture2D(fb, GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL11.GL_TEXTURE_2D, texture, 0);
        } else {
            AurumRenderSystem.framebufferTexture2D(fb, GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, texture, 0);
        }

        this.hasDepthAttachment = true;
    }

    public void addColorAttachment(int index, int texture) {
        final int fb = getGlId();
        AurumRenderSystem.framebufferTexture2D(fb, GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 + index, GL11.GL_TEXTURE_2D, texture, 0);
    }

    public void noDrawBuffers() {
        AurumRenderSystem.drawBuffers(getGlId(), new int[]{0});
    }

    public void drawBuffers(int[] buffers) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer glBuffers = stack.mallocInt(buffers.length);
            int index = 0;

            if (buffers.length > maxDrawBuffers) {
                throw new IllegalArgumentException("Cannot write to more than " + maxDrawBuffers + " draw buffers on this GPU");
            }
            for (int buffer : buffers) {
                if (buffer >= maxColorAttachments) {
                    throw new IllegalArgumentException("Only " + maxColorAttachments + " color attachments are supported on this GPU, but an attempt was made to write to a color attachment with index " + buffer);
                }
                glBuffers.put(index++, GL30.GL_COLOR_ATTACHMENT0 + buffer);
            }
            AurumRenderSystem.drawBuffers(getGlId(), glBuffers);
        }
    }

    public void readBuffer(int buffer) {
        AurumRenderSystem.readBuffer(getGlId(), GL30.GL_COLOR_ATTACHMENT0 + buffer);
    }

    public boolean hasDepthAttachment() {
        return hasDepthAttachment;
    }

    public void bind() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, getGlId());
    }

    public void bindAsReadBuffer() {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, getGlId());
    }

    protected void destroyInternal() {
        GL30.glDeleteFramebuffers(getGlId());
    }

    public boolean isIncomplete() {
        bind();

        return GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE;
    }

    public int getId() {
        return getGlId();
    }
}