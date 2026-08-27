package re.lilith.aurum.gl.buffer;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.sampler.SamplerLimits;

import java.util.Collections;

public class ShaderStorageBufferHolder {
    private int cachedWidth;
    private int cachedHeight;
    private ShaderStorageBuffer[] buffers;
    private boolean destroyed;

    public ShaderStorageBufferHolder(Int2ObjectMap<ShaderStorageInfo> overrides, int width, int height) {
        destroyed = false;
        cachedWidth = width;
        cachedHeight = height;
        buffers = new ShaderStorageBuffer[Collections.max(overrides.keySet()) + 1];
        overrides.forEach((index, bufferInfo) -> {
            if (index > SamplerLimits.get().getMaxShaderStorageUnits()) {
                throw new IllegalStateException("We don't have enough SSBO units??? (index: " + index + ", max: " + SamplerLimits.get().getMaxShaderStorageUnits());
            }

            buffers[index] = new ShaderStorageBuffer(index, bufferInfo);
            int buffer = buffers[index].getId();

            if (bufferInfo.relative()) {
                buffers[index].resizeIfRelative(width, height);
            } else {
                AurumRenderSystem.bindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, buffer);
                AurumRenderSystem.bufferStorage(GL43.GL_SHADER_STORAGE_BUFFER, bufferInfo.size(), 0);
                AurumRenderSystem.clearBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, GL30.GL_R8, 0, bufferInfo.size(), GL11.GL_RED, GL11.GL_BYTE);
                AurumRenderSystem.bindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, index, buffer);
            }
        });
        AurumRenderSystem.bindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
    }

    public void hasResizedScreen(int width, int height) {
        if (width != cachedWidth || height != cachedHeight) {
            cachedWidth = width;
            cachedHeight = height;
            for (ShaderStorageBuffer buffer : buffers) {
                if (buffer != null) {
                    buffer.resizeIfRelative(width, height);
                }
            }
        }
    }

    public void setupBuffers() {
        if (destroyed) {
            throw new IllegalStateException("Tried to use destroyed buffer objects");
        }

        for (ShaderStorageBuffer buffer : buffers) {
            if (buffer != null) {
                buffer.bind();
            }
        }
    }

    public int getBufferIndex(int index) {
        if (buffers.length < index || buffers[index] == null)
            throw new RuntimeException("Tried to query a buffer for indirect dispatch that doesn't exist!");

        return buffers[index].getId();
    }

    public void destroyBuffers() {
        for (ShaderStorageBuffer buffer : buffers) {
            if (buffer != null) {
                buffer.destroy();
            }
        }
        buffers = null;
        destroyed = true;
    }

}
