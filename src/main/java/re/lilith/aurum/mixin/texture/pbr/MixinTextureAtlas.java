package re.lilith.aurum.mixin.texture.pbr;

import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.SpriteAtlasTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.texture.pbr.PBRAtlasHolder;
import re.lilith.aurum.texture.pbr.extension.TextureAtlasExtension;

@Mixin(SpriteAtlasTexture.class)
public abstract class MixinTextureAtlas extends AbstractTexture implements TextureAtlasExtension {
    @Unique
    private PBRAtlasHolder pbrHolder;

    @Inject(method = "update", at = @At("TAIL"))
    private void aurum$onTailCycleAnimationFrames(CallbackInfo ci) {
        if (pbrHolder != null) {
            pbrHolder.cycleAnimationFrames();
        }
    }

    @Override
    public PBRAtlasHolder aurum$getPBRHolder() {
        return pbrHolder;
    }

    @Override
    public PBRAtlasHolder aurum$getOrCreatePBRHolder() {
        if (pbrHolder == null) {
            pbrHolder = new PBRAtlasHolder();
        }
        return pbrHolder;
    }
}
