package re.lilith.aurum.uniforms;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import re.lilith.aurum.gl.uniform.holder.UniformHolder;

import java.util.Objects;

import static re.lilith.aurum.gl.uniform.UniformUpdateFrequency.PER_TICK;

public final class WorldTimeUniforms {
    private WorldTimeUniforms() {
    }

    /**
     * Makes world time uniforms available to the given program
     *
     * @param uniforms the program to make the uniforms available to
     */
    public static void addWorldTimeUniforms(UniformHolder uniforms) {
        uniforms
                .uniform1i(PER_TICK, "worldTime", WorldTimeUniforms::getWorldDayTime)
                .uniform1i(PER_TICK, "worldDay", WorldTimeUniforms::getWorldDay)
                .uniform1i(PER_TICK, "moonPhase", () -> getWorld().getMoonPhase());
    }

    static int getWorldDayTime() {
        return (int) (getWorld().getTimeOfDay() % 24000L);
    }

    private static int getWorldDay() {
        long time = getWorld().getLevelProperties().getTime();

        return (int) (time / 24000L);
    }

    private static ClientWorld getWorld() {
        return Objects.requireNonNull(MinecraftClient.getInstance().world);
    }
}
