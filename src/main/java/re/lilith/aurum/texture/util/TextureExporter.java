package re.lilith.aurum.texture.util;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import org.apache.commons.io.FilenameUtils;

import java.io.File;

public class TextureExporter {
    public static void exportTextures(String directory, String filename, int textureId, int mipLevel, int width, int height) {
        String extension = FilenameUtils.getExtension(filename);
        String baseName = filename.substring(0, filename.length() - extension.length() - 1);
        for (int level = 0; level <= mipLevel; ++level) {
            exportTexture(directory, baseName + "_" + level + "." + extension, textureId, level, width >> level, height >> level);
        }
    }

    public static void exportTexture(String directory, String filename, int textureId, int level, int width, int height) {
        NativeImage nativeImage = new NativeImage(width, height, false);
        GlStateManager.bindTexture(textureId);
        nativeImage.downloadTexture(level, false);

        File dir = new File(MinecraftClient.getInstance().runDirectory, directory);
        dir.mkdirs();
        File file = new File(dir, filename);

        try {
            nativeImage.writeToFile(file.toPath());
        } catch (Exception _) {
        } finally {
            nativeImage.close();
        }
    }
}
