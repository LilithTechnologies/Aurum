package re.lilith.aurum.shaderpack;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.Nullable;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.expression.CustomUniforms;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.blending.*;
import re.lilith.aurum.gl.buffer.ShaderStorageInfo;
import re.lilith.aurum.gl.image.ImageInformation;
import re.lilith.aurum.gl.texture.*;
import re.lilith.aurum.shaderpack.option.ShaderPackOptions;
import re.lilith.aurum.shaderpack.preprocessor.PropertiesPreprocessor;
import re.lilith.aurum.shaderpack.texture.TextureStage;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The parsed representation of the shaders.properties file. This class is not meant to be stored permanently, rather
 * it merely exists as an intermediate step until we build up PackDirectives and ProgramDirectives objects from the
 * values in here & the values parsed from shader source code.
 */
public class ShaderProperties {
    private CloudSetting cloudSetting = CloudSetting.DEFAULT;
    private OptionalBoolean oldHandLight = OptionalBoolean.DEFAULT;
    private OptionalBoolean dynamicHandLight = OptionalBoolean.DEFAULT;
    private OptionalBoolean oldLighting = OptionalBoolean.DEFAULT;
    private OptionalBoolean shadowTerrain = OptionalBoolean.DEFAULT;
    private OptionalBoolean shadowTranslucent = OptionalBoolean.DEFAULT;
    private OptionalBoolean shadowEntities = OptionalBoolean.DEFAULT;
    private OptionalBoolean shadowPlayer = OptionalBoolean.DEFAULT;
    private OptionalBoolean shadowBlockEntities = OptionalBoolean.DEFAULT;
    private OptionalBoolean underwaterOverlay = OptionalBoolean.DEFAULT;
    private OptionalBoolean sun = OptionalBoolean.DEFAULT;
    private OptionalBoolean moon = OptionalBoolean.DEFAULT;
    private OptionalBoolean vignette = OptionalBoolean.DEFAULT;
    private OptionalBoolean weather = OptionalBoolean.DEFAULT;
    private OptionalBoolean weatherParticles = OptionalBoolean.DEFAULT;
    private OptionalBoolean backFaceSolid = OptionalBoolean.DEFAULT;
    private OptionalBoolean backFaceCutout = OptionalBoolean.DEFAULT;
    private OptionalBoolean backFaceCutoutMipped = OptionalBoolean.DEFAULT;
    private OptionalBoolean backFaceTranslucent = OptionalBoolean.DEFAULT;
    private OptionalBoolean rainDepth = OptionalBoolean.DEFAULT;
    private OptionalBoolean concurrentCompute = OptionalBoolean.DEFAULT;
    private OptionalBoolean beaconBeamDepth = OptionalBoolean.DEFAULT;
    private OptionalBoolean separateAo = OptionalBoolean.DEFAULT;
    private OptionalBoolean frustumCulling = OptionalBoolean.DEFAULT;
    private ShadowCullState shadowCulling = ShadowCullState.DEFAULT;
    private OptionalBoolean shadowEnabled = OptionalBoolean.DEFAULT;
    private OptionalBoolean particlesBeforeDeferred = OptionalBoolean.DEFAULT;
    @Nullable
    private ParticleRenderingSettings particleRenderingSettings;
    private OptionalBoolean prepareBeforeShadow = OptionalBoolean.DEFAULT;
    private List<String> sliderOptions = new ArrayList<>();
    private final Map<String, List<String>> profiles = new LinkedHashMap<>();
    private List<String> mainScreenOptions = null;
    private final Map<String, List<String>> subScreenOptions = new HashMap<>();
    private Integer mainScreenColumnCount = null;
    private final Map<String, Integer> subScreenColumnCount = new HashMap<>();
    private final CustomUniforms.Builder customUniforms = new CustomUniforms.Builder();
    private final Object2ObjectMap<String, AlphaTestOverride> alphaTestOverrides = new Object2ObjectOpenHashMap<>();
    private final Object2FloatMap<String> viewportScaleOverrides = new Object2FloatOpenHashMap<>();
    private final Object2ObjectMap<String, TextureScaleOverride> textureScaleOverrides = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectMap<String, BlendModeOverride> blendModeOverrides = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectMap<String, IndirectPointer> indirectPointers = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectMap<String, ArrayList<BufferBlendInformation>> bufferBlendOverrides = new Object2ObjectOpenHashMap<>();
    private final EnumMap<TextureStage, Object2ObjectMap<String, String>> customTextures = new EnumMap<>(TextureStage.class);
    private final Object2ObjectMap<String, Object2BooleanMap<String>> explicitFlips = new Object2ObjectOpenHashMap<>();
    private String noiseTexturePath = null;
    private final Object2ObjectMap<String, String> conditionallyEnabledPrograms = new Object2ObjectOpenHashMap<>();
    private List<String> requiredFeatureFlags = new ArrayList<>();
    private List<String> optionalFeatureFlags = new ArrayList<>();
    private final Int2ObjectMap<ShaderStorageInfo> bufferObjects = new Int2ObjectOpenHashMap<>();
    private final Object2ObjectMap<String, ImageInformation> customImages = new Object2ObjectOpenHashMap<>();

    private ShaderProperties() {
        // empty
    }

    // TODO: Is there a better solution than having ShaderPack pass a root path to ShaderProperties to be able to read textures?
    public ShaderProperties(String contents, ShaderPackOptions shaderPackOptions, Iterable<StringPair> environmentDefines) {
        String preprocessedContents = PropertiesPreprocessor.preprocessSource(contents, shaderPackOptions, environmentDefines);

        Properties preprocessed = new OrderBackedProperties();
        Properties original = new OrderBackedProperties();
        try {
            preprocessed.load(new StringReader(preprocessedContents));
            original.load(new StringReader(contents));
        } catch (IOException e) {
            Aurum.LOGGER.error("Error loading shaders.properties!", e);
        }

        preprocessed.forEach((keyObject, valueObject) -> {
            String key = (String) keyObject;
            String value = (String) valueObject;

            if ("texture.noise".equals(key)) {
                noiseTexturePath = value;
                return;
            }

            if ("clouds".equals(key)) {
                if ("off".equals(value)) {
                    cloudSetting = CloudSetting.OFF;
                } else if ("fast".equals(value)) {
                    cloudSetting = CloudSetting.FAST;
                } else if ("fancy".equals(value)) {
                    cloudSetting = CloudSetting.FANCY;
                } else {
                    Aurum.LOGGER.error("Unrecognized clouds setting: " + value);
                }
            }

            handleBooleanDirective(key, value, "oldHandLight", bool -> oldHandLight = bool);
            handleBooleanDirective(key, value, "dynamicHandLight", bool -> dynamicHandLight = bool);
            handleBooleanDirective(key, value, "oldLighting", bool -> oldLighting = bool);
            handleBooleanDirective(key, value, "shadowTerrain", bool -> shadowTerrain = bool);
            handleBooleanDirective(key, value, "shadowTranslucent", bool -> shadowTranslucent = bool);
            handleBooleanDirective(key, value, "shadowEntities", bool -> shadowEntities = bool);
            handleBooleanDirective(key, value, "shadowPlayer", bool -> shadowPlayer = bool);
            handleBooleanDirective(key, value, "shadowBlockEntities", bool -> shadowBlockEntities = bool);
            handleBooleanDirective(key, value, "underwaterOverlay", bool -> underwaterOverlay = bool);
            handleBooleanDirective(key, value, "sun", bool -> sun = bool);
            handleBooleanDirective(key, value, "moon", bool -> moon = bool);
            handleBooleanDirective(key, value, "vignette", bool -> vignette = bool);

            if ("weather".equals(key)) {
                String[] parts = value.split(" ");

                weather = "true".equals(parts[0]) ? OptionalBoolean.TRUE : OptionalBoolean.FALSE;

                if (parts.length > 1) {
                    weatherParticles = "true".equals(parts[1]) ? OptionalBoolean.TRUE : OptionalBoolean.FALSE;
                }
            }

            handleBooleanDirective(key, value, "backFace.solid", bool -> backFaceSolid = bool);
            handleBooleanDirective(key, value, "backFace.cutout", bool -> backFaceCutout = bool);
            handleBooleanDirective(key, value, "backFace.cutoutMipped", bool -> backFaceCutoutMipped = bool);
            handleBooleanDirective(key, value, "backFace.translucent", bool -> backFaceTranslucent = bool);
            handleBooleanDirective(key, value, "rain.depth", bool -> rainDepth = bool);
            handleBooleanDirective(key, value, "allowConcurrentCompute", bool -> concurrentCompute = bool);
            handleBooleanDirective(key, value, "beacon.beam.depth", bool -> beaconBeamDepth = bool);
            handleBooleanDirective(key, value, "separateAo", bool -> separateAo = bool);
            handleBooleanDirective(key, value, "frustum.culling", bool -> frustumCulling = bool);

            if ("shadow.culling".equals(key)) {
                if ("false".equals(value)) {
                    shadowCulling = ShadowCullState.DISTANCE;
                } else if ("true".equals(value)) {
                    shadowCulling = ShadowCullState.ADVANCED;
                } else if ("reversed".equals(value) || "safe_zone".equals(value)) {
                    shadowCulling = ShadowCullState.SAFE_ZONE;
                } else {
                    Aurum.LOGGER.error("Unrecognized shadow culling setting: " + value);
                }
            }

            handleBooleanDirective(key, value, "shadow.enabled", bool -> shadowEnabled = bool);
            handleBooleanDirective(key, value, "particles.before.deferred", bool -> particlesBeforeDeferred = bool);

            if (key.startsWith("particles.ordering")) {
                Optional<ParticleRenderingSettings> settings = ParticleRenderingSettings.fromString(value.trim().toUpperCase(Locale.ROOT));
                settings.ifPresent(s -> particleRenderingSettings = s);
            }
            handleBooleanDirective(key, value, "prepareBeforeShadow", bool -> prepareBeforeShadow = bool);

            // TODO: Min optifine versions, shader options layout / appearance / profiles

            handlePassDirective("variable.", key, value, pass -> {
                String[] parts = pass.split("\\.");
                if (parts.length != 2) {
                    Aurum.LOGGER.warn("Custom variables should take the form of `variable.<type>.<name> = <expression>. Ignoring " + key);
                    return;
                }

                customUniforms.addVariable(parts[0], parts[1], value, false);
            });

            handlePassDirective("uniform.", key, value, pass -> {
                String[] parts = pass.split("\\.");
                if (parts.length != 2) {
                    Aurum.LOGGER.warn("Custom uniforms should take the form of `uniform.<type>.<name> = <expression>. Ignoring " + key);
                    return;
                }

                customUniforms.addVariable(parts[0], parts[1], value, true);
            });

            handlePassDirective("scale.", key, value, pass -> {
                float scale;

                try {
                    scale = Float.parseFloat(value);
                } catch (NumberFormatException e) {
                    Aurum.LOGGER.error("Unable to parse scale directive for " + pass + ": " + value, e);
                    return;
                }

                viewportScaleOverrides.put(pass, scale);
            });

            handlePassDirective("size.buffer.", key, value, pass -> {
                String[] parts = value.split(" ");

                if (parts.length != 2) {
                    Aurum.LOGGER.error("Unable to parse size.buffer directive for " + pass + ": " + value);
                    return;
                }

                textureScaleOverrides.put(pass, new TextureScaleOverride(parts[0], parts[1]));
            });

            handlePassDirective("bufferObject.", key, value, indexStr -> parseBufferObject(indexStr, value));

            handlePassDirective("image.", key, value, imageName -> parseCustomImage(imageName, value));

            handlePassDirective("alphaTest.", key, value, pass -> {
                if ("off".equals(value)) {
                    alphaTestOverrides.put(pass, AlphaTestOverride.OFF);
                    return;
                }

                String[] parts = value.split(" ");

                if (parts.length > 2) {
                    Aurum.LOGGER.warn("Weird alpha test directive for " + pass + " contains more parts than we expected: " + value);
                } else if (parts.length < 2) {
                    Aurum.LOGGER.error("Invalid alpha test directive for " + pass + ": " + value);
                    return;
                }

                Optional<AlphaTestFunction> function = AlphaTestFunction.fromString(parts[0]);

                if (!function.isPresent()) {
                    Aurum.LOGGER.error("Unable to parse alpha test directive for " + pass + ", unknown alpha test function " + parts[0] + ": " + value);
                    return;
                }

                float reference;

                try {
                    reference = Float.parseFloat(parts[1]);
                } catch (NumberFormatException e) {
                    Aurum.LOGGER.error("Unable to parse alpha test directive for " + pass + ": " + value, e);
                    return;
                }

                alphaTestOverrides.put(pass, new AlphaTestOverride(new AlphaTest(function.get(), reference)));
            });

            handlePassDirective("blend.", key, value, pass -> {
                if (pass.contains(".")) {

                    if (!AurumRenderSystem.supportsBufferBlending()) {
                        throw new RuntimeException("Buffer blending is not supported on this platform, however it was attempted to be used!");
                    }

                    String[] parts = pass.split("\\.");
                    int index = PackRenderTargetDirectives.LEGACY_RENDER_TARGETS.indexOf(parts[1]);

                    if (index == -1 && parts[1].startsWith("colortex")) {
                        String id = parts[1].substring("colortex".length());

                        try {
                            index = Integer.parseInt(id);
                        } catch (NumberFormatException e) {
                            throw new RuntimeException("Failed to parse buffer blend!", e);
                        }
                    }

                    if (index == -1) {
                        throw new RuntimeException("Failed to parse buffer blend! index = " + index);
                    }

                    if ("off".equals(value)) {
                        bufferBlendOverrides.computeIfAbsent(parts[0], list -> new ArrayList<>()).add(new BufferBlendInformation(index, null));
                        return;
                    }

                    String[] modeArray = value.split(" ");
                    int[] modes = new int[modeArray.length];

                    int i = 0;
                    for (String modeName : modeArray) {
                        modes[i] = BlendModeFunction.fromString(modeName).get().getGlId();
                        i++;
                    }

                    bufferBlendOverrides.computeIfAbsent(parts[0], list -> new ArrayList<>()).add(new BufferBlendInformation(index, new BlendMode(modes[0], modes[1], modes[2], modes[3])));

                    return;
                }

                if ("off".equals(value)) {
                    blendModeOverrides.put(pass, BlendModeOverride.OFF);
                    return;
                }

                String[] modeArray = value.split(" ");
                int[] modes = new int[modeArray.length];

                int i = 0;
                for (String modeName : modeArray) {
                    modes[i] = BlendModeFunction.fromString(modeName).get().getGlId();
                    i++;
                }

                blendModeOverrides.put(pass, new BlendModeOverride(new BlendMode(modes[0], modes[1], modes[2], modes[3])));
            });

            handleProgramEnabledDirective("program.", key, value, program -> {
                conditionallyEnabledPrograms.put(program, value);
            });

            handlePassDirective("indirect.", key, value, pass -> {
                try {
                    String[] locations = value.split(" ");
                    indirectPointers.put(pass, new IndirectPointer(Integer.parseInt(locations[0]), Long.parseLong(locations[1])));
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    Aurum.LOGGER.fatal("Failed to parse indirect command for " + pass + "! " + value);
                }
            });

            handleTwoArgDirective("texture.", key, value, (stageName, samplerName) -> {
                String[] parts = value.split(" ");

                // TODO: Support raw textures
                if (parts.length > 1) {
                    Aurum.LOGGER.warn("Custom texture directive for stage " + stageName + ", sampler " + samplerName + " contains more parts than we expected: " + value);
                    return;
                }

                Optional<TextureStage> optionalTextureStage = TextureStage.parse(stageName);

                if (!optionalTextureStage.isPresent()) {
                    Aurum.LOGGER.warn("Unknown texture stage " + "\"" + stageName + "\"," + " ignoring custom texture directive for " + key);
                    return;
                }

                TextureStage stage = optionalTextureStage.get();

                customTextures.computeIfAbsent(stage, _stage -> new Object2ObjectOpenHashMap<>())
                        .put(samplerName, value);
            });

            handleTwoArgDirective("flip.", key, value, (pass, buffer) -> {
                handleBooleanValue(key, value, shouldFlip -> {
                    explicitFlips.computeIfAbsent(pass, _pass -> new Object2BooleanOpenHashMap<>())
                            .put(buffer, shouldFlip);
                });
            });


            // "iris.features.*" is the stable, externally-authored directive shader packs actually use to
            // detect mod capabilities - this is a wire contract, not something to rebrand along with the mod.
            handleWhitespacedListDirective(key, value, "iris.features.required", options -> requiredFeatureFlags = options);
            handleWhitespacedListDirective(key, value, "iris.features.optional", options -> optionalFeatureFlags = options);

            // TODO: Buffer size directives
            // TODO: Conditional program enabling directives
        });

        // We need to use a non-preprocessed property file here since we don't want any weird preprocessor changes to be applied to the screen/value layout.
        original.forEach((keyObject, valueObject) -> {
            String key = (String) keyObject;
            String value = (String) valueObject;

            // Defining "sliders" multiple times in the properties file will only result in
            // the last definition being used, should be tested if behavior matches OptiFine
            handleWhitespacedListDirective(key, value, "sliders", sliders -> sliderOptions = sliders);
            handlePrefixedWhitespacedListDirective("profile.", key, value, profiles::put);

            if (handleIntDirective(key, value, "screen.columns", columns -> mainScreenColumnCount = columns)) {
                return;
            }

            if (handleAffixedIntDirective("screen.", ".columns", key, value, subScreenColumnCount::put)) {
                return;
            }

            handleWhitespacedListDirective(key, value, "screen", options -> mainScreenOptions = options);
            handlePrefixedWhitespacedListDirective("screen.", key, value, subScreenOptions::put);
        });
    }

    private static void handleBooleanValue(String key, String value, BooleanConsumer handler) {
        if ("true".equals(value)) {
            handler.accept(true);
        } else if ("false".equals(value)) {
            handler.accept(false);
        } else {
            Aurum.LOGGER.warn("Unexpected value for boolean key " + key + " in shaders.properties: got " + value + ", but expected either true or false");
        }
    }

    private static void handleBooleanDirective(String key, String value, String expectedKey, Consumer<OptionalBoolean> handler) {
        if (!expectedKey.equals(key)) {
            return;
        }

        if ("true".equals(value)) {
            handler.accept(OptionalBoolean.TRUE);
        } else if ("false".equals(value)) {
            handler.accept(OptionalBoolean.FALSE);
        } else {
            Aurum.LOGGER.warn("Unexpected value for boolean key " + key + " in shaders.properties: got " + value + ", but expected either true or false");
        }
    }

    private static boolean handleIntDirective(String key, String value, String expectedKey, Consumer<Integer> handler) {
        if (!expectedKey.equals(key)) {
            return false;
        }

        try {
            int result = Integer.parseInt(value);

            handler.accept(result);
        } catch (NumberFormatException nex) {
            Aurum.LOGGER.warn("Unexpected value for integer key " + key + " in shaders.properties: got " + value + ", but expected an integer");
        }

        return true;
    }

    private static boolean handleAffixedIntDirective(String prefix, String suffix, String key, String value, BiConsumer<String, Integer> handler) {
        if (key.startsWith(prefix) && key.endsWith(suffix)) {
            int substrBegin = prefix.length();
            int substrEnd = key.length() - suffix.length();

            if (substrEnd <= substrBegin) {
                return false;
            }

            String affixStrippedKey = key.substring(substrBegin, substrEnd);

            try {
                int result = Integer.parseInt(value);

                handler.accept(affixStrippedKey, result);
            } catch (NumberFormatException nex) {
                Aurum.LOGGER.warn("Unexpected value for integer key " + key + " in shaders.properties: got " + value + ", but expected an integer");
            }

            return true;
        }

        return false;
    }

    private static void handlePassDirective(String prefix, String key, String value, Consumer<String> handler) {
        if (key.startsWith(prefix)) {
            String pass = key.substring(prefix.length());

            handler.accept(pass);
        }
    }

    private void parseBufferObject(String indexStr, String value) {
        try {
            final int index = Integer.parseInt(indexStr);

            if (index > 8) {
                Aurum.LOGGER.error("SSBO index " + index + " exceeds maximum of 8, buffers 9+ are reserved");
                return;
            }

            final String[] parts = value.split("\\s+");

            if (parts.length < 1) {
                Aurum.LOGGER.error("Invalid buffer object directive for index " + index + ": expected size");
                return;
            }

            final long size = Long.parseLong(parts[0]);

            if (size < 1) {
                return;
            }

            ShaderStorageInfo info;

            if (parts.length >= 4 && "true".equalsIgnoreCase(parts[1])) {
                float scaleX = Float.parseFloat(parts[2]);
                float scaleY = Float.parseFloat(parts[3]);
                info = new ShaderStorageInfo(size, true, scaleX, scaleY);
            } else {
                info = new ShaderStorageInfo(size, false, 1.0f, 1.0f);
            }

            bufferObjects.put(index, info);
        } catch (NumberFormatException e) {
            Aurum.LOGGER.error("Failed to parse buffer object " + indexStr + ": " + e.getMessage());
        }
    }

    private void parseCustomImage(String imageName, String value) {
        if (customImages.size() >= 16) {
            Aurum.LOGGER.error("Maximum of 16 custom images exceeded, cannot add: " + imageName);
            return;
        }

        final String[] parts = value.split("\\s+");

        if (parts.length < 6) {
            Aurum.LOGGER.error("Invalid custom image directive for " + imageName + ": expected at least 6 parts, got " + parts.length);
            return;
        }

        try {
            String samplerName = parts[0];
            if (samplerName.equals("none") || samplerName.equals("null") || samplerName.isEmpty()) {
                samplerName = null;
            }

            final Optional<PixelFormat> pixelFormatOpt = PixelFormat.fromString(parts[1].toUpperCase());
            if (pixelFormatOpt.isEmpty()) {
                Aurum.LOGGER.error("Invalid pixel format for custom image " + imageName + ": " + parts[1]);
                return;
            }
            final PixelFormat pixelFormat = pixelFormatOpt.get();

            final Optional<InternalTextureFormat> internalFormatOpt = InternalTextureFormat.fromString(parts[2].toUpperCase());
            if (internalFormatOpt.isEmpty()) {
                Aurum.LOGGER.error("Invalid internal format for custom image " + imageName + ": " + parts[2]);
                return;
            }
            final InternalTextureFormat internalFormat = internalFormatOpt.get();

            final Optional<PixelType> pixelTypeOpt = PixelType.fromString(parts[3].toUpperCase());
            if (pixelTypeOpt.isEmpty()) {
                Aurum.LOGGER.error("Invalid pixel type for custom image " + imageName + ": " + parts[3]);
                return;
            }
            final PixelType pixelType = pixelTypeOpt.get();

            final boolean clear = Boolean.parseBoolean(parts[4]);
            final boolean relative = Boolean.parseBoolean(parts[5]);

            ImageInformation imageInfo;

            if (parts.length == 7) {
                int width = Integer.parseInt(parts[6]);
                imageInfo = new ImageInformation(imageName, samplerName, TextureType.TEXTURE_1D, pixelFormat, internalFormat, pixelType, width, 1, 1, clear, false, 1.0f, 1.0f);
            } else if (parts.length == 8) {
                if (relative) {
                    float relativeWidth = Float.parseFloat(parts[6]);
                    float relativeHeight = Float.parseFloat(parts[7]);
                    imageInfo = new ImageInformation(imageName, samplerName, TextureType.TEXTURE_2D, pixelFormat, internalFormat, pixelType, 0, 0, 1, clear, true, relativeWidth, relativeHeight);
                } else {
                    int width = Integer.parseInt(parts[6]);
                    int height = Integer.parseInt(parts[7]);
                    imageInfo = new ImageInformation(imageName, samplerName, TextureType.TEXTURE_2D, pixelFormat, internalFormat, pixelType, width, height, 1, clear, false, 1.0f, 1.0f);
                }
            } else if (parts.length >= 9) {
                int width = Integer.parseInt(parts[6]);
                int height = Integer.parseInt(parts[7]);
                int depth = Integer.parseInt(parts[8]);
                imageInfo = new ImageInformation(imageName, samplerName, TextureType.TEXTURE_3D, pixelFormat, internalFormat, pixelType, width, height, depth, clear, false, 1.0f, 1.0f);
            } else {
                Aurum.LOGGER.error("Invalid custom image directive for " + imageName + ": expected dimensions");
                return;
            }

            customImages.put(imageName, imageInfo);
        } catch (NumberFormatException e) {
            Aurum.LOGGER.error("Failed to parse custom image " + imageName + ": " + e.getMessage());
        }
    }

    private static void handleProgramEnabledDirective(String prefix, String key, String value, Consumer<String> handler) {
        if (key.startsWith(prefix)) {
            String program = key.substring(prefix.length(), key.indexOf(".", prefix.length()));

            handler.accept(program);
        }
    }

    private static void handleWhitespacedListDirective(String key, String value, String expectedKey, Consumer<List<String>> handler) {
        if (!expectedKey.equals(key)) {
            return;
        }

        String[] elements = value.split(" +");

        handler.accept(Arrays.asList(elements));
    }

    private static void handlePrefixedWhitespacedListDirective(String prefix, String key, String value, BiConsumer<String, List<String>> handler) {
        if (key.startsWith(prefix)) {
            String prefixStrippedKey = key.substring(prefix.length());
            String[] elements = value.split(" +");

            handler.accept(prefixStrippedKey, Arrays.asList(elements));
        }
    }

    private static void handleTwoArgDirective(String prefix, String key, String value, BiConsumer<String, String> handler) {
        if (key.startsWith(prefix)) {
            int endOfPassIndex = key.indexOf(".", prefix.length());
            String stage = key.substring(prefix.length(), endOfPassIndex);
            String sampler = key.substring(endOfPassIndex + 1);

            handler.accept(stage, sampler);
        }
    }

    public static ShaderProperties empty() {
        return new ShaderProperties();
    }

    public CloudSetting getCloudSetting() {
        return cloudSetting;
    }

    public CustomUniforms.Builder getCustomUniforms() {
        return customUniforms;
    }

    public OptionalBoolean getOldHandLight() {
        return oldHandLight;
    }

    public OptionalBoolean getDynamicHandLight() {
        return dynamicHandLight;
    }

    public OptionalBoolean getOldLighting() {
        return oldLighting;
    }

    public OptionalBoolean getShadowTerrain() {
        return shadowTerrain;
    }

    public OptionalBoolean getShadowTranslucent() {
        return shadowTranslucent;
    }

    public OptionalBoolean getShadowEntities() {
        return shadowEntities;
    }

    public OptionalBoolean getShadowPlayer() {
        return shadowPlayer;
    }

    public OptionalBoolean getShadowBlockEntities() {
        return shadowBlockEntities;
    }

    public OptionalBoolean getUnderwaterOverlay() {
        return underwaterOverlay;
    }

    public OptionalBoolean getSun() {
        return sun;
    }

    public OptionalBoolean getMoon() {
        return moon;
    }

    public OptionalBoolean getVignette() {
        return vignette;
    }

    public OptionalBoolean getWeather() {
        return weather;
    }

    public OptionalBoolean getWeatherParticles() {
        return weatherParticles;
    }

    public OptionalBoolean getBackFaceSolid() {
        return backFaceSolid;
    }

    public OptionalBoolean getBackFaceCutout() {
        return backFaceCutout;
    }

    public OptionalBoolean getBackFaceCutoutMipped() {
        return backFaceCutoutMipped;
    }

    public OptionalBoolean getBackFaceTranslucent() {
        return backFaceTranslucent;
    }

    public OptionalBoolean getRainDepth() {
        return rainDepth;
    }

    public OptionalBoolean getBeaconBeamDepth() {
        return beaconBeamDepth;
    }

    public OptionalBoolean getSeparateAo() {
        return separateAo;
    }

    public OptionalBoolean getFrustumCulling() {
        return frustumCulling;
    }

    public ShadowCullState getShadowCulling() {
        return shadowCulling;
    }

    public OptionalBoolean getShadowEnabled() {
        return shadowEnabled;
    }

    public OptionalBoolean getParticlesBeforeDeferred() {
        return particlesBeforeDeferred;
    }

    public Optional<ParticleRenderingSettings> getParticleRenderingSettings() {
        if (particleRenderingSettings != null) {
            return Optional.of(particleRenderingSettings);
        }
        if (particlesBeforeDeferred.orElse(false)) {
            return Optional.of(ParticleRenderingSettings.BEFORE);
        }
        return Optional.empty();
    }

    public OptionalBoolean getConcurrentCompute() {
        return concurrentCompute;
    }

    public OptionalBoolean getPrepareBeforeShadow() {
        return prepareBeforeShadow;
    }

    public Object2ObjectMap<String, AlphaTestOverride> getAlphaTestOverrides() {
        return alphaTestOverrides;
    }

    public Object2FloatMap<String> getViewportScaleOverrides() {
        return viewportScaleOverrides;
    }

    public Object2ObjectMap<String, TextureScaleOverride> getTextureScaleOverrides() {
        return textureScaleOverrides;
    }

    public Object2ObjectMap<String, BlendModeOverride> getBlendModeOverrides() {
        return blendModeOverrides;
    }

    public Object2ObjectMap<String, IndirectPointer> getIndirectPointers() {
        return indirectPointers;
    }

    public Object2ObjectMap<String, ArrayList<BufferBlendInformation>> getBufferBlendOverrides() {
        return bufferBlendOverrides;
    }

    public EnumMap<TextureStage, Object2ObjectMap<String, String>> getCustomTextures() {
        return customTextures;
    }

    public Optional<String> getNoiseTexturePath() {
        return Optional.ofNullable(noiseTexturePath);
    }

    public Int2ObjectMap<ShaderStorageInfo> getBufferObjects() {
        return bufferObjects;
    }

    public Object2ObjectMap<String, ImageInformation> getCustomImages() {
        return customImages;
    }

    public Object2ObjectMap<String, String> getConditionallyEnabledPrograms() {
        return conditionallyEnabledPrograms;
    }

    public List<String> getSliderOptions() {
        return sliderOptions;
    }

    public Map<String, List<String>> getProfiles() {
        return profiles;
    }

    public Optional<List<String>> getMainScreenOptions() {
        return Optional.ofNullable(mainScreenOptions);
    }

    public Map<String, List<String>> getSubScreenOptions() {
        return subScreenOptions;
    }

    public @Nullable Integer getMainScreenColumnCount() {
        return mainScreenColumnCount;
    }

    public Map<String, Integer> getSubScreenColumnCount() {
        return subScreenColumnCount;
    }

    public Object2ObjectMap<String, Object2BooleanMap<String>> getExplicitFlips() {
        return explicitFlips;
    }

    public List<String> getRequiredFeatureFlags() {
        return requiredFeatureFlags;
    }

    public List<String> getOptionalFeatureFlags() {
        return optionalFeatureFlags;
    }
}
