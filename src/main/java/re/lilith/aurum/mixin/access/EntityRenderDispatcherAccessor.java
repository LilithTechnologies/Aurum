package re.lilith.aurum.mixin.access;

import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityRenderDispatcher.class)
public interface EntityRenderDispatcherAccessor {
    @Accessor("field_11098")
    Entity getCameraEntity();

    @Accessor("field_11098")
    void setCameraEntity(Entity entity);
}
