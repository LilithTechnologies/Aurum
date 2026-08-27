package re.lilith.aurum.config;

import net.minecraft.client.resource.language.I18n;
import org.apache.commons.lang3.text.WordUtils;
import re.lilith.aurum.gl.AurumRenderSystem;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

public enum FeatureFlags {
    SEPARATE_HARDWARE_SAMPLERS(() -> true, () -> true),
    HIGHER_SHADOWCOLOR(() -> true, () -> true),
    CUSTOM_IMAGES(() -> true, () -> true),
    BLOCK_EMISSION_ATTRIBUTE(() -> true, () -> true),
    PER_BUFFER_BLENDING(() -> true, AurumRenderSystem::supportsBufferBlending),
    COMPUTE_SHADERS(() -> true, AurumRenderSystem::supportsCompute),
    ENTITY_TRANSLUCENT(() -> true, () -> true),
    REVERSED_CULLING(() -> true, () -> true),
    FADE_VARIABLE(() -> true, () -> true),
    CAN_DISABLE_WEATHER(() -> true, () -> true),
    SSBO(() -> true, AurumRenderSystem::supportsSSBO),
    TEXTURE_FILTERING(() -> true, () -> true),
    UNKNOWN(() -> false, () -> false);

    private final BooleanSupplier aurumRequirement;
    private final BooleanSupplier hardwareRequirement;

    FeatureFlags(BooleanSupplier aurumRequirement, BooleanSupplier hardwareRequirement) {
        this.aurumRequirement = aurumRequirement;
        this.hardwareRequirement = hardwareRequirement;
    }

    public static String getInvalidStatus(List<FeatureFlags> invalidFeatureFlags) {
        boolean unsupportedHardware = false, unsupportedAurum = false;
        FeatureFlags[] flags = invalidFeatureFlags.toArray(new FeatureFlags[0]);
        for (FeatureFlags flag : flags) {
            unsupportedAurum |= !flag.aurumRequirement.getAsBoolean();
            unsupportedHardware |= !flag.hardwareRequirement.getAsBoolean();
        }

        if (unsupportedAurum) {
            if (unsupportedHardware) {
                return I18n.translate("aurum.unsupported.aurumorpc");
            }

            return I18n.translate("aurum.unsupported.aurum");
        } else if (unsupportedHardware) {
            return I18n.translate("aurum.unsupported.pc");
        } else {
            return null;
        }
    }

    public String getHumanReadableName() {
        return WordUtils.capitalize(name().replace("_", " ").toLowerCase());
    }

    public boolean isUsable() {
        return aurumRequirement.getAsBoolean() && hardwareRequirement.getAsBoolean();
    }

    public static boolean isInvalid(String name) {
        try {
            return !FeatureFlags.valueOf(name.toUpperCase(Locale.US)).isUsable();
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    public static FeatureFlags getValue(String value) {
        try {
            return FeatureFlags.valueOf(value.toUpperCase(Locale.US));
        } catch (IllegalArgumentException e) {
            return FeatureFlags.UNKNOWN;
        }
    }
}
