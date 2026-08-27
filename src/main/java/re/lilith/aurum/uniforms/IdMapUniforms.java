package re.lilith.aurum.uniforms;

import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.util.vector.Vector3f;
import re.lilith.aurum.gl.uniform.UniformUpdateFrequency;
import re.lilith.aurum.gl.uniform.holder.DynamicUniformHolder;
import re.lilith.aurum.pipeline.state.CapturedRenderingState;
import re.lilith.aurum.shaderpack.IdMap;
import re.lilith.aurum.shaderpack.materialmap.NamespacedId;
import re.lilith.aurum.uniforms.utility.FrameUpdateNotifier;

import static re.lilith.aurum.gl.uniform.UniformUpdateFrequency.PER_FRAME;

public final class IdMapUniforms {

    private IdMapUniforms() {
    }

    public static void addIdMapUniforms(FrameUpdateNotifier notifier, DynamicUniformHolder uniforms, IdMap idMap) {
        HeldItemSupplier mainHandSupplier = new HeldItemSupplier(idMap.getItemIdMap());
        notifier.addListener(mainHandSupplier::update);

        uniforms
                .uniform1i(UniformUpdateFrequency.PER_FRAME, "heldItemId", mainHandSupplier::getIntID)
                .uniform1i(PER_FRAME, "heldBlockLightValue", mainHandSupplier::getLightValue)
                .uniformVanilla3f(PER_FRAME, "heldBlockLightColor", mainHandSupplier::getLightColor);
        //.uniformVanilla3f(PER_FRAME, "heldBlockLightColor2", offHandSupplier::getLightColor);

        uniforms.uniform1i("entityId", CapturedRenderingState.INSTANCE::getCurrentRenderedEntity,
                CapturedRenderingState.INSTANCE.getEntityIdNotifier());

        uniforms.uniform1i("blockEntityId", CapturedRenderingState.INSTANCE::getCurrentRenderedBlockEntity,
                CapturedRenderingState.INSTANCE.getBlockEntityIdNotifier());

        uniforms.uniform1i("currentRenderedItemId", CapturedRenderingState.INSTANCE::getCurrentRenderedItem,
                CapturedRenderingState.INSTANCE.getItemIdNotifier());
    }

    /**
     * Provides the currently held item, and it's light value, in the given hand as a uniform. Uses the item.properties ID map to map the item
     * to an integer, and the old hand light value to map offhand to main hand.
     */
    private static class HeldItemSupplier {
        private final Object2IntFunction<NamespacedId> itemIdMap;
        private int intID;
        private int lightValue;
        private Vector3f lightColor;

        HeldItemSupplier(Object2IntFunction<NamespacedId> itemIdMap) {
            this.itemIdMap = itemIdMap;
        }

        private void invalidate() {
            intID = -1;
            lightValue = 0;
            lightColor = new Vector3f(1f, 1f, 1f);
        }

        public void update() {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;

            if (player == null) {
                // Not valid when the player doesn't exist
                invalidate();
                return;
            }

            ItemStack heldStack = player.getMainHandStack();

            if (heldStack == null) {
                invalidate();
                return;
            }

            Item heldItem = heldStack.getItem();

            if (heldItem == null) {
                invalidate();
                return;
            }

            intID = itemIdMap.applyAsInt(new NamespacedId(Item.REGISTRY.getIdentifier(heldItem).toString()));
        }

        public int getIntID() {
            return intID;
        }

        public int getLightValue() {
            return lightValue;
        }

        public Vector3f getLightColor() {
            return lightColor;
        }
    }
}
