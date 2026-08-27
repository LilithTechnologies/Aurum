package re.lilith.aurum.gui.element.widget;

import net.minecraft.text.Text;
import re.lilith.aurum.shaderpack.option.menu.OptionMenuElement;

import java.util.Optional;

public abstract class CommentedElementWidget<T extends OptionMenuElement> extends AbstractElementWidget<T> {
    public CommentedElementWidget(T element) {
        super(element);
    }

    public abstract Optional<Text> getCommentTitle();

    public abstract Optional<Text> getCommentBody();
}
