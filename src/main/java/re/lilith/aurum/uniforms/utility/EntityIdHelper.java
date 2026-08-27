package re.lilith.aurum.uniforms.utility;

import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningBoltEntity;
import net.minecraft.entity.player.PlayerEntity;
import re.lilith.aurum.gbuffer.BlockRenderingSettings;
import re.lilith.aurum.shaderpack.materialmap.NamespacedId;

public final class EntityIdHelper {
    private static final NamespacedId CURRENT_PLAYER = new NamespacedId("minecraft", "current_player");
    private static final NamespacedId LIGHTNING_BOLT_ID = new NamespacedId("minecraft", "lightning_bolt");

    private static final Object2IntMap<Class<?>> entityIdCache = new Object2IntOpenHashMap<>();
    private static Object2IntFunction<NamespacedId> cachedEntityIdMap;
    private static int cachedCurrentPlayerId = -1;

    static {
        entityIdCache.defaultReturnValue(Integer.MIN_VALUE);
    }

    private EntityIdHelper() {
    }

    public static int getEntityId(Entity entity) {
        Object2IntFunction<NamespacedId> entityIdMap = BlockRenderingSettings.INSTANCE.getEntityIds();
        if (entityIdMap == null) {
            return -1;
        }

        if (entityIdMap != cachedEntityIdMap) {
            entityIdCache.clear();
            cachedEntityIdMap = entityIdMap;
            cachedCurrentPlayerId = entityIdMap.applyAsInt(CURRENT_PLAYER);
        }

        int specialId = getSpecialEntityId(entity);
        if (specialId != -1) {
            return specialId;
        }

        return getNormalEntityId(entity, entityIdMap);
    }

    private static int getSpecialEntityId(Entity entity) {
        if (cachedCurrentPlayerId != -1 && entity instanceof PlayerEntity
                && entity == MinecraftClient.getInstance().getCameraEntity()) {
            return cachedCurrentPlayerId;
        }

        return -1;
    }

    private static int getNormalEntityId(Entity entity, Object2IntFunction<NamespacedId> entityIdMap) {
        Class<?> entityClass = entity.getClass();

        int cached = entityIdCache.getInt(entityClass);
        if (cached != Integer.MIN_VALUE) {
            return cached;
        }

        int resolvedId = resolveEntityId(entity, entityClass, entityIdMap);
        entityIdCache.put(entityClass, resolvedId);

        return resolvedId;
    }

    private static int resolveEntityId(Entity entity, Class<?> entityClass, Object2IntFunction<NamespacedId> entityIdMap) {
        String entityType = EntityType.getEntityName(entity);
        if (entityType != null) {
            int id = entityIdMap.applyAsInt(new NamespacedId(entityType));

            if (id != -1) {
                return id;
            }
        }

        String simpleClassName = entityClass.getSimpleName();
        int id = entityIdMap.applyAsInt(new NamespacedId(simpleClassName));

        if (id == -1) {
            String className = entityClass.getName();
            id = entityIdMap.applyAsInt(new NamespacedId(className));
        }

        if (id == -1 && entity instanceof LightningBoltEntity) {
            id = entityIdMap.applyAsInt(LIGHTNING_BOLT_ID);
        }

        return id;
    }
}
