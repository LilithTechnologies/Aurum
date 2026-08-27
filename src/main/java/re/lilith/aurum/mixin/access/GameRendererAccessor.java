package re.lilith.aurum.mixin.access;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Accessor("lightmapTexture")
    NativeImageBackedTexture getLightTexture();
}