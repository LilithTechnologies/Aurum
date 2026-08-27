package re.lilith.aurum.celeritas.mixin;

import dev.rdh.argentum.impl.gui.VideoOptionsScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.celeritas.ArgentumVideoSettingsContext;

@Mixin(VideoOptionsScreen.class)
public abstract class MixinVideoOptionsScreen extends Screen {
    @Inject(method = "init", at = @At("RETURN"))
    private void aurum$capture(CallbackInfo ci) {
        ArgentumVideoSettingsContext.SCREEN = (VideoOptionsScreen) (Screen) this;
    }
}
