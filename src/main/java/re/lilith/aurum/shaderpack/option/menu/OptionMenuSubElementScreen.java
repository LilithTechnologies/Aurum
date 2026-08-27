package re.lilith.aurum.shaderpack.option.menu;

import org.jetbrains.annotations.Nullable;
import re.lilith.aurum.shaderpack.ShaderProperties;
import re.lilith.aurum.shaderpack.option.ShaderPackOptions;

import java.util.List;

public class OptionMenuSubElementScreen extends OptionMenuElementScreen {
    public final String screenId;

    public OptionMenuSubElementScreen(String screenId, OptionMenuContainer container, ShaderProperties shaderProperties, ShaderPackOptions shaderPackOptions, List<String> elementStrings, @Nullable Integer columnCount) {
        super(container, shaderProperties, shaderPackOptions, elementStrings, columnCount);

        this.screenId = screenId;
    }
}
