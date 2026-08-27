package re.lilith.aurum.mixin.gbuffer;

import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.gbuffer.matching.SpecialCondition;
import re.lilith.aurum.gbuffer.matching.SpecialConditions;

/**
 * Selects the pack's {@code gbuffers_armor_glint} program for the enchantment glint.
 *
 * <p>The glint is drawn as an extra pass over the armour with an animated {@code GL_TEXTURE} matrix. Nothing else
 * distinguishes it from an ordinary entity draw, so without this it renders with the entity program and the pack
 * never gets to shade it as a glint.</p>
 */
@Mixin(ArmorFeatureRenderer.class)
public class MixinArmorFeatureRenderer {
    @Inject(method = "renderGlint", at = @At("HEAD"))
    private void aurum$beginGlint(CallbackInfo ci) {
        SpecialConditions.set(SpecialCondition.GLINT);
    }

    @Inject(method = "renderGlint", at = @At("RETURN"))
    private void aurum$endGlint(CallbackInfo ci) {
        SpecialConditions.set(null);
    }
}
