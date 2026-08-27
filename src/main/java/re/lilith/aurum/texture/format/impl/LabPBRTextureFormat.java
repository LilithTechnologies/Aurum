package re.lilith.aurum.texture.format.impl;

import org.jetbrains.annotations.Nullable;
import re.lilith.aurum.texture.format.TextureFormat;
import re.lilith.aurum.texture.mipmap.generator.ChannelMipmapGenerator;
import re.lilith.aurum.texture.mipmap.generator.CustomMipmapGenerator;
import re.lilith.aurum.texture.mipmap.function.DiscreteBlendFunction;
import re.lilith.aurum.texture.mipmap.function.LinearBlendFunction;
import re.lilith.aurum.texture.pbr.PBRType;

import java.util.Objects;

public record LabPBRTextureFormat(String name, @Nullable String version) implements TextureFormat {
    public static final ChannelMipmapGenerator SPECULAR_MIPMAP_GENERATOR = new ChannelMipmapGenerator(
            LinearBlendFunction.INSTANCE,
            new DiscreteBlendFunction(v -> v < 230 ? 0 : v - 229),
            new DiscreteBlendFunction(v -> v < 65 ? 0 : 1),
            new DiscreteBlendFunction(v -> v < 255 ? 0 : 1)
    );

    @Override
    public boolean canInterpolateValues(PBRType pbrType) {
        return pbrType != PBRType.SPECULAR;
    }

    @Override
    public @Nullable CustomMipmapGenerator getMipmapGenerator(PBRType pbrType) {
        if (pbrType == PBRType.SPECULAR) {
            return SPECULAR_MIPMAP_GENERATOR;
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LabPBRTextureFormat other = (LabPBRTextureFormat) obj;
        return Objects.equals(name, other.name) && Objects.equals(version, other.version);
    }
}
