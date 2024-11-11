package software.bernie.geckolib3.mixin;

import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import software.bernie.geckolib3.renderers.texture.AnimatableTexture;

import java.util.Map;
import java.util.Set;

@Mixin(value = TextureManager.class, priority = 2000)
public abstract class TextureManagerMixin {

    @Shadow
    @Final
    private Map<ResourceLocation, AbstractTexture> byPath;

    @Shadow protected abstract AbstractTexture loadTexture(ResourceLocation pPath, AbstractTexture pTexture);

    @Shadow @Final private Set<Tickable> tickableTextures;

    @Shadow protected abstract void safeClose(ResourceLocation p_118509_, AbstractTexture p_118510_);


    @Shadow public abstract void register(ResourceLocation pPath, AbstractTexture pTexture);

    /**
     * @author
     * @reason
     */
    @Overwrite
    public AbstractTexture getTexture(ResourceLocation path) {
        AbstractTexture abstracttexture = this.byPath.get(path);
        if (abstracttexture == null) {
            abstracttexture = geckolib$replaceAnimatableTexture(path);

            if (!(abstracttexture instanceof AnimatableTexture)) {
                this.register(path, abstracttexture);
            }
        }

        return abstracttexture;
    }

    private SimpleTexture geckolib$replaceAnimatableTexture(ResourceLocation location) {
        AnimatableTexture animatableTexture = new AnimatableTexture(location);

        register(location, animatableTexture);

        return animatableTexture.isAnimated() ? animatableTexture : new SimpleTexture(location);
    }


}