// This file is based on code from Sodium by JellySquid, licensed under the LGPLv3 license.

package re.lilith.aurum.gl.shader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.KHRDebug;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.GlDebug;

public class ProgramCreator {
    private static final Logger LOGGER = LogManager.getLogger(ProgramCreator.class);

    public static int create(String name, GlShader... shaders) {
        int program = GL20.glCreateProgram();

        // TODO: This is *really* hardcoded, we need to refactor this to support external calls
        // to glBindAttribLocation
        AurumRenderSystem.bindAttributeLocation(program, 11, "mc_Entity");
        AurumRenderSystem.bindAttributeLocation(program, 12, "mc_midTexCoord");
        AurumRenderSystem.bindAttributeLocation(program, 13, "at_tangent");
        AurumRenderSystem.bindAttributeLocation(program, 14, "at_midBlock");

        for (GlShader shader : shaders) {
            GL20.glAttachShader(program, shader.getHandle());
        }

        GL20.glLinkProgram(program);

        GlDebug.nameObject(KHRDebug.GL_PROGRAM, program, name);

        // Always detach shaders according to https://www.khronos.org/opengl/wiki/Shader_Compilation#Cleanup
        for (GlShader shader : shaders) {
            AurumRenderSystem.detachShader(program, shader.getHandle());
        }

        String log = AurumRenderSystem.getProgramInfoLog(program);

        if (!log.isEmpty()) {
            LOGGER.warn("Program link log for {}: {}", name, log);
        }

        int result = GL20.glGetProgrami(program, GL20C.GL_LINK_STATUS);

        if (result != GL20C.GL_TRUE) {
            throw new RuntimeException("Shader program linking failed, see log for details");
        }

        return program;
    }
}
