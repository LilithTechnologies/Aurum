package re.lilith.aurum.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.shaderpack.texture.CustomTextureData;
import re.lilith.aurum.shaderpack.texture.TextureFilteringData;
import re.lilith.aurum.targets.texture.NativeImageBackedCustomTexture;

import java.io.IOException;
import java.util.Objects;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    @Unique
    private static boolean aurum$hasConnectedBefore = false;

    @Inject(method = "setGlErrorMessage", at = @At("HEAD"), cancellable = true)
    private void aurum$disableVanillaGlErrorCheck(String message, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "initializeGame", at = @At("TAIL"))
    private void aurum$setupImages(CallbackInfo ci) {
        try {
            MinecraftClient.getInstance().getTextureManager().loadTexture(new Identifier("aurum", "textures/gui/widgets.png"), new NativeImageBackedCustomTexture(new CustomTextureData.PngData(new TextureFilteringData(false, false), IOUtils.toByteArray(Objects.requireNonNull(Aurum.class.getResourceAsStream("/assets/aurum/textures/gui/widgets.png"))))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Inject(method = "connect(Lnet/minecraft/client/world/ClientWorld;)V", at = @At("HEAD"))
    private void aurum$trackLastDimensionOnLevelChange(ClientWorld world, CallbackInfo ci) {
        Aurum.lastDimension = Aurum.getCurrentDimension();
    }

    @Inject(method = "initializeGame", at = @At("RETURN"))
    private void aurum$postInit(CallbackInfo ci) {
        Aurum.onLoadingComplete();
    }

    @Inject(method = "connect(Lnet/minecraft/client/world/ClientWorld;Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;setWorld(Lnet/minecraft/client/world/ClientWorld;)V", shift = At.Shift.BEFORE))
    private void aurum$resetPipeline(ClientWorld world, String loadingMessage, CallbackInfo ci) {
        if (!aurum$hasConnectedBefore) {
            aurum$hasConnectedBefore = true;
            return;
        }

        Aurum.LOGGER.info("Reloading pipeline on world change: {} => {}", Aurum.lastDimension, Aurum.getCurrentDimension());
        Aurum.getPipelineManager().destroyPipeline();

        // NB: We need create the pipeline immediately, so that it is ready by the time that Celeritas starts trying to
        // initialize its world renderer.
        if (world != null) {
            Aurum.getPipelineManager().preparePipeline(Aurum.getCurrentDimension());
        }
    }
}