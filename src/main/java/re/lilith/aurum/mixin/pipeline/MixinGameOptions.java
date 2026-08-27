package re.lilith.aurum.mixin.pipeline;

import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.Aurum;

@Mixin(value = GameOptions.class, priority = 990)
public class MixinGameOptions {
    @Unique
    private static boolean aurum$initialized;

    @Inject(method = "load()V", at = @At("HEAD"))
    private void aurum$beforeLoadOptions(CallbackInfo ci) {
        if (aurum$initialized) {
            return;
        }

        aurum$initialized = true;
        new Aurum().onEarlyInitialize();
    }
}
