package re.lilith.aurum.gui.element.screen;

import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public record ElementWidgetScreenData(Text heading, boolean backButton) {
    public static final ElementWidgetScreenData EMPTY = new ElementWidgetScreenData(new LiteralText(""), true);
}
