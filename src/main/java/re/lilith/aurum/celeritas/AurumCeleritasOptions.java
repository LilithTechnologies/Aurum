package re.lilith.aurum.celeritas;

import org.embeddedt.embeddium.impl.gui.framework.TextComponent;
import org.taumc.celeritas.api.options.OptionIdentifier;
import org.taumc.celeritas.api.options.control.ControlValueFormatter;
import org.taumc.celeritas.api.options.control.SliderControl;
import org.taumc.celeritas.api.options.structure.OptionGroup;
import org.taumc.celeritas.api.options.structure.OptionImpl;
import org.taumc.celeritas.api.options.structure.OptionPage;
import org.taumc.celeritas.api.options.structure.OptionStorage;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.gui.option.AurumVideoSettings;

import java.util.List;

public final class AurumCeleritasOptions {
    private static final OptionStorage<Void> STORAGE = new OptionStorage<>() {
        @Override
        public Void getData() {
            return null;
        }

        @Override
        public void save() {
            try {
                Aurum.getAurumConfig().save();
            } catch (java.io.IOException e) {
                Aurum.LOGGER.error("Failed to save Aurum config", e);
            }
        }
    };

    private AurumCeleritasOptions() {
    }

    public static OptionPage page() {
        OptionGroup.Builder group = OptionGroup.createBuilder();

        group.add(OptionImpl.createBuilder(Integer.class, STORAGE)
                .setId(OptionIdentifier.create(Aurum.MODID, "shadow_distance", Integer.class))
                .setName(TextComponent.translatable("options.aurum.shadowDistance"))
                .setTooltip(TextComponent.translatable(AurumVideoSettings.isShadowDistanceSliderEnabled()
                        ? "options.aurum.shadowDistance.enabled"
                        : "options.aurum.shadowDistance.disabled"))
                .setBinding(
                        (_, value) -> AurumVideoSettings.shadowDistance = value,
                        _ -> AurumVideoSettings.getOverriddenShadowDistance(AurumVideoSettings.shadowDistance))
                .setControl(option -> new SliderControl(option, 0, 32, 1,
                        ControlValueFormatter.translateVariable("options.aurum.shadowDistance.value")))
                .setEnabledPredicate(AurumVideoSettings::isShadowDistanceSliderEnabled)
                .build());

        return new OptionPage(
                OptionIdentifier.create(Aurum.MODID, "video_settings"),
                TextComponent.translatable("options.aurum.videoSettings"),
                List.of(group.build()));
    }
}
