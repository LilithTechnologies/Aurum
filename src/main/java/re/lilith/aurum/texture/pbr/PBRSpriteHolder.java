package re.lilith.aurum.texture.pbr;

import net.minecraft.client.texture.Sprite;

public class PBRSpriteHolder {
    protected Sprite normalSprite;
    protected Sprite specularSprite;

    public void setNormalSprite(Sprite sprite) {
        normalSprite = sprite;
    }

    public void setSpecularSprite(Sprite sprite) {
        specularSprite = sprite;
    }

    public void close() {
    }
}
