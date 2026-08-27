package re.lilith.aurum.texture;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.texture.AbstractTexture;
import org.jetbrains.annotations.Nullable;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.state.StateUpdateNotifiers;
import re.lilith.aurum.mixin.access.GlStateManagerAccessor;
import re.lilith.aurum.pipeline.WorldRenderingPipeline;

public class TextureTracker {
    public static final TextureTracker INSTANCE = new TextureTracker();

    private static Runnable bindTextureListener;

    static {
        StateUpdateNotifiers.bindTextureNotifier = listener -> bindTextureListener = listener;
    }

    private final Int2ObjectMap<AbstractTexture> textures = new Int2ObjectOpenHashMap<>();

    private boolean lockBindCallback;

    private TextureTracker() {
    }

    public void trackTexture(int id, AbstractTexture texture) {
        textures.put(id, texture);
    }

    @Nullable
    public AbstractTexture getTexture(int id) {
        return textures.get(id);
    }

    public void onBindTexture(int id) {
        if (lockBindCallback) {
            return;
        }
        if (GlStateManagerAccessor.getActiveTexture() == 0) {
            lockBindCallback = true;
            if (bindTextureListener != null) {
                bindTextureListener.run();
            }
            WorldRenderingPipeline pipeline = Aurum.getPipelineManager().getPipelineNullable();
            if (pipeline != null) {
                pipeline.onBindTexture(id);
            }
            // Reset texture state
            AurumRenderSystem.bindTextureToUnit(0, id);
            lockBindCallback = false;
        }
    }

    public void onDeleteTexture(int id) {
        textures.remove(id);
    }
}
