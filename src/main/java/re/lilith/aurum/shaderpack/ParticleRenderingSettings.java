package re.lilith.aurum.shaderpack;

import re.lilith.aurum.Aurum;

import java.util.Optional;

public enum ParticleRenderingSettings {
    BEFORE,
    MIXED,
    AFTER;

    public static Optional<ParticleRenderingSettings> fromString(String name) {
        try {
            return Optional.of(ParticleRenderingSettings.valueOf(name));
        } catch (IllegalArgumentException e) {
            Aurum.LOGGER.warn("Invalid particle rendering settings! " + name);
            return Optional.empty();
        }
    }
}
