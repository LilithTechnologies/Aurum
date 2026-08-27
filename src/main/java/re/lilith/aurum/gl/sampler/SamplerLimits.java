package re.lilith.aurum.gl.sampler;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL43C;
import re.lilith.aurum.gl.AurumRenderSystem;

public class SamplerLimits {
    private final int maxTextureUnits;
    private final int maxShaderStorageUnits;
    private static SamplerLimits instance;

    private SamplerLimits() {
        this.maxTextureUnits = GL20.glGetInteger(GL20C.GL_MAX_TEXTURE_IMAGE_UNITS);
        this.maxShaderStorageUnits = AurumRenderSystem.supportsSSBO() ? GL20.glGetInteger(GL43C.GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS) : 0;
    }

    public int getMaxTextureUnits() {
        return maxTextureUnits;
    }

    public int getMaxShaderStorageUnits() {
        return maxShaderStorageUnits;
    }

    public static SamplerLimits get() {
        if (instance == null) {
            instance = new SamplerLimits();
        }

        return instance;
    }
}
