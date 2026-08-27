package re.lilith.aurum.gui.element.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.gui.GuiUtil;
import re.lilith.aurum.gui.NavigationController;
import re.lilith.aurum.gui.screen.ShaderPackScreen;
import re.lilith.aurum.shaderpack.option.OptionSet;
import re.lilith.aurum.shaderpack.option.Profile;
import re.lilith.aurum.shaderpack.option.ProfileSet;
import re.lilith.aurum.shaderpack.option.menu.OptionMenuProfileElement;
import re.lilith.aurum.shaderpack.option.values.OptionValues;

import java.util.Optional;

public class ProfileElementWidget extends BaseOptionElementWidget<OptionMenuProfileElement> {
    private static final Text PROFILE_LABEL = new TranslatableText("options.aurum.profile");
    private static final Text PROFILE_CUSTOM = new TranslatableText("options.aurum.profile.custom").setStyle(new Style().setFormatting(Formatting.YELLOW));

    private Profile next;
    private Profile previous;
    private Text profileLabel;

    public ProfileElementWidget(OptionMenuProfileElement element) {
        super(element);
    }

    @Override
    public void init(ShaderPackScreen screen, NavigationController navigation) {
        super.init(screen, navigation);
        this.setLabel(PROFILE_LABEL);

        ProfileSet profiles = this.element.profiles;
        OptionSet options = this.element.options;
        OptionValues pendingValues = this.element.getPendingOptionValues();

        ProfileSet.ProfileResult result = profiles.scan(options, pendingValues);

        this.next = result.next;
        this.previous = result.previous;
        Optional<String> profileName = result.getCurrent().map(p -> p.name);

        this.profileLabel = profileName.map(name -> GuiUtil.translateOrDefault(new LiteralText(name), "profile." + name)).orElse(PROFILE_CUSTOM);
    }

    @Override
    public void render(int x, int y, int width, int height, int mouseX, int mouseY, float tickDelta, boolean hovered) {
        this.updateRenderParams(width, width - (MinecraftClient.getInstance().textRenderer.getStringWidth(PROFILE_LABEL.asFormattedString()) + 16));

        this.renderOptionWithValue(x, y, width, height, hovered);
    }

    @Override
    protected Text createValueLabel() {
        return this.profileLabel;
    }

    @Override
    public Optional<Text> getCommentTitle() {
        return Optional.of(PROFILE_LABEL);
    }

    @Override
    public String getCommentKey() {
        return "profile.comment";
    }

    @Override
    public boolean applyNextValue() {
        if (this.next == null) {
            return false;
        }

        Aurum.queueShaderPackOptionsFromProfile(this.next);

        return true;
    }

    @Override
    public boolean applyPreviousValue() {
        if (this.previous == null) {
            return false;
        }

        Aurum.queueShaderPackOptionsFromProfile(this.previous);

        return true;
    }

    @Override
    public boolean applyOriginalValue() {
        return false; // Resetting options is the way to return to the "default profile"
    }

    @Override
    public boolean isValueModified() {
        return false;
    }
}
