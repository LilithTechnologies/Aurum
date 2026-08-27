package re.lilith.aurum.gui.element.widget;

import net.minecraft.client.gui.widget.ButtonWidget;

import java.util.function.Consumer;

public class AurumButtonWidget extends ButtonWidget {
    protected final Consumer<AurumButtonWidget> callback;

    public AurumButtonWidget(int x, int y, int width, int height, String message, Consumer<AurumButtonWidget> callback) {
        super(69420, x, y, width, height, message);

        this.callback = callback;
    }

    public void click() {
        this.callback.accept(this);
    }
}