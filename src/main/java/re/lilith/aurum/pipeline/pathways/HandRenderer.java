package re.lilith.aurum.pipeline.pathways;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.gbuffer.BlockRenderingSettings;
import re.lilith.aurum.pipeline.WorldRenderingPhase;
import re.lilith.aurum.pipeline.WorldRenderingPipeline;
import re.lilith.aurum.pipeline.state.CapturedRenderingState;

import java.util.Map;

public class HandRenderer {
    public static final HandRenderer INSTANCE = new HandRenderer();

    private boolean ACTIVE;
    private boolean renderingSolid;
    public static final float DEPTH = 0.125F;

    // HandRenderer draws before the depth buffer is cleared for the hand, so the hand has to win the
    // depth test against terrain on its own. With near=0.05 the window depth of anything further than a
    // few centimetres is already above 0.1, so compressing the hand into [0, 0.1] keeps it in front
    // without clearing the shared depth buffer, which the composite passes read.
    private static final double HAND_DEPTH_RANGE = 0.1;

    private void setupGlState(GameRenderer gameRenderer, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();

        GL11.glDepthRange(0.0, HAND_DEPTH_RANGE);

        GlStateManager.matrixMode(5889);
        GlStateManager.loadIdentity();

        Project.gluPerspective(gameRenderer.getFov(tickDelta, false), (float) client.width / (float) client.height, 0.05F, gameRenderer.viewDistance * 2.0F);
        GlStateManager.matrixMode(5888);
        GlStateManager.loadIdentity();

        GlStateManager.pushMatrix();
        gameRenderer.bobViewWhenHurt(tickDelta);
        if (client.options.bobView) {
            gameRenderer.bobView(tickDelta);
        }


        GlStateManager.popMatrix();
    }

    private boolean canNotRender() {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean bl = client.getCameraEntity() instanceof LivingEntity && ((LivingEntity) client.getCameraEntity()).isSleeping();
        return client.options.perspective != 0 || bl || client.options.hudHidden || client.interactionManager.isSpectator();
    }

    public boolean isHandTranslucent() {
        ItemStack heldItem = MinecraftClient.getInstance().player.getStackInHand();
        if (heldItem == null) {
            return false;
        }
        Item item = heldItem.getItem();

        if (item instanceof BlockItem itemBlock) {
            Map<Block, RenderLayer> blockTypeIds = BlockRenderingSettings.INSTANCE.getBlockTypeIds();
            return blockTypeIds != null && blockTypeIds.get(itemBlock.getBlock()) == RenderLayer.TRANSLUCENT;
        }

        return false;
    }

    public boolean isAnyHandTranslucent() {
        return isHandTranslucent();
    }

    public void renderSolid(float tickDelta, GameRenderer gameRenderer, WorldRenderingPipeline pipeline) {
        if (canNotRender() || Aurum.getCurrentPack().isEmpty()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();

        ACTIVE = true;

        pipeline.setPhase(WorldRenderingPhase.HAND_SOLID);

        GL11.glPushMatrix();
        GL11.glDepthMask(true); // actually write to the depth buffer, it's normally disabled at this point

        client.profiler.push("aurum_hand");

        setupGlState(gameRenderer, tickDelta);

        renderingSolid = true;

        gameRenderer.enableLightmap();
        gameRenderer.firstPersonRenderer.renderArmHoldingItem(tickDelta);
        gameRenderer.disableLightmap();

        GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glDepthMask(false);
        GL11.glPopMatrix();

        client.profiler.pop();

        resetProjectionMatrix();
        restoreDepthRange();

        renderingSolid = false;

        pipeline.setPhase(WorldRenderingPhase.NONE);

        ACTIVE = false;
    }


    public void renderTranslucent(float tickDelta, GameRenderer gameRenderer, WorldRenderingPipeline pipeline) {
        if (canNotRender() || !isAnyHandTranslucent() || Aurum.getCurrentPack().isEmpty()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();

        ACTIVE = true;

        pipeline.setPhase(WorldRenderingPhase.HAND_TRANSLUCENT);

        GL11.glPushMatrix();

        client.profiler.push("aurum_hand_translucent");

        setupGlState(gameRenderer, tickDelta);

        gameRenderer.enableLightmap();
        gameRenderer.firstPersonRenderer.renderArmHoldingItem(tickDelta);
        gameRenderer.disableLightmap();

        GL11.glPopMatrix();

        resetProjectionMatrix();
        restoreDepthRange();

        client.profiler.pop();

        pipeline.setPhase(WorldRenderingPhase.NONE);

        ACTIVE = false;
    }

    private void restoreDepthRange() {
        GL11.glDepthRange(0.0, 1.0);
    }

    private void resetProjectionMatrix() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glMultMatrixf(CapturedRenderingState.INSTANCE.getGbufferProjection().get(new float[16]));
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    public boolean isActive() {
        return ACTIVE;
    }

    public boolean isRenderingSolid() {
        return renderingSolid;
    }
}