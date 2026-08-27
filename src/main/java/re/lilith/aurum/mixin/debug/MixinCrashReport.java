package re.lilith.aurum.mixin.debug;

import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.Aurum;

/**
 * Adds the current shaderpack and number of changed options to crash reports
 */
@Mixin(CrashReport.class)
public abstract class MixinCrashReport {
    @Shadow
    @Final
    private CrashReportSection systemDetailsSection;

    @Inject(at = @At("RETURN"), method = "fillSystemDetails")
    private void fillSystemDetails(CallbackInfo info) {
        if (Aurum.getCurrentPackName() == null) return; // this also gets called at startup for some reason

        this.systemDetailsSection.add("Loaded Shaderpack", () -> {
            StringBuilder sb = new StringBuilder(Aurum.getCurrentPackName() + (Aurum.isFallback() ? " (fallback)" : ""));
            Aurum.getCurrentPack().ifPresent(pack -> {
                sb.append("\n\t\t");
                sb.append(pack.getProfileInfo());
            });
            return sb.toString();
        });
    }
}
