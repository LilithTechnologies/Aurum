package re.lilith.aurum.shaderpack.option.menu;

import re.lilith.aurum.Aurum;
import re.lilith.aurum.shaderpack.ShaderProperties;
import re.lilith.aurum.shaderpack.option.values.MutableOptionValues;
import re.lilith.aurum.shaderpack.option.values.OptionValues;

public abstract class OptionMenuOptionElement extends OptionMenuElement {
    public final boolean slider;
    public final OptionMenuContainer container;
    public final String optionId;

    private final OptionValues packAppliedValues;

    public OptionMenuOptionElement(String elementString, OptionMenuContainer container, ShaderProperties shaderProperties, OptionValues packAppliedValues) {
        this.slider = shaderProperties.getSliderOptions().contains(elementString);
        this.container = container;
        this.optionId = elementString;
        this.packAppliedValues = packAppliedValues;
    }

    /**
     * @return the {@link OptionValues} currently in use by the shader pack
     */
    public OptionValues getAppliedOptionValues() {
        return packAppliedValues;
    }

    /**
     * @return an {@link OptionValues} that also contains values currently
     * pending application.
     */
    public OptionValues getPendingOptionValues() {
        MutableOptionValues values = getAppliedOptionValues().mutableCopy();
        values.addAll(Aurum.getShaderPackOptionQueue());

        return values;
    }
}
