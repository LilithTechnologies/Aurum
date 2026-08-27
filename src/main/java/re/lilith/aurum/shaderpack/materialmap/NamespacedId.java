package re.lilith.aurum.shaderpack.materialmap;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record NamespacedId(String namespace, String name) {
    public NamespacedId(String combined) {
        this(parseNamespace(combined), parseName(combined));
    }

    private static String parseNamespace(String combined) {
        int colonIdx = combined.indexOf(':');
        return colonIdx == -1 ? "minecraft" : combined.substring(0, colonIdx);
    }

    private static String parseName(String combined) {
        int colonIdx = combined.indexOf(':');
        return colonIdx == -1 ? combined : combined.substring(colonIdx + 1);
    }

    public NamespacedId(String namespace, String name) {
        this.namespace = Objects.requireNonNull(namespace);
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NamespacedId that = (NamespacedId) o;

        return namespace.equals(that.namespace) && name.equals(that.name);
    }

    @Override
    public @NotNull String toString() {
        return "NamespacedId{" +
                "namespace='" + namespace + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
