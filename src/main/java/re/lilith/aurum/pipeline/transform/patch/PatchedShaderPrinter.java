package re.lilith.aurum.pipeline.transform.patch;

import net.fabricmc.loader.api.FabricLoader;
import re.lilith.aurum.Aurum;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Static class that deals with printing the patched_shader folder.
 */
public class PatchedShaderPrinter {
    private static boolean outputLocationCleared = false;
    private static int programCounter = 0;
    public static final boolean prettyPrintShaders = FabricLoader.getInstance().isDevelopmentEnvironment()
            || System.getProperty("aurum.prettyPrintShaders", "false").equals("true");

    public static void resetPrintState() {
        outputLocationCleared = false;
        programCounter = 0;
    }

    public static void debugPatchedShaders(String name, String vertex, String geometry, String fragment) {
        if (prettyPrintShaders) {
            final Path debugOutDir = FabricLoader.getInstance().getGameDir().resolve("patched_shaders");
            if (!outputLocationCleared) {
                try {
                    if (Files.exists(debugOutDir)) {
                        try (Stream<Path> stream = Files.list(debugOutDir)) {
                            stream.forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                        }
                    }

                    Files.createDirectories(debugOutDir);
                } catch (IOException e) {
                    Aurum.LOGGER.warn("Failed to initialize debug patched shader source location", e);
                }
                outputLocationCleared = true;
            }

            try {
                programCounter++;
                String prefix = String.format("%03d_", programCounter);
                if (vertex != null) {
                    Files.writeString(debugOutDir.resolve(prefix + name + ".vsh"), vertex);
                }
                if (geometry != null) {
                    Files.writeString(debugOutDir.resolve(prefix + name + ".gsh"), geometry);
                }
                if (fragment != null) {
                    Files.writeString(debugOutDir.resolve(prefix + name + ".fsh"), fragment);
                }
            } catch (IOException e) {
                Aurum.LOGGER.warn("Failed to write debug patched shader source", e);
            }
        }
    }
}
