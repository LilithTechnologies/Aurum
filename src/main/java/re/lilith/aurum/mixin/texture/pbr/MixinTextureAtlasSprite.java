package re.lilith.aurum.mixin.texture.pbr;

import net.minecraft.client.texture.Sprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.texture.pbr.PBRSpriteHolder;
import re.lilith.aurum.texture.pbr.extension.TextureAtlasSpriteExtension;

@Mixin(Sprite.class)
public class MixinTextureAtlasSprite implements TextureAtlasSpriteExtension {
    @Unique
    private PBRSpriteHolder pbrHolder;

    @Inject(method = "nullify()V", at = @At("TAIL"), remap = false)
    private void aurum$onTailClose(CallbackInfo ci) {
        if (pbrHolder != null) {
            pbrHolder.close();
        }
    }

    @Override
    public PBRSpriteHolder aurum$getOrCreatePBRHolder() {
        if (pbrHolder == null) {
            pbrHolder = new PBRSpriteHolder();
        }
        return pbrHolder;
    }
}
