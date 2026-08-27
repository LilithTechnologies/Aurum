package re.lilith.aurum.shaderpack.option.menu;

import re.lilith.aurum.shaderpack.ShaderProperties;
import re.lilith.aurum.shaderpack.option.BooleanOption;
import re.lilith.aurum.shaderpack.option.values.OptionValues;

public class OptionMenuBooleanOptionElement extends OptionMenuOptionElement {
    public final BooleanOption option;

    public OptionMenuBooleanOptionElement(String elementString, OptionMenuContainer container, ShaderProperties shaderProperties, OptionValues values, BooleanOption option) {
        super(elementString, container, shaderProperties, values);
        this.option = option;
    }
}
