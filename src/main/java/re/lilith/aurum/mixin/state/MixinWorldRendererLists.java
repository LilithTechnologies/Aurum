package re.lilith.aurum.mixin.state;

import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.gbuffer.state.StateTracker;

@Mixin(WorldRenderer.class)
public class MixinWorldRendererLists {
    @Inject(method = {"renderDarkSky", "renderLightSky", "renderStars()V"}, at = @At("HEAD"))
    private void aurum$beginCompileList(CallbackInfo ci) {
        StateTracker.INSTANCE.compilingDisplayList = true;
    }

    @Inject(method = {"renderDarkSky", "renderLightSky", "renderStars()V"}, at = @At("RETURN"))
    private void aurum$endCompileList(CallbackInfo ci) {
        StateTracker.INSTANCE.compilingDisplayList = false;
    }
}
