package re.lilith.aurum.gui.element.widget;

import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.gui.GuiUtil;
import re.lilith.aurum.gui.NavigationController;
import re.lilith.aurum.gui.screen.ShaderPackScreen;
import re.lilith.aurum.shaderpack.option.BooleanOption;
import re.lilith.aurum.shaderpack.option.menu.OptionMenuBooleanOptionElement;

public class BooleanElementWidget extends BaseOptionElementWidget<OptionMenuBooleanOptionElement> {
    private static final Text TEXT_TRUE = new TranslatableText("label.aurum.true").setStyle(new Style().setFormatting(Formatting.GREEN));
    private static final Text TEXT_FALSE = new TranslatableText("label.aurum.false").setStyle(new Style().setFormatting(Formatting.GREEN));
    private static final Text TEXT_TRUE_DEFAULT = new TranslatableText("label.aurum.true");
    private static final Text TEXT_FALSE_DEFAULT = new TranslatableText("label.aurum.false");

    private final BooleanOption option;

    private boolean appliedValue;
    private boolean value;
    private boolean defaultValue;

    public BooleanElementWidget(OptionMenuBooleanOptionElement element) {
        super(element);

        this.option = element.option;
    }

    @Override
    public void init(ShaderPackScreen screen, NavigationController navigation) {
        super.init(screen, navigation);

        // The value currently in use by the shader pack
        this.appliedValue = this.element.getAppliedOptionValues().getBooleanValueOrDefault(this.option.getName());

        // The yet-to-be-applied value that has been queued (if that is the case)
        // Might be equal to the applied value
        this.value = this.element.getPendingOptionValues().getBooleanValueOrDefault(this.option.getName());

        this.defaultValue = this.element.getAppliedOptionValues().getOptionSet().getBooleanOptions()
                .get(this.option.getName()).getOption().getDefaultValue();

        this.setLabel(GuiUtil.translateOrDefault(new LiteralText(this.option.getName()), "option." + this.option.getName()));
    }

    @Override
    public void render(int x, int y, int width, int height, int mouseX, int mouseY, float tickDelta, boolean hovered) {
        this.updateRenderParams(width, 28);

        this.renderOptionWithValue(x, y, width, height, hovered);
        this.tryRenderTooltip(mouseX, mouseY, hovered);
    }

    @Override
    protected Text createValueLabel() {
        // UX: Do not use color if the value is set to default.
        //
        // This is because the red color for "Off" and green color of "On"
        // was causing people to want to change options to On when that was
        // unnecessary due to red having a bad association.
        //
        // This was changed on request of Emin, since people kept on changing
        // Compatibility Mode to "On" when not needed. Now we use white for
        // default to avoid giving a positive or negative connotation to a
        // default value.
        if (this.value == this.defaultValue) {
            return this.value ? TEXT_TRUE_DEFAULT : TEXT_FALSE_DEFAULT;
        }

        return this.value ? TEXT_TRUE : TEXT_FALSE;
    }

    @Override
    public String getCommentKey() {
        return "option." + this.option.getName() + ".comment";
    }

    public String getValue() {
        return Boolean.toString(this.value);
    }

    private void queue() {
        Aurum.getShaderPackOptionQueue().put(this.option.getName(), this.getValue());
    }

    @Override
    public boolean applyNextValue() {
        this.value = !this.value;
        this.queue();

        return true;
    }

    @Override
    public boolean applyPreviousValue() {
        return this.applyNextValue();
    }

    @Override
    public boolean applyOriginalValue() {
        this.value = this.option.getDefaultValue();
        this.queue();

        return true;
    }

    @Override
    public boolean isValueModified() {
        return this.value != this.appliedValue;
    }
}
