package re.lilith.aurum.texture;

import com.mojang.blaze3d.platform.GlStateManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.mixin.access.GameRendererAccessor;
import re.lilith.aurum.mixin.access.GlStateManagerAccessor;
import re.lilith.aurum.shaderpack.PackDirectives;
import re.lilith.aurum.shaderpack.texture.CustomTextureData;
import re.lilith.aurum.shaderpack.texture.TextureStage;
import re.lilith.aurum.targets.texture.NativeImageBackedCustomTexture;
import re.lilith.aurum.targets.texture.NativeImageBackedNoiseTexture;
import re.lilith.aurum.texture.format.TextureFormat;
import re.lilith.aurum.texture.format.TextureFormatLoader;
import re.lilith.aurum.texture.pbr.PBRTextureHolder;
import re.lilith.aurum.texture.pbr.PBRTextureManager;
import re.lilith.aurum.texture.pbr.PBRType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.function.IntSupplier;

public class CustomTextureManager {
    private final EnumMap<TextureStage, Object2ObjectMap<String, IntSupplier>> customTextureIdMap = new EnumMap<>(TextureStage.class);
    private final IntSupplier noise;

    /**
     * List of all OpenGL texture objects owned by this CustomTextureManager that need to be deleted in order to avoid
     * leaks.
     * Make sure any textures added to this list call releaseId from the close method.
     */
    private final List<AbstractTexture> ownedTextures = new ArrayList<>();

    public CustomTextureManager(PackDirectives packDirectives,
                                EnumMap<TextureStage, Object2ObjectMap<String, CustomTextureData>> customTextureDataMap,
                                @Nullable CustomTextureData customNoiseTextureData) {
        customTextureDataMap.forEach((textureStage, customTextureStageDataMap) -> {
            Object2ObjectMap<String, IntSupplier> customTextureIds = new Object2ObjectOpenHashMap<>();

            customTextureStageDataMap.forEach((samplerName, textureData) -> {
                try {
                    customTextureIds.put(samplerName, createCustomTexture(textureData));
                } catch (IOException e) {
                    Aurum.LOGGER.error("Unable to parse the image data for the custom texture on stage {}, sampler {}", textureStage, samplerName, e);
                }
            });

            customTextureIdMap.put(textureStage, customTextureIds);
        });

        noise = Optional.ofNullable(customNoiseTextureData).flatMap(textureData -> {
            try {
                return Optional.of(createCustomTexture(textureData));
            } catch (IOException e) {
                Aurum.LOGGER.error("Unable to parse the image data for the custom noise texture", e);

                return Optional.empty();
            }
        }).orElseGet(() -> {
            final int noiseTextureResolution = packDirectives.getNoiseTextureResolution();

            AbstractTexture texture = new NativeImageBackedNoiseTexture(noiseTextureResolution);
            ownedTextures.add(texture);

            return texture::getGlId;
        });
    }

    private IntSupplier createCustomTexture(CustomTextureData textureData) throws IOException {
        switch (textureData) {
            case CustomTextureData.PngData pngData -> {
                AbstractTexture texture = new NativeImageBackedCustomTexture(pngData);
                ownedTextures.add(texture);

                return texture::getGlId;
            }
            case CustomTextureData.LightmapMarker lightmapMarker -> {
                // Special code path for the light texture. While shader packs hardcode the primary light texture, it's
                // possible that a mod will create a different light texture, so this code path is robust to that.
                return () ->
                        ((GameRendererAccessor) MinecraftClient.getInstance().gameRenderer).getLightTexture().getGlId();
                // Special code path for the light texture. While shader packs hardcode the primary light texture, it's
                // possible that a mod will create a different light texture, so this code path is robust to that.
            }
            case CustomTextureData.ResourceData resourceData -> {
                String namespace = resourceData.getNamespace();
                String location = resourceData.getLocation();

                String withoutExtension;
                int extensionIndex = FilenameUtils.indexOfExtension(location);
                if (extensionIndex != -1) {
                    withoutExtension = location.substring(0, extensionIndex);
                } else {
                    withoutExtension = location;
                }
                PBRType pbrType = PBRType.fromFileLocation(withoutExtension);

                TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();

                if (pbrType == null) {
                    Identifier textureLocation = new Identifier(namespace, location);

                    // NB: We have to re-query the TextureManager for the texture object every time. This is because the
                    //     AbstractTexture object could be removed / deleted from the TextureManager on resource reloads,
                    //     and we could end up holding on to a deleted texture unless we added special code to handle resource
                    //     reloads. Re-fetching the texture from the TextureManager every time is the most robust approach for
                    //     now.
                    return () -> {
                        AbstractTexture texture = (AbstractTexture) textureManager.getTexture(textureLocation);

                        // TODO: Should we give something else if the texture isn't there? This will need some thought
                        return texture != null ? texture.getGlId() : textureManager.getTexture(SpriteAtlasTexture.MISSING).getGlId();
                    };
                } else {
                    location = location.substring(0, extensionIndex - pbrType.getSuffix().length()) + location.substring(extensionIndex);
                    Identifier textureLocation = new Identifier(namespace, location);

                    return () -> {
                        AbstractTexture texture = (AbstractTexture) textureManager.getTexture(textureLocation);

                        if (texture != null) {
                            int id = texture.getGlId();
                            PBRTextureHolder pbrHolder = PBRTextureManager.INSTANCE.getOrLoadHolder(id);
                            AbstractTexture pbrTexture = switch (pbrType) {
                                case NORMAL -> pbrHolder.normalTexture();
                                case SPECULAR -> pbrHolder.specularTexture();
                            };

                            TextureFormat textureFormat = TextureFormatLoader.getFormat();
                            if (textureFormat != null) {
                                int previousBinding = GlStateManagerAccessor.getActiveTexture();
                                GlStateManager.bindTexture(pbrTexture.getGlId());
                                textureFormat.setupTextureParameters(pbrType, pbrTexture);
                                GlStateManager.bindTexture(previousBinding);
                            }

                            return pbrTexture.getGlId();
                        }

                        return textureManager.getTexture(SpriteAtlasTexture.MISSING).getGlId();
                    };
                }
            }
            case null, default ->
                    throw new IllegalArgumentException("Unable to handle custom texture data " + textureData);
        }
    }

    public EnumMap<TextureStage, Object2ObjectMap<String, IntSupplier>> getCustomTextureIdMap() {
        return customTextureIdMap;
    }

    public Object2ObjectMap<String, IntSupplier> getCustomTextureIdMap(TextureStage stage) {
        return customTextureIdMap.getOrDefault(stage, Object2ObjectMaps.emptyMap());
    }

    public IntSupplier getNoiseTexture() {
        return noise;
    }

    public void destroy() {
        ownedTextures.forEach(AbstractTexture::clearGlId);
    }
}
