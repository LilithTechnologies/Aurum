package re.lilith.aurum.texture.pbr.loader;

import net.minecraft.util.Identifier;

public class SpriteInfo {
    private final Identifier name;
    private final int width, height;

    public SpriteInfo(Identifier name, int width, int height) {
        this.name = name;
        this.width = width;
        this.height = height;
    }

    public Identifier name() {
        return name;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
