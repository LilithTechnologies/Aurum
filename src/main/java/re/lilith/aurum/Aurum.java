package re.lilith.aurum;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.world.dimension.TheEndDimension;
import net.minecraft.world.dimension.TheNetherDimension;
import net.ornithemc.osl.keybinds.api.KeybindRegistry;
import net.ornithemc.osl.lifecycle.api.client.MinecraftClientEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;
import re.lilith.aurum.config.AurumConfig;
import re.lilith.aurum.gl.shader.StandardMacros;
import re.lilith.aurum.gui.screen.ShaderPackScreen;
import re.lilith.aurum.pipeline.PipelineManager;
import re.lilith.aurum.pipeline.WorldRenderingPipeline;
import re.lilith.aurum.pipeline.impl.DeferredWorldRenderingPipeline;
import re.lilith.aurum.pipeline.impl.FixedFunctionWorldRenderingPipeline;
import re.lilith.aurum.shaderpack.DimensionId;
import re.lilith.aurum.shaderpack.OptionalBoolean;
import re.lilith.aurum.shaderpack.ShaderPack;
import re.lilith.aurum.shaderpack.discovery.ShaderpackDirectoryManager;
import re.lilith.aurum.shaderpack.option.OptionSet;
import re.lilith.aurum.shaderpack.option.Profile;
import re.lilith.aurum.shaderpack.option.values.MutableOptionValues;
import re.lilith.aurum.shaderpack.option.values.OptionValues;
import re.lilith.aurum.shaderpack.program.ProgramSet;
import re.lilith.aurum.texture.pbr.PBRTextureManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;
import java.util.zip.ZipException;

public class Aurum {
    public static final String MODID = "aurum";

    public static final String MODNAME = "Aurum";

    public static final Logger LOGGER = LogManager.getLogger(MODNAME);

    private static Path shaderpacksDirectory;
    private static ShaderpackDirectoryManager shaderpacksDirectoryManager;

    private static ShaderPack currentPack;
    private static String currentPackName;
    private static boolean initialized;

    private static PipelineManager pipelineManager;
    private static AurumConfig aurumConfig;
    private static FileSystem zipFileSystem;
    private static KeyBinding reloadKeybind;
    private static KeyBinding toggleShadersKeybind;
    private static KeyBinding shaderpackScreenKeybind;

    private static final Map<String, String> shaderPackOptionQueue = new HashMap<>();
    // Flag variable used when reloading
    // Used in favor of queueDefaultShaderPackOptionValues() for resetting as the
    // behavior is more concrete and therefore is more likely to repair a user's issues
    private static boolean resetShaderPackOptions = false;

    private static Version AURUM_VERSION;
    private static boolean fallback;

    /**
     * Called very early on in Minecraft initialization. At this point we *cannot* safely access OpenGL, but we can do
     * some very basic setup, config loading, and environment checks.
     *
     * <p>This is roughly equivalent to Fabric Loader's ClientModInitializer#onInitializeClient entrypoint, except
     * it's entirely cross-platform & we get to decide its exact semantics.</p>
     *
     * <p>This is called right before options are loaded, so we can add key bindings here.</p>
     */
    public void onEarlyInitialize() {
        ModContainer aurumMod = FabricLoader.getInstance().getModContainer(MODID)
                .orElseThrow(() -> new IllegalStateException("Couldn't find the mod container for Aurum"));

        AURUM_VERSION = aurumMod.getMetadata().getVersion();

        try {
            if (!Files.exists(getShaderpacksDirectory())) {
                Files.createDirectories(getShaderpacksDirectory());
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to create the shaderpacks directory!");
            LOGGER.warn("", e);
        }

        aurumConfig = new AurumConfig(FabricLoader.getInstance().getConfigDir().resolve("aurum.properties"));

        try {
            aurumConfig.initialize();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize Aurum configuration, default values will be used instead");
            LOGGER.error("", e);
        }

        reloadKeybind = KeybindRegistry.register(new KeyBinding("aurum.keybind.reload", Keyboard.KEY_R, "aurum.keybinds"));
        toggleShadersKeybind = KeybindRegistry.register(new KeyBinding("aurum.keybind.toggleShaders", Keyboard.KEY_K, "aurum.keybinds"));
        shaderpackScreenKeybind = KeybindRegistry.register(new KeyBinding("aurum.keybind.shaderPackSelection", Keyboard.KEY_O, "aurum.keybinds"));

        MinecraftClientEvents.TICK_END.register(client -> {
            if (reloadKeybind.isPressed() && !reloadKeybind.wasPressed()) {
                try {
                    Aurum.reload();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if (toggleShadersKeybind.isPressed() && !toggleShadersKeybind.wasPressed()) {
                getAurumConfig().setShadersEnabled(!getAurumConfig().areShadersEnabled());
            }
            if (shaderpackScreenKeybind.isPressed() && !shaderpackScreenKeybind.wasPressed()) {
                client.setScreen(new ShaderPackScreen(null));
            }
        });

        initialized = true;
    }

    public static void onRenderSystemInit() {
        if (!initialized) {
            Aurum.LOGGER.warn("Aurum::onRenderSystemInit was called, but Aurum::onEarlyInitialize was not called." +
                    " Trying to avoid a crash but this is an odd state.");
            return;
        }

        PBRTextureManager.INSTANCE.init();

        // Only load the shader pack when we can access the graphics API
        loadShaderpack();
    }

    /**
     * Called when the title screen is initialized for the first time.
     */
    public static void onLoadingComplete() {
        if (!initialized) {
            Aurum.LOGGER.warn("Aurum::onLoadingComplete was called, but Aurum::onEarlyInitialize was not called." +
                    " Trying to avoid a crash but this is an odd state.");
            return;
        }

        // Initialize the pipeline now so that we don't increase world loading time. Just going to guess that
        // the player is in the overworld.
        // See: https://github.com/IrisShaders/Iris/issues/323
        lastDimension = DimensionId.OVERWORLD;
        Aurum.getPipelineManager().preparePipeline(DimensionId.OVERWORLD);
    }

    public static void toggleShaders(MinecraftClient minecraft, boolean enabled) throws IOException {
        aurumConfig.setShadersEnabled(enabled);
        aurumConfig.save();

        reload();
        if (minecraft.player != null) {
            minecraft.player.sendMessage(enabled ? new TranslatableText("aurum.shaders.toggled", currentPackName) : new TranslatableText("aurum.shaders.disabled"));
        }
    }

    public static void loadShaderpack() {
        if (aurumConfig == null) {
            if (!initialized) {
                throw new IllegalStateException("Aurum::loadShaderpack was called, but Aurum::onInitializeClient wasn't" +
                        " called yet. How did this happen?");
            } else {
                throw new NullPointerException("Aurum.aurumConfig was null unexpectedly");
            }
        }

        if (!aurumConfig.areShadersEnabled()) {
            LOGGER.info("Shaders are disabled because enableShaders is set to false in aurum.properties");

            setShadersDisabled();

            return;
        }

        // Attempt to load an external shaderpack if it is available
        Optional<String> externalName = aurumConfig.getShaderPackName();

        if (externalName.isEmpty()) {
            LOGGER.info("Shaders are disabled because no valid shaderpack is selected");

            setShadersDisabled();

            return;
        }

        if (!loadExternalShaderpack(externalName.get())) {
            LOGGER.warn("Falling back to normal rendering without shaders because the shaderpack could not be loaded");
            setShadersDisabled();
            fallback = true;
        }
    }

    private static boolean loadExternalShaderpack(String name) {
        Path shaderPackRoot;
        Path shaderPackConfigTxt;

        try {
            shaderPackRoot = getShaderpacksDirectory().resolve(name);
            shaderPackConfigTxt = getShaderpacksDirectory().resolve(name + ".txt");
        } catch (InvalidPathException e) {
            LOGGER.error("Failed to load the shaderpack \"{}\" because it contains invalid characters in its path", name);

            return false;
        }

        Path shaderPackPath;

        if (shaderPackRoot.toString().endsWith(".zip")) {
            Optional<Path> optionalPath;

            try {
                optionalPath = loadExternalZipShaderpack(shaderPackRoot);
            } catch (FileSystemNotFoundException | NoSuchFileException e) {
                LOGGER.error("Failed to load the shaderpack \"{}\" because it does not exist in your shaderpacks folder!", name);

                return false;
            } catch (ZipException e) {
                LOGGER.error("The shaderpack \"{}\" appears to be corrupted, please try downloading it again!", name);

                return false;
            } catch (IOException e) {
                LOGGER.error("Failed to load the shaderpack \"{}\"!", name);
                LOGGER.error("", e);

                return false;
            }

            if (optionalPath.isPresent()) {
                shaderPackPath = optionalPath.get();
            } else {
                LOGGER.error("Could not load the shaderpack \"{}\" because it appears to lack a \"shaders\" directory", name);
                return false;
            }
        } else {
            if (!Files.exists(shaderPackRoot)) {
                LOGGER.error("Failed to load the shaderpack \"{}\" because it does not exist!", name);
                return false;
            }

            // If it's a folder-based shaderpack, just use the shaders subdirectory
            shaderPackPath = shaderPackRoot.resolve("shaders");
        }

        if (!Files.exists(shaderPackPath)) {
            LOGGER.error("Could not load the shaderpack \"{}\" because it appears to lack a \"shaders\" directory", name);
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, String> changedConfigs = tryReadConfigProperties(shaderPackConfigTxt)
                .map(properties -> (Map<String, String>) (Object) properties)
                .orElse(new HashMap<>());

        changedConfigs.putAll(shaderPackOptionQueue);
        clearShaderPackOptionQueue();

        if (resetShaderPackOptions) {
            changedConfigs.clear();
        }
        resetShaderPackOptions = false;

        try {
            currentPack = new ShaderPack(shaderPackPath, changedConfigs, StandardMacros.createStandardEnvironmentDefines());

            MutableOptionValues changedConfigsValues = currentPack.getShaderPackOptions().getOptionValues().mutableCopy();

            // Store changed values from those currently in use by the shader pack
            Properties configsToSave = new Properties();
            changedConfigsValues.getBooleanValues().forEach((k, v) -> configsToSave.setProperty(k, Boolean.toString(v)));
            changedConfigsValues.getStringValues().forEach(configsToSave::setProperty);

            tryUpdateConfigPropertiesFile(shaderPackConfigTxt, configsToSave);
        } catch (Exception e) {
            LOGGER.error("Failed to load the external shaderpack \"{}\"!", name);
            LOGGER.error("", e);

            return false;
        }

        fallback = false;
        currentPackName = name;

        LOGGER.info("Using shaderpack: {}", name);

        return true;
    }

    private static Optional<Path> loadExternalZipShaderpack(Path shaderpackPath) throws IOException {
        FileSystem zipSystem = FileSystems.newFileSystem(shaderpackPath, Aurum.class.getClassLoader());
        zipFileSystem = zipSystem;

        // Should only be one root directory for a zip shaderpack
        Path root = zipSystem.getRootDirectories().iterator().next();

        Path potentialShaderDir = zipSystem.getPath("shaders");

        // If the shaders dir was immediately found return it
        // Otherwise, manually search through each directory path until it ends with "shaders"
        if (Files.exists(potentialShaderDir)) {
            return Optional.of(potentialShaderDir);
        }

        // Sometimes shaderpacks have their shaders directory within another folder in the shaderpack
        // For example Sildurs-Vibrant-Shaders.zip/shaders
        // While other packs have Trippy-Shaderpack-master.zip/Trippy-Shaderpack-master/shaders
        // This makes it hard to determine what is the actual shaders dir
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(path -> path.endsWith("shaders"))
                    .findFirst();
        }
    }

    private static void setShadersDisabled() {
        currentPack = null;
        fallback = false;
        currentPackName = "(off)";

        LOGGER.info("Shaders are disabled");
    }

    private static Optional<Properties> tryReadConfigProperties(Path path) {
        Properties properties = new Properties();

        if (Files.exists(path)) {
            try (InputStream is = Files.newInputStream(path)) {
                // NB: config properties are specified to be encoded with ISO-8859-1 by OptiFine,
                //     so we don't need to do the UTF-8 workaround here.
                properties.load(is);
            } catch (IOException e) {
                LOGGER.error(e);
                return Optional.empty();
            }
        }

        return Optional.of(properties);
    }

    private static void tryUpdateConfigPropertiesFile(Path path, Properties properties) {
        try {
            if (properties.isEmpty()) {
                // Delete the file or don't create it if there are no changed configs
                if (Files.exists(path)) {
                    Files.delete(path);
                }

                return;
            }

            try (OutputStream out = Files.newOutputStream(path)) {
                properties.store(out, null);
            }
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    public static boolean isValidShaderpack(Path pack) {
        if (Files.isDirectory(pack)) {
            // Sometimes the shaderpack directory itself can be
            // identified as a shader pack due to it containing
            // folders which contain "shaders" folders, this is
            // necessary to check against that
            if (pack.equals(getShaderpacksDirectory())) {
                return false;
            }
            try (Stream<Path> stream = Files.walk(pack)) {
                return stream
                        .filter(Files::isDirectory)
                        // Prevent a pack simply named "shaders" from being
                        // identified as a valid pack
                        .filter(path -> !path.equals(pack))
                        .anyMatch(path -> path.endsWith("shaders"));
            } catch (IOException ignored) {
                // ignored, not a valid shader pack.
            }
        }

        if (pack.toString().endsWith(".zip")) {
            try (FileSystem zipSystem = FileSystems.newFileSystem(pack, Aurum.class.getClassLoader())) {
                Path root = zipSystem.getRootDirectories().iterator().next();
                try (Stream<Path> stream = Files.walk(root)) {
                    return stream
                            .filter(Files::isDirectory)
                            .anyMatch(path -> path.endsWith("shaders"));
                }
            } catch (ZipException zipError) {
                // Java 8 seems to throw a ZipError instead of a subclass of IOException
                Aurum.LOGGER.warn("The ZIP at {} is corrupt", pack);
            } catch (IOException ignored) {
                // ignored, not a valid shader pack.
            }
        }

        return false;
    }

    public static Map<String, String> getShaderPackOptionQueue() {
        return shaderPackOptionQueue;
    }

    public static void queueShaderPackOptionsFromProfile(Profile profile) {
        getShaderPackOptionQueue().putAll(profile.optionValues);
    }

    public static void queueShaderPackOptionsFromProperties(Properties properties) {
        queueDefaultShaderPackOptionValues();

        properties.stringPropertyNames().forEach(key ->
                getShaderPackOptionQueue().put(key, properties.getProperty(key)));
    }

    // Used in favor of resetShaderPackOptions as the aforementioned requires the pack to be reloaded
    public static void queueDefaultShaderPackOptionValues() {
        clearShaderPackOptionQueue();

        getCurrentPack().ifPresent(pack -> {
            OptionSet options = pack.getShaderPackOptions().getOptionSet();
            OptionValues values = pack.getShaderPackOptions().getOptionValues();

            options.getStringOptions().forEach((key, mOpt) -> {
                if (values.getStringValue(key).isPresent()) {
                    getShaderPackOptionQueue().put(key, mOpt.getOption().getDefaultValue());
                }
            });
            options.getBooleanOptions().forEach((key, mOpt) -> {
                if (values.getBooleanValue(key) != OptionalBoolean.DEFAULT) {
                    getShaderPackOptionQueue().put(key, Boolean.toString(mOpt.getOption().getDefaultValue()));
                }
            });
        });
    }

    public static void clearShaderPackOptionQueue() {
        getShaderPackOptionQueue().clear();
    }

    public static void resetShaderPackOptionsOnNextReload() {
        resetShaderPackOptions = true;
    }

    public static boolean shouldResetShaderPackOptionsOnNextReload() {
        return resetShaderPackOptions;
    }

    public static void reload() throws IOException {
        // allows shaderpacks to be changed at runtime
        aurumConfig.initialize();

        // Destroy all allocated resources
        destroyEverything();

        // Load the new shaderpack
        loadShaderpack();

        // Very important - we need to re-create the pipeline straight away.
        // https://github.com/IrisShaders/Iris/issues/1330
        if (MinecraftClient.getInstance().world != null) {
            Aurum.getPipelineManager().preparePipeline(Aurum.getCurrentDimension());
        }
    }


    /**
     * Destroys and deallocates all created OpenGL resources. Useful as part of a reload.
     */
    private static void destroyEverything() {
        currentPack = null;

        getPipelineManager().destroyPipeline();

        // Close the zip filesystem that the shaderpack was loaded from
        //
        // This prevents a FileSystemAlreadyExistsException when reloading shaderpacks.
        if (zipFileSystem != null) {
            try {
                zipFileSystem.close();
            } catch (NoSuchFileException e) {
                LOGGER.warn("Failed to close the shaderpack zip when reloading because it was deleted, proceeding anyways.");
            } catch (IOException e) {
                LOGGER.error("Failed to close zip file system?", e);
            }
        }
    }

    public static DimensionId lastDimension = null;

    public static DimensionId getCurrentDimension() {
        ClientWorld level = MinecraftClient.getInstance().world;

        if (level != null) {
            if (level.dimension instanceof TheEndDimension) {
                return DimensionId.END;
            } else if (level.dimension instanceof TheNetherDimension) {
                return DimensionId.NETHER;
            } else {
                return DimensionId.OVERWORLD;
            }
        } else {
            // This prevents us from reloading the shaderpack unless we need to. Otherwise, if the player is in the
            // nether and quits the game, we might end up reloading the shaders on exit and on entry to the level
            // because the code thinks that the dimension changed.
            return lastDimension;
        }
    }

    private static WorldRenderingPipeline createPipeline(DimensionId dimensionId) {
        if (currentPack == null) {
            // Completely disables shader-based rendering
            return new FixedFunctionWorldRenderingPipeline();
        }

        ProgramSet programs = currentPack.getProgramSet(dimensionId);

        try {
            return new DeferredWorldRenderingPipeline(programs);
        } catch (Exception e) {
            LOGGER.error("Failed to create shader rendering pipeline, disabling shaders!", e);
            fallback = true;

            return new FixedFunctionWorldRenderingPipeline();
        }
    }

    @NotNull
    public static PipelineManager getPipelineManager() {
        if (pipelineManager == null) {
            pipelineManager = new PipelineManager(Aurum::createPipeline);
        }

        return pipelineManager;
    }

    @NotNull
    public static Optional<ShaderPack> getCurrentPack() {
        return Optional.ofNullable(currentPack);
    }

    public static String getCurrentPackName() {
        return currentPackName;
    }

    public static AurumConfig getAurumConfig() {
        return aurumConfig;
    }

    public static boolean isFallback() {
        return fallback;
    }

    public static String getVersion() {
        if (AURUM_VERSION == null) {
            return "Version info unknown!";
        }

        return AURUM_VERSION.getFriendlyString();
    }

    public static String getFormattedVersion() {
        Formatting color;
        String version = getVersion();

        if (version.endsWith("-development-environment")) {
            color = Formatting.GOLD;
            version = version.replace("-development-environment", " (Development Environment)");
        } else if (version.endsWith("-dirty") || version.contains("unknown") || version.endsWith("-nogit")) {
            color = Formatting.RED;
        } else if (version.contains("+rev.")) {
            color = Formatting.LIGHT_PURPLE;
        } else {
            color = Formatting.GREEN;
        }

        return color + version;
    }

    public static Path getShaderpacksDirectory() {
        if (shaderpacksDirectory == null) {
            shaderpacksDirectory = FabricLoader.getInstance().getGameDir().resolve("shaderpacks");
        }

        return shaderpacksDirectory;
    }

    public static ShaderpackDirectoryManager getShaderpacksDirectoryManager() {
        if (shaderpacksDirectoryManager == null) {
            shaderpacksDirectoryManager = new ShaderpackDirectoryManager(getShaderpacksDirectory());
        }

        return shaderpacksDirectoryManager;
    }
}
