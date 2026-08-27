package re.lilith.aurum.shaderpack.option.menu;

import org.jetbrains.annotations.Nullable;
import re.lilith.aurum.shaderpack.ShaderProperties;
import re.lilith.aurum.shaderpack.option.ShaderPackOptions;

import java.util.List;

public class OptionMenuMainElementScreen extends OptionMenuElementScreen {
    public OptionMenuMainElementScreen(OptionMenuContainer container, ShaderProperties shaderProperties, ShaderPackOptions shaderPackOptions, List<String> elementStrings, @Nullable Integer columnCount) {
        super(container, shaderProperties, shaderPackOptions, elementStrings, columnCount);
    }
}
