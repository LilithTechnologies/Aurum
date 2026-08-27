package re.lilith.aurum.celeritas.mixin;

import net.minecraft.client.MinecraftClient;
import org.embeddedt.embeddium.impl.gui.CeleritasVideoOptionsController;
import org.embeddedt.embeddium.impl.gui.frame.tab.Tab;
import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.api.options.OptionIdentifier;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.celeritas.ArgentumVideoSettingsContext;
import re.lilith.aurum.celeritas.AurumCeleritasOptions;
import re.lilith.aurum.gui.screen.ShaderPackScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(CeleritasVideoOptionsController.class)
public class MixinCeleritasVideoOptionsController {

    @Inject(method = "createExtraTabs", at = @At("TAIL"))
    private void aurum$addShaderTab(
            Map<String, List<Tab<?>>> tabs,
            CallbackInfo ci
    ) {
        tabs.computeIfAbsent(Aurum.MODID, $ -> new ArrayList<>())
                .add(Tab.createBuilder()
                        .setTitle(TextComponent.translatable(
                                "options.aurum.shaderPackSelection"))
                        .setId(OptionIdentifier.create(
                                Aurum.MODID,
                                "shader_packs"))
                        .setOnSelectFunction(() -> {
                            MinecraftClient.getInstance().setScreen(new ShaderPackScreen(ArgentumVideoSettingsContext.SCREEN));
                            return false;
                        })
                        .build());

        tabs.computeIfAbsent(Aurum.MODID, $ -> new ArrayList<>())
                .add(Tab.from(AurumCeleritasOptions.page(), option -> true, new AtomicReference<>(0)));
    }
}
