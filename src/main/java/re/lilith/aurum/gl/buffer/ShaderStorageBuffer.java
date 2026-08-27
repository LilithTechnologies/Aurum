package re.lilith.aurum.gl.buffer;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL43C;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.GlDebug;

public class ShaderStorageBuffer {
    protected final int index;
    protected final ShaderStorageInfo info;
    protected int id;

    public ShaderStorageBuffer(int index, ShaderStorageInfo info) {
        this.id = AurumRenderSystem.genBuffers();
        GlDebug.nameObject(GL43C.GL_BUFFER, id, "SSBO " + index);
        this.index = index;
        this.info = info;
    }

    public final int getId() {
        return id;
    }

    public final int getIndex() {
        return index;
    }

    public final long getSize() {
        return info.size();
    }

    protected void destroy() {
        AurumRenderSystem.bindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, index, 0);
        AurumRenderSystem.deleteBuffers(id);
    }

    public void bind() {
        AurumRenderSystem.bindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, index, id);
    }

    public void resizeIfRelative(int width, int height) {
        if (!info.relative()) return;

        AurumRenderSystem.deleteBuffers(id);
        int newId = AurumRenderSystem.genBuffers();
        GlDebug.nameObject(GL43C.GL_BUFFER, newId, "SSBO " + index);
        AurumRenderSystem.bindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, newId);

        long newWidth = (long) (width * info.scaleX());
        long newHeight = (long) (height * info.scaleY());
        long finalSize = (newHeight * newWidth) * info.size();
        AurumRenderSystem.bufferStorage(GL43C.GL_SHADER_STORAGE_BUFFER, finalSize, 0);
        AurumRenderSystem.clearBufferSubData(GL43C.GL_SHADER_STORAGE_BUFFER, GL30C.GL_R8, 0, finalSize, GL11C.GL_RED, GL11C.GL_BYTE);
        AurumRenderSystem.bindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, index, newId);
        id = newId;
    }
}
