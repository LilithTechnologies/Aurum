package re.lilith.aurum.gl.uniform;


import java.util.Collections;
import java.util.List;
import java.util.Set;

public record UniformManifest(List<Entry> entries, Set<String> dynamicNames, Set<String> externallyManagedNames) {

    public record Entry(String name, UniformType type, UniformUpdateFrequency frequency) {
    }

    public UniformManifest(List<Entry> entries, Set<String> dynamicNames, Set<String> externallyManagedNames) {
        this.entries = Collections.unmodifiableList(entries);
        this.dynamicNames = Collections.unmodifiableSet(dynamicNames);
        this.externallyManagedNames = Collections.unmodifiableSet(externallyManagedNames);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
