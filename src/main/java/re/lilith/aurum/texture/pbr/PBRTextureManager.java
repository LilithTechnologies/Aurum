package re.lilith.aurum.texture.pbr;

import com.mojang.blaze3d.platform.GlStateManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import org.jetbrains.annotations.NotNull;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.gl.state.StateUpdateNotifiers;
import re.lilith.aurum.mixin.access.GlStateManagerAccessor;
import re.lilith.aurum.targets.texture.NativeImageBackedSingleColorTexture;
import re.lilith.aurum.texture.TextureTracker;
import re.lilith.aurum.texture.pbr.loader.PBRTextureLoader;
import re.lilith.aurum.texture.pbr.loader.PBRTextureLoader.PBRTextureConsumer;
import re.lilith.aurum.texture.pbr.loader.PBRTextureLoaderRegistry;

public class PBRTextureManager {
    public static final PBRTextureManager INSTANCE = new PBRTextureManager();

    public static final boolean DEBUG = System.getProperty("aurum.pbr.debug") != null;

    // TODO: Figure out how to merge these two.
    private static Runnable normalTextureChangeListener;
    private static Runnable specularTextureChangeListener;

    static {
        StateUpdateNotifiers.normalTextureChangeNotifier = listener -> normalTextureChangeListener = listener;
        StateUpdateNotifiers.specularTextureChangeNotifier = listener -> specularTextureChangeListener = listener;
    }

    private final Int2ObjectMap<PBRTextureHolder> holders = new Int2ObjectOpenHashMap<>();
    private final PBRTextureConsumerImpl consumer = new PBRTextureConsumerImpl();

    private NativeImageBackedSingleColorTexture defaultNormalTexture;
    private NativeImageBackedSingleColorTexture defaultSpecularTexture;

    // Not PBRTextureHolderImpl to directly reference fields
    private final PBRTextureHolder defaultHolder = new PBRTextureHolder() {
        @Override
        public @NotNull AbstractTexture normalTexture() {
            return defaultNormalTexture;
        }

        @Override
        public @NotNull AbstractTexture specularTexture() {
            return defaultSpecularTexture;
        }
    };

    private PBRTextureManager() {
    }

    public void init() {
        defaultNormalTexture = new NativeImageBackedSingleColorTexture(PBRType.NORMAL.getDefaultValue());
        defaultSpecularTexture = new NativeImageBackedSingleColorTexture(PBRType.SPECULAR.getDefaultValue());
    }

    public PBRTextureHolder getOrLoadHolder(int id) {
        PBRTextureHolder holder = holders.get(id);
        if (holder == null) {
            holder = loadHolder(id);
            holders.put(id, holder);
        }
        return holder;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private PBRTextureHolder loadHolder(int id) {
        AbstractTexture texture = TextureTracker.INSTANCE.getTexture(id);
        if (texture != null) {
            Class<? extends AbstractTexture> clazz = texture.getClass();
            PBRTextureLoader loader = PBRTextureLoaderRegistry.INSTANCE.getLoader(clazz);
            if (loader != null) {
                int previousTextureBinding = GlStateManagerAccessor.getActiveTexture();
                consumer.clear();
                try {
                    loader.load(texture, MinecraftClient.getInstance().getResourceManager(), consumer);
                    return consumer.toHolder();
                } catch (Exception e) {
                    Aurum.LOGGER.error("Failed to load PBR textures for texture {}", id, e);
                } finally {
                    GlStateManager.bindTexture(previousTextureBinding);
                }
            }
        }
        return defaultHolder;
    }

    public void onDeleteTexture(int id) {
        PBRTextureHolder holder = holders.remove(id);
        if (holder != null) {
            closeHolder(holder);
        }
    }

    public void clear() {
        for (PBRTextureHolder holder : holders.values()) {
            if (holder != defaultHolder) {
                closeHolder(holder);
            }
        }
        holders.clear();
    }

    public void close() {
        clear();
        defaultNormalTexture.clearGlId();
        defaultSpecularTexture.clearGlId();
    }

    private void closeHolder(PBRTextureHolder holder) {
        AbstractTexture normalTexture = holder.normalTexture();
        AbstractTexture specularTexture = holder.specularTexture();
        if (normalTexture != defaultNormalTexture) {
            closeTexture(normalTexture);
        }
        if (specularTexture != defaultSpecularTexture) {
            closeTexture(specularTexture);
        }
    }

    private static void closeTexture(AbstractTexture texture) {
        try {
            texture.clearGlId();
        } catch (Exception e) {
            Aurum.LOGGER.error(e);
        }
    }

    public static void notifyPBRTexturesChanged() {
        if (normalTextureChangeListener != null) {
            normalTextureChangeListener.run();
        }

        if (specularTextureChangeListener != null) {
            specularTextureChangeListener.run();
        }
    }

    private class PBRTextureConsumerImpl implements PBRTextureConsumer {
        private AbstractTexture normalTexture;
        private AbstractTexture specularTexture;
        private boolean changed;

        @Override
        public void acceptNormalTexture(@NotNull AbstractTexture texture) {
            normalTexture = texture;
            changed = true;
        }

        @Override
        public void acceptSpecularTexture(@NotNull AbstractTexture texture) {
            specularTexture = texture;
            changed = true;
        }

        public void clear() {
            normalTexture = defaultNormalTexture;
            specularTexture = defaultSpecularTexture;
            changed = false;
        }

        public PBRTextureHolder toHolder() {
            if (changed) {
                return new PBRTextureHolderImpl(normalTexture, specularTexture);
            } else {
                return defaultHolder;
            }
        }
    }

    private record PBRTextureHolderImpl(AbstractTexture normalTexture,
                                        AbstractTexture specularTexture) implements PBRTextureHolder {
        @Override
        public @NotNull AbstractTexture normalTexture() {
            return normalTexture;
        }

        @Override
        public @NotNull AbstractTexture specularTexture() {
            return specularTexture;
        }
    }
}
