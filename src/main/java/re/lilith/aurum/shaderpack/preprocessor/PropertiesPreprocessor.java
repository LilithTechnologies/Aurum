package re.lilith.aurum.shaderpack.preprocessor;

import org.anarres.cpp.*;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.shaderpack.StringPair;
import re.lilith.aurum.shaderpack.option.ShaderPackOptions;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PropertiesPreprocessor {
    // Derived from ShaderProcessor.glslPreprocessSource, which is derived from GlShader from Canvas, licenced under LGPL
    public static String preprocessSource(String source, ShaderPackOptions shaderPackOptions, Iterable<StringPair> environmentDefines) {
        if (source.contains(PropertyCollectingListener.PROPERTY_MARKER)) {
            throw new RuntimeException("Some shader author is trying to exploit internal Aurum implementation details, stop!");
        }

        List<String> booleanValues = getBooleanValues(shaderPackOptions);
        Map<String, String> stringValues = getStringValues(shaderPackOptions);

        try (Preprocessor pp = new Preprocessor()) {
            for (String value : booleanValues) {
                pp.addMacro(value);
            }

            for (StringPair envDefine : environmentDefines) {
                pp.addMacro(envDefine.getKey(), envDefine.getValue());
            }

            stringValues.forEach((name, value) -> {
                try {
                    pp.addMacro(name, value);
                } catch (LexerException e) {
                    Aurum.LOGGER.error(e);
                }
            });

            return process(pp, source);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IOException while processing macros", e);
        } catch (LexerException e) {
            throw new RuntimeException("Unexpected LexerException processing macros", e);
        }
    }

    private static String process(Preprocessor preprocessor, String source) {
        preprocessor.setListener(new PropertiesCommentListener());
        PropertyCollectingListener listener = new PropertyCollectingListener();
        preprocessor.setListener(listener);

        // Not super efficient, but this removes trailing whitespace on lines, fixing an issue with whitespace after
        // line continuations (see PreprocessorTest#testWeirdPropertiesLineContinuation)
        // Required for Voyager Shader
        source = Arrays.stream(source.split("\\R")).map(String::trim)
                .map(line -> {
                    if (line.startsWith("#")) {
                        // In PropertyCollectingListener we suppress "unknown preprocessor directive errors" and
                        // assume the line to be a comment, since in .properties files `#` also functions as a comment
                        // marker.
                        return line;
                    } else {
                        // This is a hack to ensure that non-macro lines don't have any preprocessing applied...
                        // In properties files, we don't substitute #define values except on macro lines.
                        return "#warning AURUM_PASSTHROUGH " + line;
                    }
                }).collect(Collectors.joining("\n")) + "\n";

        preprocessor.addInput(new StringLexerSource(source, true));
        preprocessor.addFeature(Feature.KEEPCOMMENTS);

        final StringBuilder builder = new StringBuilder();

        try {
            for (; ; ) {
                final Token tok = preprocessor.token();
                if (tok.getType() == Token.EOF) break;
                builder.append(tok.getText());
            }
        } catch (final Exception e) {
            Aurum.LOGGER.error("Properties pre-processing failed", e);
        }

        source = builder.toString();

        return listener.collectLines() + source;
    }

    private static List<String> getBooleanValues(ShaderPackOptions shaderPackOptions) {
        List<String> booleanValues = new ArrayList<>();

        shaderPackOptions.getOptionSet().getBooleanOptions().forEach((string, _) -> {
            boolean trueValue = shaderPackOptions.getOptionValues().getBooleanValueOrDefault(string);

            if (trueValue) {
                booleanValues.add(string);
            }
        });

        return booleanValues;
    }

    private static Map<String, String> getStringValues(ShaderPackOptions shaderPackOptions) {
        Map<String, String> stringValues = new HashMap<>();

        shaderPackOptions.getOptionSet().getStringOptions().forEach(
                (optionName, _) -> stringValues.put(optionName, shaderPackOptions.getOptionValues().getStringValueOrDefault(optionName)));

        return stringValues;
    }
}
