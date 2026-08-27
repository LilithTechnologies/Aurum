package re.lilith.aurum.gl;

import org.lwjgl.opengl.*;
import org.lwjgl.system.APIUtil;
import re.lilith.aurum.Aurum;

import java.io.PrintStream;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public final class GlDebug {
    public static int setupDebugMessageCallback() {
        return setupDebugMessageCallback(APIUtil.DEBUG_STREAM);
    }

    private static void trace(Consumer<String> output) {
        StackTraceElement[] elems = filterStackTrace(new Throwable(), 4).getStackTrace();
        for (StackTraceElement ste : elems) {
            output.accept(ste.toString());
        }
    }

    public static Throwable filterStackTrace(Throwable throwable, int offset) {
        StackTraceElement[] elems = throwable.getStackTrace();
        StackTraceElement[] filtered = new StackTraceElement[elems.length];
        int j = 0;
        for (int i = offset; i < elems.length; i++) {
            filtered[j++] = elems[i];
        }
        StackTraceElement[] newElems = new StackTraceElement[j];
        System.arraycopy(filtered, 0, newElems, 0, j);
        throwable.setStackTrace(newElems);
        return throwable;
    }

    private static void printTrace(PrintStream stream) {
        trace(new Consumer<>() {
            boolean first = true;

            public void accept(String str) {
                if (first) {
                    printDetail(stream, "Stacktrace", str);
                    first = false;
                } else {
                    printDetailLine(stream, str);
                }
            }
        });
    }

    public static int setupDebugMessageCallback(PrintStream stream) {
        GLCapabilities caps = GL.getCapabilities();
        if (caps.OpenGL43) {
            Aurum.LOGGER.info("[GL] Using OpenGL 4.3 for error logging.");
            GLDebugMessageCallback proc = GLDebugMessageCallback.create((source, type, id, severity, length, message, userParam) -> {
                stream.println("[LWJGL] OpenGL debug message");
                printDetail(stream, "ID", String.format("0x%X", id));
                printDetail(stream, "Source", getDebugSource(source));
                printDetail(stream, "Type", getDebugType(type));
                printDetail(stream, "Severity", getDebugSeverity(severity));
                printDetail(stream, "Message", GLDebugMessageCallback.getMessage(length, message));
                printTrace(stream);
            });
            GL43C.glDebugMessageControl(4352, 4352, GL43C.GL_DEBUG_SEVERITY_HIGH, (int[]) null, true);
            GL43C.glDebugMessageControl(4352, 4352, GL43C.GL_DEBUG_SEVERITY_MEDIUM, (int[]) null, false);
            GL43C.glDebugMessageControl(4352, 4352, GL43C.GL_DEBUG_SEVERITY_LOW, (int[]) null, false);
            GL43C.glDebugMessageControl(4352, 4352, GL43C.GL_DEBUG_SEVERITY_NOTIFICATION, (int[]) null, false);
            GL43C.glDebugMessageCallback(proc, 0L);
            if ((GL43C.glGetInteger(33310) & 2) == 0) {
                Aurum.LOGGER.warn("[GL/4.3] Warning: A non-debug context may not produce any debug output.");
                GL43C.glEnable(37600);
                return 2;
            }
            return 1;
        } else if (caps.GL_KHR_debug) {
            Aurum.LOGGER.info("[GL] Using KHR_debug for error logging.");
            GLDebugMessageCallback proc = GLDebugMessageCallback.create((source, type, id, severity, length, message, userParam) -> {
                stream.println("[LWJGL] OpenGL debug message");
                printDetail(stream, "ID", String.format("0x%X", id));
                printDetail(stream, "Source", getDebugSource(source));
                printDetail(stream, "Type", getDebugType(type));
                printDetail(stream, "Severity", getDebugSeverity(severity));
                printDetail(stream, "Message", GLDebugMessageCallback.getMessage(length, message));
                printTrace(stream);
            });
            KHRDebug.glDebugMessageControl(4352, 4352, GL43C.GL_DEBUG_SEVERITY_HIGH, (int[]) null, true);
            KHRDebug.glDebugMessageControl(4352, 4352, GL43C.GL_DEBUG_SEVERITY_MEDIUM, (int[]) null, false);
            KHRDebug.glDebugMessageControl(4352, 4352, GL43C.GL_DEBUG_SEVERITY_LOW, (int[]) null, false);
            KHRDebug.glDebugMessageControl(4352, 4352, GL43C.GL_DEBUG_SEVERITY_NOTIFICATION, (int[]) null, false);
            KHRDebug.glDebugMessageCallback(proc, 0L);
            if (caps.OpenGL30 && (GL43C.glGetInteger(33310) & 2) == 0) {
                Aurum.LOGGER.warn("[GL/KHR_debug] Warning: A non-debug context may not produce any debug output.");
                GL43C.glEnable(37600);
                return 2;
            }
            return 1;
        } else if (caps.GL_ARB_debug_output) {
            Aurum.LOGGER.info("[GL] Using ARB_debug_output for error logging.");
            GLDebugMessageARBCallback proc = GLDebugMessageARBCallback.create((source, type, id, severity, length, message, userParam) -> {
                stream.println("[LWJGL] ARB_debug_output message");
                printDetail(stream, "ID", String.format("0x%X", id));
                printDetail(stream, "Source", getSourceARB(source));
                printDetail(stream, "Type", getTypeARB(type));
                printDetail(stream, "Severity", getSeverityARB(severity));
                printDetail(stream, "Message", GLDebugMessageARBCallback.getMessage(length, message));
                printTrace(stream);
            });
            ARBDebugOutput.glDebugMessageControlARB(4352, 4352, GL43C.GL_DEBUG_SEVERITY_HIGH, (int[]) null, true);
            ARBDebugOutput.glDebugMessageControlARB(4352, 4352, GL43C.GL_DEBUG_SEVERITY_MEDIUM, (int[]) null, false);
            ARBDebugOutput.glDebugMessageControlARB(4352, 4352, GL43C.GL_DEBUG_SEVERITY_LOW, (int[]) null, false);
            ARBDebugOutput.glDebugMessageControlARB(4352, 4352, GL43C.GL_DEBUG_SEVERITY_NOTIFICATION, (int[]) null, false);
            ARBDebugOutput.glDebugMessageCallbackARB(proc, 0L);
            return 1;
        } else if (caps.GL_AMD_debug_output) {
            Aurum.LOGGER.info("[GL] Using AMD_debug_output for error logging.");
            GLDebugMessageAMDCallback proc = GLDebugMessageAMDCallback.create((id, category, severity, length, message, userParam) -> {
                stream.println("[LWJGL] AMD_debug_output message");
                printDetail(stream, "ID", String.format("0x%X", id));
                printDetail(stream, "Category", getCategoryAMD(category));
                printDetail(stream, "Severity", getSeverityAMD(severity));
                printDetail(stream, "Message", GLDebugMessageAMDCallback.getMessage(length, message));
                printTrace(stream);
            });
            AMDDebugOutput.glDebugMessageEnableAMD(0, GL43C.GL_DEBUG_SEVERITY_HIGH, (int[]) null, true);
            AMDDebugOutput.glDebugMessageEnableAMD(0, GL43C.GL_DEBUG_SEVERITY_MEDIUM, (int[]) null, false);
            AMDDebugOutput.glDebugMessageEnableAMD(0, GL43C.GL_DEBUG_SEVERITY_LOW, (int[]) null, false);
            AMDDebugOutput.glDebugMessageEnableAMD(0, GL43C.GL_DEBUG_SEVERITY_NOTIFICATION, (int[]) null, false);
            AMDDebugOutput.glDebugMessageCallbackAMD(proc, 0L);
            return 1;
        } else {
            Aurum.LOGGER.info("[GL/AMD_debug_output] No debug output implementation is available, cannot return debug info.");
            return 0;
        }
    }

    private static void printDetail(PrintStream stream, String type, String message) {
        stream.printf("\t%s: %s\n", type, message);
    }

    private static void printDetailLine(PrintStream stream, String message) {
        stream.append("    ");
        for (int i = 0; i < "Stacktrace".length(); i++) {
            stream.append(" ");
        }
        stream.append(message).append("\n");
    }

    private static String getDebugSource(int source) {
        return switch (source) {
            case 33350 -> "API";
            case 33351 -> "WINDOW SYSTEM";
            case 33352 -> "SHADER COMPILER";
            case 33353 -> "THIRD PARTY";
            case 33354 -> "APPLICATION";
            case 33355 -> "OTHER";
            default -> APIUtil.apiUnknownToken(source);
        };
    }

    private static String getDebugType(int type) {
        return switch (type) {
            case 33356 -> "ERROR";
            case 33357 -> "DEPRECATED BEHAVIOR";
            case 33358 -> "UNDEFINED BEHAVIOR";
            case 33359 -> "PORTABILITY";
            case 33360 -> "PERFORMANCE";
            case 33361 -> "OTHER";
            case 33384 -> "MARKER";
            default -> APIUtil.apiUnknownToken(type);
        };
    }

    private static String getDebugSeverity(int severity) {
        return switch (severity) {
            case 33387 -> "NOTIFICATION";
            case 37190 -> "HIGH";
            case 37191 -> "MEDIUM";
            case 37192 -> "LOW";
            default -> APIUtil.apiUnknownToken(severity);
        };
    }

    private static String getSourceARB(int source) {
        return switch (source) {
            case 33350 -> "API";
            case 33351 -> "WINDOW SYSTEM";
            case 33352 -> "SHADER COMPILER";
            case 33353 -> "THIRD PARTY";
            case 33354 -> "APPLICATION";
            case 33355 -> "OTHER";
            default -> APIUtil.apiUnknownToken(source);
        };
    }

    private static String getTypeARB(int type) {
        return switch (type) {
            case 33356 -> "ERROR";
            case 33357 -> "DEPRECATED BEHAVIOR";
            case 33358 -> "UNDEFINED BEHAVIOR";
            case 33359 -> "PORTABILITY";
            case 33360 -> "PERFORMANCE";
            case 33361 -> "OTHER";
            default -> APIUtil.apiUnknownToken(type);
        };
    }

    private static String getSeverityARB(int severity) {
        return switch (severity) {
            case 37190 -> "HIGH";
            case 37191 -> "MEDIUM";
            case 37192 -> "LOW";
            default -> APIUtil.apiUnknownToken(severity);
        };
    }

    private static String getCategoryAMD(int category) {
        return switch (category) {
            case 37193 -> "API ERROR";
            case 37194 -> "WINDOW SYSTEM";
            case 37195 -> "DEPRECATION";
            case 37196 -> "UNDEFINED BEHAVIOR";
            case 37197 -> "PERFORMANCE";
            case 37198 -> "SHADER COMPILER";
            case 37199 -> "APPLICATION";
            case 37200 -> "OTHER";
            default -> APIUtil.apiUnknownToken(category);
        };
    }

    private static String getSeverityAMD(int severity) {
        return switch (severity) {
            case 37190 -> "HIGH";
            case 37191 -> "MEDIUM";
            case 37192 -> "LOW";
            default -> APIUtil.apiUnknownToken(severity);
        };
    }

    private static DebugState debugState;

    private interface DebugState {
        void nameObject(int id, int object, String name);

        void pushGroup(int id, String name);

        void popGroup();
    }

    private static class KHRDebugState implements DebugState {
        private boolean hasGroup;

        @Override
        public void nameObject(int id, int object, String name) {
            KHRDebug.glObjectLabel(id, object, name);
        }

        @Override
        public void pushGroup(int id, String name) {
            KHRDebug.glPushDebugGroup(KHRDebug.GL_DEBUG_SOURCE_APPLICATION, id, name);
            hasGroup = true;
        }

        @Override
        public void popGroup() {
            if (hasGroup) {
                KHRDebug.glPopDebugGroup();
                hasGroup = false;
            }
        }
    }

    private static class UnsupportedDebugState implements DebugState {
        @Override
        public void nameObject(int id, int object, String name) {
        }

        @Override
        public void pushGroup(int id, String name) {
        }

        @Override
        public void popGroup() {
        }
    }

    public static void initRenderer() {
        if (GL.getCapabilities().GL_KHR_debug || GL.getCapabilities().OpenGL43) {
            debugState = new KHRDebugState();
        } else {
            debugState = new UnsupportedDebugState();
        }
    }

    public static void nameObject(int id, int object, String name) {
        debugState.nameObject(id, object, name);
    }
}
