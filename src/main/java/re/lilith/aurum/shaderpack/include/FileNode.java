package re.lilith.aurum.shaderpack.include;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import re.lilith.aurum.shaderpack.transform.LineTransform;

import java.util.Objects;

public class FileNode {
    private final AbsolutePackPath path;
    private final ImmutableList<String> lines;
    private final ImmutableMap<Integer, AbsolutePackPath> includes;

    // NB: The caller is responsible for ensuring that the includes map
    //     is in sync with the lines list.
    private FileNode(AbsolutePackPath path, ImmutableList<String> lines,
                     ImmutableMap<Integer, AbsolutePackPath> includes) {
        this.path = path;
        this.lines = lines;
        this.includes = includes;
    }

    public FileNode(AbsolutePackPath path, ImmutableList<String> lines) {
        this.path = path;
        this.lines = lines;

        AbsolutePackPath currentDirectory = path.parent().orElseThrow(
                () -> new IllegalArgumentException("Not a valid shader file name: " + path));

        this.includes = findIncludes(currentDirectory, lines);
    }

    public AbsolutePackPath getPath() {
        return path;
    }

    public ImmutableList<String> getLines() {
        return lines;
    }

    public ImmutableMap<Integer, AbsolutePackPath> getIncludes() {
        return includes;
    }

    public FileNode map(LineTransform transform) {
        ImmutableList.Builder<String> newLines = ImmutableList.builder();
        int index = 0;

        for (String line : lines) {
            String transformedLine = transform.transform(index, line);

            if (includes.containsKey(index)) {
                if (!Objects.equals(line, transformedLine)) {
                    throw new IllegalStateException("Attempted to modify an #include line in LineTransform.");
                }
            }

            newLines.add(transformedLine);
            index += 1;
        }

        return new FileNode(path, newLines.build(), includes);
    }

    private static ImmutableMap<Integer, AbsolutePackPath> findIncludes(AbsolutePackPath currentDirectory,
                                                                        ImmutableList<String> lines) {
        ImmutableMap.Builder<Integer, AbsolutePackPath> foundIncludes = ImmutableMap.builder();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();

            if (!line.startsWith("#include")) {
                continue;
            }

            // Remove the "#include " part so that we just have the file path
            String target = findTarget(line);

            foundIncludes.put(i, currentDirectory.resolve(target));
        }

        return foundIncludes.build();
    }

    private static @NotNull String findTarget(String line) {
        String target = line.substring("#include ".length()).trim();

        // Only strip quotes when they're matched on both ends - a lone leading or trailing quote
        // means the directive is malformed, so leave it untouched rather than silently accepting it.
        if (target.length() >= 2 && target.startsWith("\"") && target.endsWith("\"")) {
            target = target.substring(1, target.length() - 1);
        }

        return target;
    }
}
