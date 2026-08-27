package re.lilith.aurum.mixin.state;

import net.minecraft.client.render.model.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.gbuffer.state.StateTracker;

@Mixin(ModelPart.class)
public class MixinModelPart {
    @Inject(method = "compileList", at = @At("HEAD"))
    private void aurum$beginCompileList(float scale, CallbackInfo ci) {
        StateTracker.INSTANCE.compilingDisplayList = true;
    }

    @Inject(method = "compileList", at = @At("RETURN"))
    private void aurum$endCompileList(float scale, CallbackInfo ci) {
        StateTracker.INSTANCE.compilingDisplayList = false;
    }
}
