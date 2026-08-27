package re.lilith.aurum.mixin.i18n;

import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.shaderpack.LanguageMap;
import re.lilith.aurum.shaderpack.ShaderPack;

import java.util.*;

@Mixin(value = TranslationStorage.class, priority = 990)
public class MixinTranslationStorage {

    @Shadow
    Map<String, String> translations;

    @Unique
    private static final List<String> aurum$languageCodes = new ArrayList<>();

    @Inject(
            method = "translate",
            at = @At("HEAD"),
            cancellable = true
    )
    private void aurum$overrideLanguageEntries(
            String key,
            CallbackInfoReturnable<String> cir
    ) {
        String override = aurum$lookupOverriddenEntry(key);

        if (override != null) {
            cir.setReturnValue(override);
        }
    }

    @Unique
    private String aurum$lookupOverriddenEntry(String key) {
        ShaderPack pack = Aurum.getCurrentPack().orElse(null);

        if (pack == null) {
            return null;
        }

        if (translations.containsKey(key)) {
            return null;
        }

        LanguageMap languageMap = pack.getLanguageMap();

        for (String code : aurum$languageCodes) {
            Map<String, String> languageTranslations =
                    languageMap.getTranslations(code);

            if (languageTranslations == null) {
                continue;
            }

            String translation = languageTranslations.get(key);

            if (translation != null) {
                return translation;
            }
        }

        return null;
    }

    @Inject(
            method = "load(Lnet/minecraft/resource/ResourceManager;Ljava/util/List;)V",
            at = @At("HEAD")
    )
    private void aurum$addLanguageCodes(
            ResourceManager resourceManager,
            List<String> definitions,
            CallbackInfo ci
    ) {
        aurum$languageCodes.clear();

        new LinkedList<>(definitions)
                .descendingIterator()
                .forEachRemaining(code ->
                        aurum$languageCodes.add(
                                code.toLowerCase(Locale.ROOT)
                        )
                );
    }
}