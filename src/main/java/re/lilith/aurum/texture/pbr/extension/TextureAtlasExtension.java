package re.lilith.aurum.texture.pbr.extension;

import org.jetbrains.annotations.Nullable;
import re.lilith.aurum.texture.pbr.PBRAtlasHolder;

public interface TextureAtlasExtension {
    @Nullable
    PBRAtlasHolder aurum$getPBRHolder();

    PBRAtlasHolder aurum$getOrCreatePBRHolder();
}
