package re.lilith.aurum.mixin.texture;

import net.minecraft.client.resource.AnimationMetadata;
import net.minecraft.client.texture.Sprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(Sprite.class)
public interface SpriteAccessor {
    @Accessor("meta")
    AnimationMetadata getMetadata();

    @Accessor("field_11198")
    int[][] getMainImage();

    @Mutable
    @Accessor("field_11198")
    void setMainImage(int[][] mainImage);

    @Accessor("frames")
    List<int[][]> getFrames();

    @Accessor("x")
    int getX();

    @Accessor("y")
    int getY();

    @Accessor("frameIndex")
    int getFrame();

    @Accessor("frameIndex")
    void setFrame(int frame);
}
