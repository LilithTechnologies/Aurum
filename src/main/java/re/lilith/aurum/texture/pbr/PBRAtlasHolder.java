package re.lilith.aurum.texture.pbr;

public class PBRAtlasHolder {
    protected PBRAtlasTexture normalAtlas;
    protected PBRAtlasTexture specularAtlas;

    public void setNormalAtlas(PBRAtlasTexture atlas) {
        normalAtlas = atlas;
    }

    public void setSpecularAtlas(PBRAtlasTexture atlas) {
        specularAtlas = atlas;
    }

    public void cycleAnimationFrames() {
        if (normalAtlas != null) {
            normalAtlas.cycleAnimationFrames();
        }
        if (specularAtlas != null) {
            specularAtlas.cycleAnimationFrames();
        }
    }
}
