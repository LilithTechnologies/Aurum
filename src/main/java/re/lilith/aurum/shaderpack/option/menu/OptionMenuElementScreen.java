package re.lilith.aurum.shaderpack.option.menu;

import org.jetbrains.annotations.Nullable;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.shaderpack.ShaderProperties;
import re.lilith.aurum.shaderpack.option.ShaderPackOptions;

import java.util.ArrayList;
import java.util.List;

public class OptionMenuElementScreen {
    public final List<OptionMenuElement> elements = new ArrayList<>();

    private final @Nullable Integer columnCount;

    public OptionMenuElementScreen(OptionMenuContainer container, ShaderProperties shaderProperties, ShaderPackOptions shaderPackOptions, List<String> elementStrings, @Nullable Integer columnCount) {
        this.columnCount = columnCount;

        for (String elementString : elementStrings) {
            if ("*".equals(elementString)) {
                container.queueForUnusedOptionDump(this.elements.size(), this.elements);

                continue;
            }

            try {
                OptionMenuElement element = OptionMenuElement.create(elementString, container, shaderProperties, shaderPackOptions);

                if (element != null) {
                    this.elements.add(element);

                    if (element instanceof OptionMenuOptionElement) {
                        container.notifyOptionAdded(elementString);
                    }
                }
            } catch (IllegalArgumentException error) {
                Aurum.LOGGER.warn(error.getMessage());

                this.elements.add(OptionMenuElement.EMPTY);
            }
        }
    }

    public int getColumnCount() {
        if (columnCount != null) return columnCount;
        else return elements.size() > 18 ? 3 : 2;
    }
}
