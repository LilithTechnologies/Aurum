package re.lilith.aurum.gl.image;

import re.lilith.aurum.gl.texture.InternalTextureFormat;
import re.lilith.aurum.gl.texture.PixelFormat;
import re.lilith.aurum.gl.texture.PixelType;
import re.lilith.aurum.gl.texture.TextureType;

public record ImageInformation(String name, String samplerName, TextureType target, PixelFormat format,
                               InternalTextureFormat internalTextureFormat,
                               PixelType type, int width, int height, int depth, boolean clear, boolean isRelative,
                               float relativeWidth, float relativeHeight) {
}
