package re.lilith.aurum.uniforms.utility;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.util.HashMap;
import java.util.Map;

public final class EntityColorState {
    public static final String UNIFORM_NAME = "aurum_EntityColor";

    private static final Map<Integer, Integer> LOCATIONS = new HashMap<>();

    private static float red;
    private static float green;
    private static float blue;
    private static float alpha;

    private EntityColorState() {
    }

    /**
     * @param alpha the strength of the tint, zero when the entity is not tinted
     */
    public static void set(float red, float green, float blue, float alpha) {
        EntityColorState.red = red;
        EntityColorState.green = green;
        EntityColorState.blue = blue;
        EntityColorState.alpha = alpha;

        upload();
    }

    public static void reset() {
        set(0.0F, 0.0F, 0.0F, 0.0F);
    }

    /**
     * Uploads to whichever program is bound. Cheap enough to call on every program bind.
     */
    public static void upload() {
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);

        if (program == 0) {
            return;
        }

        int location = LOCATIONS.computeIfAbsent(program, p -> GL20.glGetUniformLocation(p, UNIFORM_NAME));

        if (location < 0) {
            return;
        }

        GL20.glUniform4f(location, red, green, blue, alpha);
    }

    /**
     * Programs are recreated when the pack reloads, so cached locations cannot outlive them.
     */
    public static void clearCache() {
        LOCATIONS.clear();
    }
}
