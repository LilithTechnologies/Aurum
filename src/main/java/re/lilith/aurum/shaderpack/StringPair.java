package re.lilith.aurum.shaderpack;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * An absurdly simple record for storing pairs of strings because Java lacks pair / tuple types.
 */
public record StringPair(@NotNull String key, @NotNull String value) {
    public StringPair {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
    }

    @NotNull
    public String getKey() {
        return key;
    }

    @NotNull
    public String getValue() {
        return value;
    }
}
