package re.lilith.aurum.pipeline.transform;

import org.taumc.glsl.ShaderParser;
import org.taumc.glsl.ShaderPrinter;
import org.taumc.glsl.Transformer;
import re.lilith.aurum.gbuffer.matching.InputAvailability;
import re.lilith.aurum.pipeline.transform.impl.CeleritasTerrainTransformer;
import re.lilith.aurum.pipeline.transform.impl.CompatibilityTransformer;
import re.lilith.aurum.pipeline.transform.impl.CompositeTransformer;
import re.lilith.aurum.pipeline.transform.impl.attribute.AttributeParameters;
import re.lilith.aurum.pipeline.transform.impl.attribute.AttributeTransformer;
import re.lilith.aurum.pipeline.transform.patch.Parameters;
import re.lilith.aurum.pipeline.transform.patch.Patch;
import re.lilith.aurum.pipeline.transform.patch.PatchShaderType;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The transform patcher (triforce 2) uses glsl-transformation-lib's Transformer
 * (an ANTLR-based GLSL AST transformer) to do shader transformation.
 * <p>
 * The TransformPatcher does caching on the source string and associated
 * parameters. For this to work, all objects contained in a parameter must have
 * an equals method, and they must never be changed after having been used for
 * patching. Since the cache also contains the source string, it doesn't need to
 * be disabled when developing shaderpacks. However, when changes are made to
 * the patcher, the cache should be disabled with {@link #useCache}.
 */
public class ShaderTransformer {
    private static final boolean useCache = false;
    private static final int CACHE_SIZE = 400;

    private static final Map<CacheKey, Map<PatchShaderType, String>> cache =
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<CacheKey, Map<PatchShaderType, String>> eldest) {
                    return size() > CACHE_SIZE;
                }
            };

    private record CacheKey(Parameters parameters, String vertex, String geometry, String fragment) {

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            CacheKey other = (CacheKey) obj;
            if (fragment == null) {
                if (other.fragment != null)
                    return false;
            } else if (!fragment.equals(other.fragment))
                return false;
            if (geometry == null) {
                if (other.geometry != null)
                    return false;
            } else if (!geometry.equals(other.geometry))
                return false;
            if (parameters == null) {
                if (other.parameters != null)
                    return false;
            } else if (!parameters.equals(other.parameters))
                return false;
            if (vertex == null) {
                return other.vertex == null;
            } else return vertex.equals(other.vertex);
        }
    }

    private static final Pattern versionPattern = Pattern.compile("#version[ \\t]+(\\d+)(?:[ \\t]+(\\w+))?");
    private static final Pattern reservedSamplePattern = Pattern.compile("\\bsample\\b");

    private static final String RENAMED_SAMPLE = "aurum_renamed_sample";

    private static Map<PatchShaderType, String> transform(String vertex, String geometry, String fragment, Parameters parameters) {
        // stop if all are null
        if (vertex == null && geometry == null && fragment == null) {
            return null;
        }

        CacheKey key = null;
        if (useCache) {
            key = new CacheKey(parameters, vertex, geometry, fragment);
            synchronized (cache) {
                Map<PatchShaderType, String> cached = cache.get(key);
                if (cached != null) {
                    return cached;
                }
            }
        }

        EnumMap<PatchShaderType, String> inputs = new EnumMap<>(PatchShaderType.class);
        inputs.put(PatchShaderType.VERTEX, vertex);
        inputs.put(PatchShaderType.GEOMETRY, geometry);
        inputs.put(PatchShaderType.FRAGMENT, fragment);

        Map<PatchShaderType, String> result = transformInternal(inputs, parameters);

        if (useCache) {
            synchronized (cache) {
                cache.put(key, result);
            }
        }

        return result;
    }

    private static Map<PatchShaderType, String> transformInternal(EnumMap<PatchShaderType, String> inputs, Parameters parameters) {
        EnumMap<PatchShaderType, String> result = new EnumMap<>(PatchShaderType.class);
        EnumMap<PatchShaderType, Transformer> transformers = new EnumMap<>(PatchShaderType.class);
        EnumMap<PatchShaderType, String> headers = new EnumMap<>(PatchShaderType.class);

        for (PatchShaderType type : PatchShaderType.values()) {
            String input = inputs.get(type);
            if (input == null) {
                continue;
            }

            parameters.type = type;

            Matcher matcher = versionPattern.matcher(input);
            if (!matcher.find()) {
                throw new IllegalArgumentException("No #version directive found in source code! See debugging.md for more information.");
            }
            int version = Integer.parseInt(matcher.group(1));
            boolean isCoreProfile = "core".equals(matcher.group(2));
            parameters.version = version;

            String renamedInput = reservedSamplePattern.matcher(input).replaceAll(RENAMED_SAMPLE);

            ShaderParser.ParsedShader parsed = ShaderParser.parseShader(renamedInput);
            Transformer transformer = new Transformer(parsed.full());

            boolean needsShaderTextureLodExtension = false;
            switch (parameters.patch) {
                case ATTRIBUTES:
                    AttributeTransformer.transform(transformer, (AttributeParameters) parameters, isCoreProfile);
                    break;
                case CELERITAS_TERRAIN:
                    CeleritasTerrainTransformer.transform(transformer, parameters);
                    break;
                case COMPOSITE:
                    needsShaderTextureLodExtension = CompositeTransformer.transform(transformer, version);
                    break;
            }
            CompatibilityTransformer.transformEach(transformer, parameters);

            // restore identifiers that were renamed to dodge reserved words before parsing
            transformer.rename(RENAMED_SAMPLE, "sample");

            String header = ShaderPrinter.getFormattedShader(parsed.pre());
            if (needsShaderTextureLodExtension) {
                header = header + "\n#extension GL_ARB_shader_texture_lod : require\n";
            }

            if (version < 130) {
                header = versionPattern.matcher(header).replaceFirst("#version 130");
            }

            if (header.contains("GL_ARB_shader_storage_buffer_object")) {
                if (!header.contains("GL_ARB_shading_language_420pack")) {
                    header = header + "\n#extension GL_ARB_shading_language_420pack : enable\n";
                }

                if (version < 150) {
                    header = versionPattern.matcher(header).replaceFirst("#version 150 compatibility");
                }
            }

            transformers.put(type, transformer);
            headers.put(type, header);
        }

        // the compatibility transformer does a grouped transformation
        CompatibilityTransformer.transformGrouped(transformers);

        for (Map.Entry<PatchShaderType, Transformer> entry : transformers.entrySet()) {
            PatchShaderType type = entry.getKey();
            Transformer transformer = entry.getValue();

            String[] bodyHolder = new String[1];
            transformer.mutateTree(tree -> bodyHolder[0] = ShaderPrinter.getFormattedShader(tree));

            result.put(type, headers.get(type) + "\n" + bodyHolder[0]);
        }

        return result;
    }

    public static Map<PatchShaderType, String> patchAttributes(String vertex, String geometry, String fragment, InputAvailability inputs, boolean scrollGlint) {
        return transform(vertex, geometry, fragment, new AttributeParameters(Patch.ATTRIBUTES, geometry != null, inputs, scrollGlint));
    }

    public static Map<PatchShaderType, String> patchCeleritasTerrain(String vertex, String geometry, String fragment) {
        return transform(vertex, geometry, fragment, new Parameters(Patch.CELERITAS_TERRAIN));
    }

    public static Map<PatchShaderType, String> patchComposite(String vertex, String geometry, String fragment) {
        return transform(vertex, geometry, fragment, new Parameters(Patch.COMPOSITE));
    }

    private static final Map<String, String> computeCache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    public static String patchCompute(String source) {
        if (source == null) {
            return null;
        }

        if (useCache) {
            synchronized (computeCache) {
                String cached = computeCache.get(source);
                if (cached != null) {
                    return cached;
                }
            }
        }

        Matcher matcher = versionPattern.matcher(source);
        if (!matcher.find()) {
            throw new IllegalArgumentException("No #version directive found in source code! See debugging.md for more information.");
        }
        int version = Integer.parseInt(matcher.group(1));

        String renamedInput = reservedSamplePattern.matcher(source).replaceAll(RENAMED_SAMPLE);

        ShaderParser.ParsedShader parsed = ShaderParser.parseShader(renamedInput);
        Transformer transformer = new Transformer(parsed.full());

        CompatibilityTransformer.transformCompute(transformer, version);

        transformer.rename(RENAMED_SAMPLE, "sample");

        String header = ShaderPrinter.getFormattedShader(parsed.pre());

        if (version < 130) {
            header = versionPattern.matcher(header).replaceFirst("#version 130");
        }

        if (header.contains("GL_ARB_shader_storage_buffer_object")) {
            if (!header.contains("GL_ARB_shading_language_420pack")) {
                header = header + "\n#extension GL_ARB_shading_language_420pack : enable\n";
            }

            if (version < 150) {
                header = versionPattern.matcher(header).replaceFirst("#version 150 compatibility");
            }
        }

        String[] bodyHolder = new String[1];
        transformer.mutateTree(tree -> bodyHolder[0] = ShaderPrinter.getFormattedShader(tree));

        String result = header + "\n" + bodyHolder[0];

        if (useCache) {
            synchronized (computeCache) {
                computeCache.put(source, result);
            }
        }

        return result;
    }
}
