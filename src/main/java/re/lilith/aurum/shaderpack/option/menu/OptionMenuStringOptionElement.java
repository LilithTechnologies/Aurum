package re.lilith.aurum.shaderpack.option.menu;

import re.lilith.aurum.shaderpack.ShaderProperties;
import re.lilith.aurum.shaderpack.option.StringOption;
import re.lilith.aurum.shaderpack.option.values.OptionValues;

public class OptionMenuStringOptionElement extends OptionMenuOptionElement {
    public final StringOption option;

    public OptionMenuStringOptionElement(String elementString, OptionMenuContainer container, ShaderProperties shaderProperties, OptionValues values, StringOption option) {
        super(elementString, container, shaderProperties, values);
        this.option = option;
    }
}
