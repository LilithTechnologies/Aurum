package re.lilith.aurum.config;

import re.lilith.aurum.Aurum;
import re.lilith.aurum.gui.option.AurumVideoSettings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/**
 * A class dedicated to storing the config values of shaderpacks. Right now it only stores the path to the current shaderpack
 */
public class AurumConfig {
    private static final String COMMENT =
            "This file stores configuration options for Aurum, such as the currently active shaderpack";

    /**
     * The path to the current shaderpack. Null if the internal shaderpack is being used.
     */
    private String shaderPackName;

    /**
     * Whether or not shaders are used for rendering. False to disable all shader-based rendering, true to enable it.
     */
    private boolean enableShaders;

    private final Path propertiesPath;

    public AurumConfig(Path propertiesPath) {
        shaderPackName = null;
        enableShaders = true;
        this.propertiesPath = propertiesPath;
    }

    /**
     * Initializes the configuration, loading it if it is present and creating a default config otherwise.
     *
     * @throws IOException file exceptions
     */
    public void initialize() throws IOException {
        load();
        if (!Files.exists(propertiesPath)) {
            save();
        }
    }

    /**
     * Returns the name of the current shaderpack
     *
     * @return Returns the current shaderpack name - if internal shaders are being used it returns "(internal)"
     */
    public Optional<String> getShaderPackName() {
        return Optional.ofNullable(shaderPackName);
    }

    /**
     * Sets the name of the current shaderpack
     */
    public void setShaderPackName(String name) {
        if (name == null || name.equals("(internal)") || name.isEmpty()) {
            this.shaderPackName = null;
        } else {
            this.shaderPackName = name;
        }
    }

    /**
     * Determines whether or not shaders are used for rendering.
     *
     * @return False to disable all shader-based rendering, true to enable shader-based rendering.
     */
    public boolean areShadersEnabled() {
        return enableShaders;
    }

    /**
     * Sets whether shaders should be used for rendering.
     */
    public void setShadersEnabled(boolean enabled) {
        this.enableShaders = enabled;
    }

    /**
     * loads the config file and then populates the string, int, and boolean entries with the parsed entries
     *
     * @throws IOException if the file cannot be loaded
     */

    public void load() throws IOException {
        if (!Files.exists(propertiesPath)) {
            return;
        }

        Properties properties = new Properties();
        // NB: This uses ISO-8859-1 with unicode escapes as the encoding
        try (InputStream is = Files.newInputStream(propertiesPath)) {
            properties.load(is);
        }
        shaderPackName = properties.getProperty("shaderPack");
        enableShaders = !"false".equals(properties.getProperty("enableShaders"));
        try {
            AurumVideoSettings.shadowDistance = Integer.parseInt(properties.getProperty("maxShadowRenderDistance", "32"));
        } catch (NumberFormatException e) {
            Aurum.LOGGER.error("Shadow distance setting reset; value is invalid.");
            AurumVideoSettings.shadowDistance = 32;
            save();
        }

        if (shaderPackName != null) {
            if (shaderPackName.equals("(internal)") || shaderPackName.isEmpty()) {
                shaderPackName = null;
            }
        }
    }

    /**
     * Serializes the config into a file. Should be called whenever any config values are modified.
     *
     * @throws IOException file exceptions
     */
    public void save() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("shaderPack", getShaderPackName().orElse(""));
        properties.setProperty("enableShaders", Boolean.toString(enableShaders));
        properties.setProperty("maxShadowRenderDistance", String.valueOf(AurumVideoSettings.shadowDistance));
        // NB: This uses ISO-8859-1 with Unicode escapes as the encoding
        try (OutputStream os = Files.newOutputStream(propertiesPath)) {
            properties.store(os, COMMENT);
        }
    }
}
