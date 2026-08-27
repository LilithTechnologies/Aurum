package re.lilith.aurum.gui.element.widget;

import re.lilith.aurum.gui.NavigationController;
import re.lilith.aurum.gui.screen.ShaderPackScreen;
import re.lilith.aurum.shaderpack.option.menu.OptionMenuElement;

public abstract class AbstractElementWidget<T extends OptionMenuElement> {
    protected final T element;

    public static final AbstractElementWidget<OptionMenuElement> EMPTY = new AbstractElementWidget<>(null) {
        @Override
        public void render(int x, int y, int width, int height, int mouseX, int mouseY, float tickDelta, boolean hovered) {
        }
    };

    public AbstractElementWidget(T element) {
        this.element = element;
    }

    public void init(ShaderPackScreen screen, NavigationController navigation) {
    }

    public abstract void render(int x, int y, int width, int height, int mouseX, int mouseY, float tickDelta, boolean hovered);

    public boolean mouseClicked(double mx, double my, int button) {
        return false;
    }

    public boolean mouseReleased(double mx, double my, int button) {
        return false;
    }
}
