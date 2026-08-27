package re.lilith.aurum.mixin.debug;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.Callback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import re.lilith.aurum.Aurum;

import java.io.PrintStream;

@Mixin(GLUtil.class)
public class MixinLWJGLUtil {
    @Shadow
    private static void printDetail(StringBuilder sb, String type, String message) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Shadow
    private static String getDebugSource(int source) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Shadow
    private static String getDebugType(int type) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Shadow
    private static String getDebugSeverity(int severity) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Shadow
    private static String getTypeARB(int type) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Shadow
    private static String getSeverityARB(int severity) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Shadow
    private static String getSourceARB(int source) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Shadow
    private static String getCategoryAMD(int category) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Shadow
    private static String getSeverityAMD(int severity) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Unique
    private static void aurum$log(StringBuilder sb) {
        Aurum.LOGGER.error(sb.toString(), new Throwable("GL debug callback"));
    }

    /**
     * @reason TODO: remove this once we're stable
     * @author Lunasa
     */
    @Overwrite
    public static @Nullable Callback setupDebugMessageCallback(PrintStream stream) {
//        GLCapabilities caps = GL.getCapabilities();
//
//        if (caps.OpenGL43) {
//            apiLog("[GL] Using OpenGL 4.3 for error logging.");
//            GLDebugMessageCallback proc = GLDebugMessageCallback.create((source, type, id, severity, length, message, userParam) -> {
//                StringBuilder sb = new StringBuilder(300);
//
//                if (severity == GL_DEBUG_SEVERITY_NOTIFICATION || severity == GL_DEBUG_SEVERITY_LOW) return;
//
//                sb.append("[Aurum] OpenGL debug message\n");
//                printDetail(sb, "ID", "0x" + Integer.toHexString(id).toUpperCase());
//                printDetail(sb, "Source", getDebugSource(source));
//                printDetail(sb, "Type", getDebugType(type));
//                printDetail(sb, "Severity", getDebugSeverity(severity));
//                printDetail(sb, "Message", GLDebugMessageCallback.getMessage(length, message));
//
//                aurum$log(sb);
//            });
//            glDebugMessageCallback(proc, NULL);
//            if ((glGetInteger(GL_CONTEXT_FLAGS) & GL_CONTEXT_FLAG_DEBUG_BIT) == 0) {
//                apiLog("[GL] Warning: A non-debug context may not produce any debug output.");
//                glEnable(GL_DEBUG_OUTPUT);
//            }
//            glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
//            return proc;
//        }
//
//        if (caps.GL_KHR_debug) {
//            apiLog("[GL] Using KHR_debug for error logging.");
//            GLDebugMessageCallback proc = GLDebugMessageCallback.create((source, type, id, severity, length, message, userParam) -> {
//                StringBuilder sb = new StringBuilder(300);
//
//                if (severity == GL_DEBUG_SEVERITY_NOTIFICATION || severity == GL_DEBUG_SEVERITY_LOW) return;
//
//                sb.append("[Aurum] OpenGL debug message\n");
//                printDetail(sb, "ID", "0x" + Integer.toHexString(id).toUpperCase());
//                printDetail(sb, "Source", getDebugSource(source));
//                printDetail(sb, "Type", getDebugType(type));
//                printDetail(sb, "Severity", getDebugSeverity(severity));
//                printDetail(sb, "Message", GLDebugMessageCallback.getMessage(length, message));
//
//                aurum$log(sb);
//            });
//            KHRDebug.glDebugMessageCallback(proc, NULL);
//            if (caps.OpenGL30 && (glGetInteger(GL_CONTEXT_FLAGS) & GL_CONTEXT_FLAG_DEBUG_BIT) == 0) {
//                apiLog("[GL] Warning: A non-debug context may not produce any debug output.");
//                glEnable(GL_DEBUG_OUTPUT);
//            }
//            glEnable(KHRDebug.GL_DEBUG_OUTPUT_SYNCHRONOUS);
//            return proc;
//        }
//
//        if (caps.GL_ARB_debug_output) {
//            apiLog("[GL] Using ARB_debug_output for error logging.");
//            GLDebugMessageARBCallback proc = GLDebugMessageARBCallback.create((source, type, id, severity, length, message, userParam) -> {
//                StringBuilder sb = new StringBuilder(300);
//
//                sb.append("[Aurum] ARB_debug_output message\n");
//                printDetail(sb, "ID", "0x" + Integer.toHexString(id).toUpperCase());
//                printDetail(sb, "Source", getSourceARB(source));
//                printDetail(sb, "Type", getTypeARB(type));
//                printDetail(sb, "Severity", getSeverityARB(severity));
//                printDetail(sb, "Message", GLDebugMessageARBCallback.getMessage(length, message));
//
//                aurum$log(sb);
//            });
//            glDebugMessageCallbackARB(proc, NULL);
//            glEnable(ARBDebugOutput.GL_DEBUG_OUTPUT_SYNCHRONOUS_ARB);
//            return proc;
//        }
//
//        if (caps.GL_AMD_debug_output) {
//            apiLog("[GL] Using AMD_debug_output for error logging.");
//            GLDebugMessageAMDCallback proc = GLDebugMessageAMDCallback.create((id, category, severity, length, message, userParam) -> {
//                StringBuilder sb = new StringBuilder(300);
//
//                sb.append("[Aurum] AMD_debug_output message\n");
//                printDetail(sb, "ID", "0x" + Integer.toHexString(id).toUpperCase());
//                printDetail(sb, "Category", getCategoryAMD(category));
//                printDetail(sb, "Severity", getSeverityAMD(severity));
//                printDetail(sb, "Message", GLDebugMessageAMDCallback.getMessage(length, message));
//
//                aurum$log(sb);
//            });
//            glDebugMessageCallbackAMD(proc, NULL);
//            return proc;
//        }
//
//        apiLog("[GL] No debug output implementation is available.");
        return null;
    }
}
