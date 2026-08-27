package re.lilith.aurum.texture.pbr;

import net.minecraft.client.texture.AbstractTexture;
import org.jetbrains.annotations.NotNull;

public interface PBRTextureHolder {
    @NotNull
    AbstractTexture normalTexture();

    @NotNull
    AbstractTexture specularTexture();
}
