package re.lilith.aurum.gl;

import com.mojang.blaze3d.platform.GlStateManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.mixin.access.GlStateManagerAccessor;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is responsible for abstracting calls to OpenGL and asserting that calls are run on the render thread.
 */
public class AurumRenderSystem {
    private static DSAAccess dsaState;
    private static boolean supportsCompute;

    private static final Map<Integer, Integer> textureTargets = new HashMap<>();

    public static int getTextureTarget(int texture) {
        return textureTargets.getOrDefault(texture, GL11C.GL_TEXTURE_2D);
    }

    public static void forgetTextureTarget(int texture) {
        textureTargets.remove(texture);
    }

    public static void initRenderer() {
        if (GL.getCapabilities().OpenGL45) {
            dsaState = new DSACore();
            Aurum.LOGGER.info("OpenGL 4.5 detected, enabling DSA.");
        } else if (GL.getCapabilities().GL_ARB_direct_state_access) {
            dsaState = new DSAARB();
            Aurum.LOGGER.info("ARB_direct_state_access detected, enabling DSA.");
        } else {
            dsaState = new DSAUnsupported();
            Aurum.LOGGER.info("DSA support not detected.");
        }

        supportsCompute = supportsCompute();
    }

    public static void getIntegerv(int pname, int[] params) {
        GL30C.glGetIntegerv(pname, params);
    }

    public static void getFloatv(int pname, float[] params) {
        GL30C.glGetFloatv(pname, params);
    }

    public static void generateMipmaps(int texture, int mipmapTarget) {
        dsaState.generateMipmaps(texture, mipmapTarget);
    }

    public static void bindAttributeLocation(int program, int index, CharSequence name) {
        GL30C.glBindAttribLocation(program, index, name);
    }

    public static void bindTextureForTarget(int target, int texture) {
        if (target == GL11C.GL_TEXTURE_2D) {
            GlStateManager.bindTexture(texture);
        } else {
            GL30C.glBindTexture(target, texture);
        }
    }

    public static void texImage1D(int texture, int target, int level, int internalformat, int width, int border, int format, int type, @NotNull ByteBuffer pixels) {
        bindTextureForTarget(target, texture);
        GL30C.glTexImage1D(target, level, internalformat, width, border, format, type, pixels);
    }

    public static void texImage2D(int texture, int target, int level, int internalformat, int width, int height, int border, int format, int type, @Nullable ByteBuffer pixels) {
        bindTextureForTarget(target, texture);
        GL30C.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }

    public static void texImage3D(int texture, int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, @NotNull ByteBuffer pixels) {
        bindTextureForTarget(target, texture);
        GL30C.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, pixels);
    }

    public static void uniformMatrix4fv(int location, boolean transpose, FloatBuffer matrix) {
        GL30C.glUniformMatrix4fv(location, transpose, matrix);
    }

    public static void uniformMatrix3fv(int location, boolean transpose, FloatBuffer matrix) {
        GL30C.glUniformMatrix3fv(location, transpose, matrix);
    }

    public static void copyTexImage2D(int target, int level, int internalFormat, int x, int y, int width, int height, int border) {
        GL30C.glCopyTexImage2D(target, level, internalFormat, x, y, width, height, border);
    }

    public static void uniform1f(int location, float v0) {
        GL30C.glUniform1f(location, v0);
    }

    public static void uniform1i(int location, int v0) {
        GL30C.glUniform1i(location, v0);
    }

    public static void uniform2f(int location, float v0, float v1) {
        GL30C.glUniform2f(location, v0, v1);
    }

    public static void uniform2i(int location, int v0, int v1) {
        GL30C.glUniform2i(location, v0, v1);
    }

    public static void uniform3f(int location, float v0, float v1, float v2) {
        GL30C.glUniform3f(location, v0, v1, v2);
    }

    public static void uniform3i(int location, int v0, int v1, int v2) {
        GL30C.glUniform3i(location, v0, v1, v2);
    }

    public static void uniform4f(int location, float v0, float v1, float v2, float v3) {
        GL30C.glUniform4f(location, v0, v1, v2, v3);
    }

    public static void uniform4i(int location, int v0, int v1, int v2, int v3) {
        GL30C.glUniform4i(location, v0, v1, v2, v3);
    }

    public static int getUniformLocation(int programId, String name) {
        return GL30C.glGetUniformLocation(programId, name);
    }

    public static void texParameteriv(int texture, int target, int pname, int[] params) {
        dsaState.texParameteriv(texture, target, pname, params);
    }

    public static void copyTexSubImage2D(int destTexture, int target, int i, int i1, int i2, int i3, int i4, int width, int height) {
        dsaState.copyTexSubImage2D(destTexture, target, i, i1, i2, i3, i4, width, height);
    }

    public static void texParameteri(int texture, int target, int pname, int param) {
        dsaState.texParameteri(texture, target, pname, param);
    }

    public static void texParameterf(int texture, int target, int pname, float param) {
        dsaState.texParameterf(texture, target, pname, param);
    }

    public static String getProgramInfoLog(int program) {
        return GL30C.glGetProgramInfoLog(program);
    }

    public static String getShaderInfoLog(int shader) {
        return GL30C.glGetShaderInfoLog(shader);
    }

    public static void drawBuffers(int framebuffer, int[] buffers) {
        dsaState.drawBuffers(framebuffer, buffers);
    }

    public static void drawBuffers(int framebuffer, IntBuffer buffers) {
        int[] buffer = new int[buffers.limit()];
        buffers.get(buffer);
        dsaState.drawBuffers(framebuffer, buffer);
    }

    public static void readBuffer(int framebuffer, int buffer) {
        dsaState.readBuffer(framebuffer, buffer);
    }

    public static String getActiveUniform(int program, int index, int size, IntBuffer type, IntBuffer name) {
        return GL30C.glGetActiveUniform(program, index, size, type, name);
    }

    public static void bufferData(int target, float[] data, int usage) {
        GL30C.glBufferData(target, data, usage);
    }

    public static int bufferStorage(int target, float[] data, int usage) {
        return dsaState.bufferStorage(target, data, usage);
    }

    public static int genBuffers() {
        return GL15C.glGenBuffers();
    }

    public static void deleteBuffers(int buffer) {
        GL15C.glDeleteBuffers(buffer);
    }

    public static void bindBuffer(int target, int buffer) {
        GL15C.glBindBuffer(target, buffer);
    }

    public static void bindBufferBase(int target, int index, int buffer) {
        GL30C.glBindBufferBase(target, index, buffer);
    }

    public static void bufferStorage(int target, long size, int flags) {
        GL44C.glBufferStorage(target, size, flags);
    }

    public static void clearBufferSubData(int target, int internalFormat, long offset, long size, int format, int type) {
        GL43C.glClearBufferSubData(target, internalFormat, offset, size, format, type, (int[]) null);
    }

    public static boolean supportsSSBO() {
        return supportsCompute;
    }

    public static void clearTexImage(int texture, int level, int format, int type) {
        GL44C.glClearTexImage(texture, level, format, type, (ByteBuffer) null);
    }

    public static void vertexAttrib4f(int index, float v0, float v1, float v2, float v3) {
        GL30C.glVertexAttrib4f(index, v0, v1, v2, v3);
    }

    public static void disableVertexAttribArray(int index) {
        GL30C.glDisableVertexAttribArray(index);
    }

    public static void resetPackGenericAttributes() {
        for (int location = 11; location <= 14; location++) {
            disableVertexAttribArray(location);
            vertexAttrib4f(location, 0.0F, 0.0F, 0.0F, 1.0F);
        }
    }

    public static void detachShader(int program, int shader) {
        GL30C.glDetachShader(program, shader);
    }

    public static void framebufferTexture2D(int fb, int fbtarget, int attachment, int target, int texture, int levels) {
        dsaState.framebufferTexture2D(fb, fbtarget, attachment, target, texture, levels);
    }

    public static int getTexParameteri(int texture, int target, int pname) {
        return dsaState.getTexParameteri(texture, target, pname);
    }

    public static void bindImageTexture(int unit, int texture, int level, boolean layered, int layer, int access, int format) {
        if (GL.getCapabilities().OpenGL42) {
            GL45C.glBindImageTexture(unit, texture, level, layered, layer, access, format);
        } else {
            EXTShaderImageLoadStore.glBindImageTextureEXT(unit, texture, level, layered, layer, access, format);
        }
    }

    public static int getMaxImageUnits() {
        if (GL.getCapabilities().OpenGL42) {
            return GL11.glGetInteger(GL45C.GL_MAX_IMAGE_UNITS);
        } else if (GL.getCapabilities().GL_EXT_shader_image_load_store) {
            return GL11.glGetInteger(EXTShaderImageLoadStore.GL_MAX_IMAGE_UNITS_EXT);
        } else {
            return 0;
        }
    }

    public static void getProgramiv(int program, int value, int[] storage) {
        GL30C.glGetProgramiv(program, value, storage);
    }

    public static void dispatchCompute(Vector3i workGroups) {
        GL45C.glDispatchCompute(workGroups.x, workGroups.y, workGroups.z);
    }

    public static void dispatchComputeIndirect(long offset) {
        GL43C.glDispatchComputeIndirect(offset);
    }

    public static void memoryBarrier(int barriers) {
        if (supportsCompute) {
            GL45C.glMemoryBarrier(barriers);
        }
    }

    public static boolean supportsBufferBlending() {
        return GL.getCapabilities().GL_ARB_draw_buffers_blend || GL.getCapabilities().OpenGL40;
    }

    public static void disableBufferBlend(int buffer) {
        GL30C.glDisablei(GL30C.GL_BLEND, buffer);
    }

    public static void enableBufferBlend(int buffer) {
        GL30C.glEnablei(GL30C.GL_BLEND, buffer);
    }

    public static void blendFuncSeparatei(int buffer, int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        GL40C.glBlendFuncSeparatei(buffer, srcRGB, dstRGB, srcAlpha, dstAlpha);
    }

    public static void bindTextureToUnit(int unit, int texture) {
        dsaState.bindTextureToUnit(unit, texture);
    }

    public static void setupProjectionMatrix(float[] matrix) {
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GL20.glLoadMatrixf(matrix);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
    }

    public static void restoreProjectionMatrix() {
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
    }

    public static void setupModelViewMatrix(float[] matrix) {
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GL20.glLoadMatrixf(matrix);
    }

    public static void restoreModelViewMatrix() {
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
    }

    public static void blitFramebuffer(int source, int dest, int offsetX, int offsetY, int width, int height, int offsetX2, int offsetY2, int width2, int height2, int bufferChoice, int filter) {
        dsaState.blitFramebuffer(source, dest, offsetX, offsetY, width, height, offsetX2, offsetY2, width2, height2, bufferChoice, filter);
    }

    public static int createFramebuffer() {
        return dsaState.createFramebuffer();
    }

    public static int createTexture(int target) {
        int texture = dsaState.createTexture(target);
        textureTargets.put(texture, target);
        return texture;
    }

    public interface DSAAccess {
        void generateMipmaps(int texture, int target);

        void texParameteri(int texture, int target, int pname, int param);

        void texParameterf(int texture, int target, int pname, float param);

        void texParameteriv(int texture, int target, int pname, int[] params);

        void readBuffer(int framebuffer, int buffer);

        void drawBuffers(int framebuffer, int[] buffers);

        int getTexParameteri(int texture, int target, int pname);

        void copyTexSubImage2D(int destTexture, int target, int i, int i1, int i2, int i3, int i4, int width, int height);

        void bindTextureToUnit(int unit, int texture);

        int bufferStorage(int target, float[] data, int usage);

        void blitFramebuffer(int source, int dest, int offsetX, int offsetY, int width, int height, int offsetX2, int offsetY2, int width2, int height2, int bufferChoice, int filter);

        void framebufferTexture2D(int fb, int fbtarget, int attachment, int target, int texture, int levels);

        int createFramebuffer();

        int createTexture(int target);
    }

    public static class DSACore extends DSAARB {

    }

    public static class DSAARB extends DSAUnsupported {

        @Override
        public void generateMipmaps(int texture, int target) {
            ARBDirectStateAccess.glGenerateTextureMipmap(texture);
        }

        @Override
        public void texParameteri(int texture, int target, int pname, int param) {
            ARBDirectStateAccess.glTextureParameteri(texture, pname, param);
        }

        @Override
        public void texParameterf(int texture, int target, int pname, float param) {
            ARBDirectStateAccess.glTextureParameterf(texture, pname, param);
        }

        @Override
        public void texParameteriv(int texture, int target, int pname, int[] params) {
            ARBDirectStateAccess.glTextureParameteriv(texture, pname, params);
        }

        @Override
        public void readBuffer(int framebuffer, int buffer) {
            ARBDirectStateAccess.glNamedFramebufferReadBuffer(framebuffer, buffer);
        }

        @Override
        public void drawBuffers(int framebuffer, int[] buffers) {
            ARBDirectStateAccess.glNamedFramebufferDrawBuffers(framebuffer, buffers);
        }

        @Override
        public int getTexParameteri(int texture, int target, int pname) {
            return ARBDirectStateAccess.glGetTextureParameteri(texture, pname);
        }

        @Override
        public void copyTexSubImage2D(int destTexture, int target, int i, int i1, int i2, int i3, int i4, int width, int height) {
            ARBDirectStateAccess.glCopyTextureSubImage2D(destTexture, i, i1, i2, i3, i4, width, height);
        }

        @Override
        public void bindTextureToUnit(int unit, int texture) {
            if (texture == 0) {
                super.bindTextureToUnit(unit, texture);
            } else {
                ARBDirectStateAccess.glBindTextureUnit(unit, texture);
                GlStateManager.activeTexture(GL13.GL_TEXTURE0 + unit);
                GlStateManager.bindTexture(texture);
            }
        }

        @Override
        public int bufferStorage(int target, float[] data, int usage) {
            int buffer = GL45C.glCreateBuffers();
            GL45C.glNamedBufferData(buffer, data, usage);
            return buffer;
        }

        @Override
        public void blitFramebuffer(int source, int dest, int offsetX, int offsetY, int width, int height, int offsetX2, int offsetY2, int width2, int height2, int bufferChoice, int filter) {
            ARBDirectStateAccess.glBlitNamedFramebuffer(source, dest, offsetX, offsetY, width, height, offsetX2, offsetY2, width2, height2, bufferChoice, filter);
        }

        @Override
        public void framebufferTexture2D(int fb, int fbtarget, int attachment, int target, int texture, int levels) {
            ARBDirectStateAccess.glNamedFramebufferTexture(fb, attachment, texture, levels);
        }

        @Override
        public int createFramebuffer() {
            return ARBDirectStateAccess.glCreateFramebuffers();
        }

        @Override
        public int createTexture(int target) {
            return ARBDirectStateAccess.glCreateTextures(target);
        }
    }

    public static class DSAUnsupported implements DSAAccess {
        @Override
        public void generateMipmaps(int texture, int target) {
            bindTextureForTarget(target, texture);
            GL30C.glGenerateMipmap(target);
        }

        @Override
        public void texParameteri(int texture, int target, int pname, int param) {
            bindTextureForTarget(target, texture);
            GL30C.glTexParameteri(target, pname, param);
        }

        @Override
        public void texParameterf(int texture, int target, int pname, float param) {
            bindTextureForTarget(target, texture);
            GL30C.glTexParameterf(target, pname, param);
        }

        @Override
        public void texParameteriv(int texture, int target, int pname, int[] params) {
            bindTextureForTarget(target, texture);
            GL30C.glTexParameteriv(target, pname, params);
        }

        @Override
        public void readBuffer(int framebuffer, int buffer) {
            GL30.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, framebuffer);
            GL30C.glReadBuffer(buffer);
        }

        @Override
        public void drawBuffers(int framebuffer, int[] buffers) {
            GL30.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, framebuffer);
            GL30C.glDrawBuffers(buffers);
        }

        @Override
        public int getTexParameteri(int texture, int target, int pname) {
            GlStateManager.bindTexture(texture);
            return GL30C.glGetTexParameteri(target, pname);
        }

        @Override
        public void copyTexSubImage2D(int destTexture, int target, int i, int i1, int i2, int i3, int i4, int width, int height) {
            int previous = GlStateManagerAccessor.getActiveTexture();
            GlStateManager.bindTexture(destTexture);
            GL30C.glCopyTexSubImage2D(target, i, i1, i2, i3, i4, width, height);
            GlStateManager.bindTexture(previous);
        }

        @Override
        public void bindTextureToUnit(int unit, int texture) {
            GlStateManager.activeTexture(GL30C.GL_TEXTURE0 + unit);
            bindTextureForTarget(getTextureTarget(texture), texture);
        }

        @Override
        public int bufferStorage(int target, float[] data, int usage) {
            int buffer = GL30.glGenBuffers();
            GL30.glBindBuffer(target, buffer);
            bufferData(target, data, usage);
            GL30.glBindBuffer(target, 0);

            return buffer;
        }

        @Override
        public void blitFramebuffer(int source, int dest, int offsetX, int offsetY, int width, int height, int offsetX2, int offsetY2, int width2, int height2, int bufferChoice, int filter) {
            GL30.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, source);
            GL30.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, dest);
            GL30C.glBlitFramebuffer(offsetX, offsetY, width, height, offsetX2, offsetY2, width2, height2, bufferChoice, filter);
        }

        @Override
        public void framebufferTexture2D(int fb, int fbtarget, int attachment, int target, int texture, int levels) {
            GL30.glBindFramebuffer(fbtarget, fb);
            GL30C.glFramebufferTexture2D(fbtarget, attachment, target, texture, levels);
        }

        @Override
        public int createFramebuffer() {
            int framebuffer = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, framebuffer);
            return framebuffer;
        }

        @Override
        public int createTexture(int target) {
            int texture = GL11.glGenTextures();
            bindTextureForTarget(target, texture);
            return texture;
        }
    }

    public static boolean supportsCompute() {
        return GL.getCapabilities().glDispatchCompute != MemoryUtil.NULL;
    }
}
