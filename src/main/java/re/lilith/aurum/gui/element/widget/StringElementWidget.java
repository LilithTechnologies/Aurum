package re.lilith.aurum.gui.element.widget;

import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.gui.GuiUtil;
import re.lilith.aurum.gui.NavigationController;
import re.lilith.aurum.gui.screen.ShaderPackScreen;
import re.lilith.aurum.shaderpack.option.StringOption;
import re.lilith.aurum.shaderpack.option.menu.OptionMenuStringOptionElement;

import java.util.List;

public class StringElementWidget extends BaseOptionElementWidget<OptionMenuStringOptionElement> {
    protected final StringOption option;

    protected String appliedValue;
    protected int valueCount;
    protected int valueIndex;

    public StringElementWidget(OptionMenuStringOptionElement element) {
        super(element);

        this.option = element.option;
    }

    @Override
    public void init(ShaderPackScreen screen, NavigationController navigation) {
        super.init(screen, navigation);

        // The yet-to-be-applied value that has been queued (if that is the case)
        // Might be equal to the applied value
        String actualPendingValue = this.element.getPendingOptionValues().getStringValueOrDefault(this.option.getName());

        // The value currently in use by the shader pack
        this.appliedValue = this.element.getAppliedOptionValues().getStringValueOrDefault(this.option.getName());

        this.setLabel(GuiUtil.translateOrDefault(new LiteralText(this.option.getName()), "option." + this.option.getName()));

        List<String> values = this.option.getAllowedValues();

        this.valueCount = values.size();
        this.valueIndex = values.indexOf(actualPendingValue);
    }

    @Override
    public void render(int x, int y, int width, int height, int mouseX, int mouseY, float tickDelta, boolean hovered) {
        this.updateRenderParams(width, 0);

        this.renderOptionWithValue(x, y, width, height, hovered);
        this.tryRenderTooltip(mouseX, mouseY, hovered);
    }

    private void increment(int amount) {
        this.valueIndex = Math.max(this.valueIndex, 0);

        this.valueIndex = Math.floorMod(this.valueIndex + amount, this.valueCount);
    }

    @Override
    protected Text createValueLabel() {
        return GuiUtil.translateOrDefault(
                new LiteralText(getValue()).setStyle(new Style().setFormatting(Formatting.BLUE)),
                "value." + this.option.getName() + "." + getValue());
    }

    @Override
    public String getCommentKey() {
        return "option." + this.option.getName() + ".comment";
    }

    public String getValue() {
        if (this.valueIndex < 0) {
            return this.appliedValue;
        }
        return this.option.getAllowedValues().get(this.valueIndex);
    }

    protected void queue() {
        Aurum.getShaderPackOptionQueue().put(this.option.getName(), this.getValue());
    }

    @Override
    public boolean applyNextValue() {
        this.increment(1);
        this.queue();

        return true;
    }

    @Override
    public boolean applyPreviousValue() {
        this.increment(-1);
        this.queue();

        return true;
    }

    @Override
    public boolean applyOriginalValue() {
        this.valueIndex = this.option.getAllowedValues().indexOf(this.option.getDefaultValue());
        this.queue();

        return true;
    }

    @Override
    public boolean isValueModified() {
        return !this.appliedValue.equals(this.getValue());
    }
}
